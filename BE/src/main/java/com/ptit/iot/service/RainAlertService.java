package com.ptit.iot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.ptit.iot.dto.request.RainAlertSettingsRequest;
import com.ptit.iot.dto.response.RainAlertStatusResponse;

public interface RainAlertService {
	RainAlertStatusResponse getStatus();
	RainAlertStatusResponse updateSettings(RainAlertSettingsRequest request);
	void recordRainReading(BigDecimal rain, LocalDateTime recordedAt);
}
