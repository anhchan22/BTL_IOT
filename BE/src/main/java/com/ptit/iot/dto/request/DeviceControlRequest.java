package com.ptit.iot.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeviceControlRequest(
		@NotBlank(message = "command không được để trống") String command,
		String user) {
}
