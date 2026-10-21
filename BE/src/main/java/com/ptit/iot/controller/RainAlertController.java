package com.ptit.iot.controller;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.common.api.ApiResponseFactory;
import com.ptit.iot.controller.api.RainAlertApi;
import com.ptit.iot.dto.request.RainAlertSettingsRequest;
import com.ptit.iot.dto.response.RainAlertStatusResponse;
import com.ptit.iot.service.RainAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RainAlertController implements RainAlertApi {
	private final RainAlertService rainAlertService;
	private final ApiResponseFactory responseFactory;

	@Override
	public ApiResponse<RainAlertStatusResponse> getStatus() {
		return responseFactory.success(rainAlertService.getStatus());
	}

	@Override
	public ApiResponse<RainAlertStatusResponse> updateSettings(@Valid @RequestBody RainAlertSettingsRequest request) {
		return responseFactory.success(rainAlertService.updateSettings(request));
	}
}
