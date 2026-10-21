package com.ptit.iot.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RainAlertStatusResponse(BigDecimal threshold, boolean configured, boolean active,
		long activationsToday, OffsetDateTime activatedAt) { }
