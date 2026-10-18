package com.ptit.iot.dto.response;

public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		UserData userData) {
}
