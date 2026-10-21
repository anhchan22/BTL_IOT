package com.ptit.iot.service;

import com.ptit.iot.dto.event.RainAlertStateChangedEvent;
import com.ptit.iot.entity.DeviceAction;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RainAlertEventPublisher {
	private final SimpMessagingTemplate messagingTemplate;
	private final MqttService mqttService;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void broadcast(RainAlertStateChangedEvent event) {
		messagingTemplate.convertAndSend("/topic/rain-alert", event.status());
		messagingTemplate.convertAndSend("/topic/action-log", event.actionLog());
		messagingTemplate.convertAndSend("/topic/device-status", event.device());
		int deviceId = Integer.parseInt(event.device().deviceCode().replace("LED", ""));
		mqttService.publishDeviceControlAsync(deviceId, event.status().active() ? DeviceAction.ON : DeviceAction.OFF);
	}
}
