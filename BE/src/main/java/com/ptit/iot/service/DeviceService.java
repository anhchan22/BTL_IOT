package com.ptit.iot.service;

import java.util.List;

import com.ptit.iot.dto.response.ActionLogDTO;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.MqttStatusResponse;
import com.ptit.iot.security.AuthenticatedUser;

public interface DeviceService {
	/** Kiểm tra lệnh điều khiển, ghi nhận lệnh đang chờ và yêu cầu gửi lệnh đến ESP qua MQTT. */
	List<ActionLogDTO> sendControl(String command, AuthenticatedUser actor);
	/** Lấy danh sách thiết bị cùng trạng thái hiện tại để hiển thị trên màn hình. */
	List<DeviceDTO> getDevices();
	/** Trả về thông tin backend hiện có kết nối tới máy chủ MQTT hay không. */
	MqttStatusResponse getMqttStatus();
	/** Xử lý phản hồi của ESP cho lệnh bật hoặc tắt do người dùng gửi. */
	void handleDeviceAck(int deviceId, String actionText, String resultText);
	/** Xử lý phản hồi của ESP cho lệnh khôi phục trạng thái đã lưu. */
	void handleDeviceRestoreAck(int deviceId, String actionText, String resultText);
	/** Cập nhật trạng thái thiết bị khi ESP thông báo đang trực tuyến hoặc mất kết nối. */
	void handleDeviceAvailability(String status);
	/** Tìm các lệnh chờ quá thời hạn xác nhận và đánh dấu chúng đã hết hạn. */
	void expireTimedOutCommands();
}
