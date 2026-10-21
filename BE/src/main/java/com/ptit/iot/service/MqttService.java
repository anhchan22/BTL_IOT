package com.ptit.iot.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ptit.iot.dto.event.SensorSnapshotEvent;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.ptit.iot.entity.DeviceAction;

@Service
@Slf4j
public class MqttService {

	private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?(?:\\d+(?:[.,]\\d*)?|[.,]\\d+)(?:[eE][-+]?\\d+)?");
	// Mức 0: gửi tối đa một lần, không xác nhận hay gửi lại nếu tin nhắn bị mất.
	// Chọn mức này cho số đo cảm biến gửi thường xuyên để giảm lưu lượng truyền.
	private static final int SENSOR_QOS = 0;

	private final String brokerUrl;
	private final String username;
	private final String password;
	private final String clientIdPrefix;
	private final String sensorTopic;
	private final String deviceControlTopic;
	private final String deviceResponseTopic;
	private final String deviceAvailabilityTopic;
	private final long deviceOfflineTimeoutMs;
	private final SensorIngestionService ingestionService;
	private final ObjectProvider<DeviceService> deviceServiceProvider;
	private final SimpMessagingTemplate messagingTemplate;
	private final ObjectMapper objectMapper;
	private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "mqtt-reconnect");
		thread.setDaemon(true);
		return thread;
	});
	private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
		Thread thread = new Thread(runnable, "mqtt-command-timeouts");
		thread.setDaemon(true);
		return thread;
	});

	private volatile MqttClient client;
	private final AtomicLong lastDeviceHeartbeatAt = new AtomicLong();
	private final AtomicBoolean deviceMarkedOnline = new AtomicBoolean();

	public MqttService(
			@Value("${app.mqtt.broker-url}") String brokerUrl,
			@Value("${app.mqtt.username:}") String username,
			@Value("${app.mqtt.password:}") String password,
			@Value("${app.mqtt.client-id}") String clientIdPrefix,
			@Value("${app.mqtt.topic-sensors}") String sensorTopic,
			@Value("${app.mqtt.topic-device-control}") String deviceControlTopic,
			@Value("${app.mqtt.topic-device-response}") String deviceResponseTopic,
			@Value("${app.mqtt.topic-device-availability}") String deviceAvailabilityTopic,
			@Value("${app.mqtt.device-offline-timeout-ms:15000}") long deviceOfflineTimeoutMs,
			SensorIngestionService ingestionService,
			ObjectProvider<DeviceService> deviceServiceProvider,
			SimpMessagingTemplate messagingTemplate,
			ObjectMapper objectMapper) {
		this.brokerUrl = brokerUrl;
		this.username = username;
		this.password = password;
		this.clientIdPrefix = clientIdPrefix;
		this.sensorTopic = sensorTopic;
		this.deviceControlTopic = deviceControlTopic;
		this.deviceResponseTopic = deviceResponseTopic;
		this.deviceAvailabilityTopic = deviceAvailabilityTopic;
		this.deviceOfflineTimeoutMs = deviceOfflineTimeoutMs;
		this.ingestionService = ingestionService;
		this.deviceServiceProvider = deviceServiceProvider;
		this.messagingTemplate = messagingTemplate;
		this.objectMapper = objectMapper;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void startConnectionLoop() {
		// Khi backend khởi động, bắt đầu kết nối MQTT và định kỳ kiểm tra thiết bị.
		reconnectExecutor.scheduleWithFixedDelay(this::connectIfNeeded, 0, 5, TimeUnit.SECONDS);
		timeoutExecutor.scheduleWithFixedDelay(() -> {
			try {
				deviceServiceProvider.getObject().expireTimedOutCommands();
				expireDeviceHeartbeat();
			} catch (Exception exception) {
				log.error("Could not expire timed-out device commands", exception);
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	// Tạo kết nối tới máy chủ MQTT; nếu mất kết nối thì bộ hẹn giờ sẽ thử kết nối lại.
	private synchronized void connectIfNeeded() {
		try {
			if (client != null && client.isConnected()) return;
			if (client == null) {
				String clientId = clientIdPrefix + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);
				client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
				client.setCallback(new MqttCallbackExtended() {
					@Override
					public void connectComplete(boolean reconnect, String serverURI) {
						log.info("Connected to MQTT broker {}{}", serverURI, reconnect ? " (reconnected)" : "");
						subscribeTopics();
					}

					@Override
					public void connectionLost(Throwable cause) {
						log.warn("MQTT connection lost: {}", cause == null ? "unknown error" : cause.getMessage());
					}

					@Override
					public void messageArrived(String topic, MqttMessage message) {
						// Thư viện MQTT gọi hàm này mỗi khi backend nhận được một tin nhắn.
						handleMessage(topic, message);
					}

					@Override
					public void deliveryComplete(IMqttDeliveryToken token) {
					}
				});
			}

			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(5);
			options.setKeepAliveInterval(30);
			if (username != null && !username.isBlank()) {
				options.setUserName(username);
				options.setPassword(password == null ? new char[0] : password.toCharArray());
			}
			client.connect(options);
		} catch (MqttException exception) {
			log.warn("Could not connect to MQTT broker {}: {}. Retrying in 5 seconds.", brokerUrl, exception.getMessage());
		}
	}

	// Đăng ký để backend nhận dữ liệu cảm biến và các phản hồi từ thiết bị.
	private void subscribeTopics() {
		MqttClient activeClient = client;
		if (activeClient == null || !activeClient.isConnected()) return;
		try {
			// sensorTopic lấy từ app.mqtt.topic-sensors trong application.yaml (mặc định data_sensors).
			// SENSOR_QOS = 0 nghĩa là backend nhận tin tối đa một lần, không yêu cầu xác nhận nhận tin.
			activeClient.subscribe(sensorTopic, SENSOR_QOS);
			activeClient.subscribe(deviceResponseTopic, SENSOR_QOS);
			activeClient.subscribe(deviceAvailabilityTopic, 1);
		} catch (MqttException exception) {
			log.error("Failed to subscribe to MQTT sensor topic {}", sensorTopic, exception);
		}
	}

	public boolean isConnected() {
		MqttClient activeClient = client;
		return activeClient != null && activeClient.isConnected();
	}

	public void publishDeviceControl(int deviceId, DeviceAction action) {
		publishDeviceCommand(deviceId, action, false);
	}

	public void publishDeviceRestore(int deviceId, DeviceAction action) {
		publishDeviceCommand(deviceId, action, true);
	}

	private void publishDeviceCommand(int deviceId, DeviceAction action, boolean restore) {
		MqttClient activeClient = client;
		if (activeClient == null || !activeClient.isConnected()) {
			throw new IllegalStateException("MQTT broker is not connected");
		}
		try {
			// Tạo nội dung lệnh gồm mã thiết bị và hành động bật hoặc tắt.
			var payload = objectMapper.createObjectNode();
			payload.put("deviceId", deviceId);
			payload.put("action", action.name().toLowerCase(java.util.Locale.ROOT));
			// Đánh dấu đây là lệnh khôi phục trạng thái đã lưu khi ESP kết nối lại.
			if (restore) payload.put("restore", true);
			// Chuyển nội dung lệnh thành dữ liệu byte để gửi qua MQTT.
			MqttMessage message = new MqttMessage(objectMapper.writeValueAsBytes(payload));
			// Mức 1 yêu cầu xác nhận đã nhận tin và có thể gửi lại nếu chưa được xác nhận.
			message.setQos(1);
			// Gửi lệnh lên topic điều khiển mà ESP đã đăng ký nhận.
			activeClient.publish(deviceControlTopic, message);
		} catch (MqttException | tools.jackson.core.JacksonException exception) {
			throw new IllegalStateException("Could not publish MQTT device command", exception);
		}
	}

	// Phân loại tin nhắn MQTT; với topic cảm biến thì đọc ba giá trị và chuyển đi lưu.
	private void handleMessage(String topic, MqttMessage message) {
		if (deviceAvailabilityTopic.equals(topic)) {
			handleDeviceAvailability(message);
			return;
		}
		if (deviceResponseTopic.equals(topic)) {
			handleDeviceResponse(message);
			return;
		}
		if (!sensorTopic.equals(topic)) {
			log.debug("Ignoring MQTT message on unhandled topic {}", topic);
			return;
		}
		String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
		try {
			// Đọc chuỗi JSON để có thể lấy từng giá trị cảm biến.
			JsonNode root = objectMapper.readTree(payload);
			// Lấy nhiệt độ;
			BigDecimal temperature = number(root, "nhiet_do", "temperature");
			// Lấy độ ẩm;
			BigDecimal humidity = number(root, "do_am", "humidity");
			// Lấy ánh sáng;
			BigDecimal light = number(root, "anh_sang", "light");
			// Lưu ba số đo vào cơ sở dữ liệu và tạo thông báo để gửi ra màn hình sau khi lưu xong.
			SensorSnapshotEvent event = ingestionService.recordSnapshot(temperature, humidity, light);
			// Có dữ liệu mới từ ESP nên ghi nhận thiết bị đang hoạt động.
			noteDeviceHeartbeat();

			log.debug("Saved sensor snapshot at {}", event.time());
		} catch (Exception exception) {
			log.warn("Discarding invalid MQTT sensor message on {}: {}", topic, exception.getMessage());
		}
	}

	private void handleDeviceResponse(MqttMessage message) {
		String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
		try {
			JsonNode root = objectMapper.readTree(payload);
			if (!root.has("deviceId") || !root.get("deviceId").canConvertToInt()) {
				throw new IllegalArgumentException("Missing or invalid deviceId");
			}
			String action = root.path("action").asString(null);
			String status = root.path("status").asString(null);
			int deviceId = root.get("deviceId").intValue();
			// Nếu ESP đánh dấu đây là phản hồi khôi phục thì chuyển cho hàm xử lý riêng.
			if (root.path("restore").asBoolean(false)) {
				deviceServiceProvider.getObject().handleDeviceRestoreAck(deviceId, action, status);
			} else {
				// Các phản hồi còn lại là xác nhận cho lệnh bật hoặc tắt thông thường.
				deviceServiceProvider.getObject().handleDeviceAck(deviceId, action, status);
			}
		} catch (Exception exception) {
			log.warn("Discarding invalid MQTT device response: {} ({})", payload, exception.getMessage());
		}
	}

	private void handleDeviceAvailability(MqttMessage message) {
		String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
		try {
			JsonNode root = objectMapper.readTree(payload);
			String status = root.path("status").asString("");
			if (!status.equalsIgnoreCase("online") && !status.equalsIgnoreCase("offline")) {
				throw new IllegalArgumentException("Unknown availability status");
			}
			boolean online = status.equalsIgnoreCase("online");
			deviceMarkedOnline.set(online);
			lastDeviceHeartbeatAt.set(online ? System.currentTimeMillis() : 0);
			deviceServiceProvider.getObject().handleDeviceAvailability(status);
		} catch (Exception exception) {
			log.warn("Discarding invalid MQTT device availability message: {} ({})", payload, exception.getMessage());
		}
	}

	private void noteDeviceHeartbeat() {
		lastDeviceHeartbeatAt.set(System.currentTimeMillis());
		if (deviceMarkedOnline.compareAndSet(false, true)) {
			log.info("Received live sensor data; marking ESP online");
			deviceServiceProvider.getObject().handleDeviceAvailability("online");
		}
	}

	private void expireDeviceHeartbeat() {
		long lastHeartbeat = lastDeviceHeartbeatAt.get();
		if (lastHeartbeat == 0 || System.currentTimeMillis() - lastHeartbeat <= deviceOfflineTimeoutMs) return;
		if (lastDeviceHeartbeatAt.compareAndSet(lastHeartbeat, 0)) {
			deviceMarkedOnline.set(false);
			log.warn("No ESP availability or sensor heartbeat for {} ms; marking ESP offline", deviceOfflineTimeoutMs);
			deviceServiceProvider.getObject().handleDeviceAvailability("offline");
		}
	}

	// Đọc một giá trị cảm biến từ nội dung tin nhắn; báo lỗi nếu thiếu hoặc không phải số.
	private BigDecimal number(JsonNode root, String... names) {
		JsonNode value = null;
		for (String name : names) {
			if (root.has(name) && !root.get(name).isNull()) {
				value = root.get(name);
				break;
			}
		}
		if (value == null) throw new IllegalArgumentException("Missing sensor value: " + String.join("/", names));

		if (value.isNumber()) return value.decimalValue();
		String text = value.asString().trim();
		Matcher matcher = NUMBER_PATTERN.matcher(text);
		if (!matcher.find()) throw new IllegalArgumentException("Invalid numeric sensor value: " + String.join("/", names));
		return new BigDecimal(matcher.group().replace(',', '.'));
	}

	//gửi dữ liệu lên màn hình
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void broadcastSensorSnapshot(SensorSnapshotEvent event) {
		messagingTemplate.convertAndSend("/topic/sensor", event);
	}

	@PreDestroy
	public void stop() {
		reconnectExecutor.shutdownNow();
		timeoutExecutor.shutdownNow();
		MqttClient activeClient = client;
		if (activeClient != null) {
			try {
				if (activeClient.isConnected()) activeClient.disconnect();
				activeClient.close();
			} catch (MqttException exception) {
				log.debug("Error while closing MQTT client", exception);
			}
		}
	}
}
