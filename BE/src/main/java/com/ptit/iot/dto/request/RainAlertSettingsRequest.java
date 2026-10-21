package com.ptit.iot.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record RainAlertSettingsRequest(
		@NotNull @DecimalMin("0.00") @DecimalMax("500.00") BigDecimal threshold) { }
