package com.ptit.iot.controller.api;

import java.util.List;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.dto.request.DeviceControlRequest;
import com.ptit.iot.dto.response.DeviceControlResponse;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.MqttStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import com.ptit.iot.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Devices")
@RequestMapping("/api/devices")
public interface DeviceApi {
	@Operation(summary = "Lấy thiết bị và trạng thái hiện tại")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	ApiResponse<List<DeviceDTO>> getDevices();
	@Operation(summary = "Gửi lệnh điều khiển thiết bị")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping("/control")
	ApiResponse<DeviceControlResponse> control(@Valid @RequestBody DeviceControlRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal);
	@Operation(summary = "Kiểm tra kết nối MQTT")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/mqtt-status")
	ApiResponse<MqttStatusResponse> mqttStatus();
}
