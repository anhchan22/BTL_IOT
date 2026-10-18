package com.ptit.iot.service;

import java.util.List;

import com.ptit.iot.dto.response.ActionLogDTO;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.MqttStatusResponse;
import com.ptit.iot.security.AuthenticatedUser;

public interface DeviceService {
	List<ActionLogDTO> sendControl(String command, AuthenticatedUser actor);
	List<DeviceDTO> getDevices();
	MqttStatusResponse getMqttStatus();
	void handleDeviceAck(int deviceId, String actionText, String resultText);
	void expireTimedOutCommands();
}
