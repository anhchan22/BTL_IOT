package com.ptit.iot.controller.api;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.dto.request.RainAlertSettingsRequest;
import com.ptit.iot.dto.response.RainAlertStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Rain alert")
@RequestMapping("/api/rain-alert")
public interface RainAlertApi {
	@Operation(summary = "Đọc cấu hình và số lần cảnh báo mưa hôm nay")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	ApiResponse<RainAlertStatusResponse> getStatus();

	@Operation(summary = "Cài ngưỡng cảnh báo độ mưa")
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping("/settings")
	ApiResponse<RainAlertStatusResponse> updateSettings(@Valid @RequestBody RainAlertSettingsRequest request);
}
