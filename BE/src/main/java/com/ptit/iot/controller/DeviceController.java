package com.ptit.iot.controller;

import java.util.List;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.common.api.ApiResponseFactory;
import com.ptit.iot.controller.api.DeviceApi;
import com.ptit.iot.dto.request.DeviceControlRequest;
import com.ptit.iot.dto.response.DeviceControlResponse;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.MqttStatusResponse;
import com.ptit.iot.security.AuthenticatedUser;
import com.ptit.iot.service.DeviceService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceApi {
	private final DeviceService deviceService;
	private final ApiResponseFactory responseFactory;

	@Override
	public ApiResponse<List<DeviceDTO>> getDevices() {
		return responseFactory.success(deviceService.getDevices());
	}

	@Override
	public ApiResponse<DeviceControlResponse> control(@Valid @RequestBody DeviceControlRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return responseFactory.success(new DeviceControlResponse(request.command().trim().toUpperCase(java.util.Locale.ROOT),
				deviceService.sendControl(request.command(), principal)));
	}

	@Override
	public ApiResponse<MqttStatusResponse> mqttStatus() {
		return responseFactory.success(deviceService.getMqttStatus());
	}
}
