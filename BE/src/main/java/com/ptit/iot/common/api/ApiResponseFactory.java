package com.ptit.iot.common.api;

import org.springframework.stereotype.Component;

@Component
public class ApiResponseFactory {
	private static final String SUCCESS_CODE = "IOT-000";
	private static final String SUCCESS_MESSAGE = "Thành công";

	public <T> ApiResponse<T> success(T data) {
		return of(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public ApiResponse<Void> success() {
		return success(null);
	}

	public <T> ApiResponse<T> of(boolean success, String code, String message, T data) {
		return new ApiResponse<>(success, code, message, data);
	}

	public <T> ApiResponse<T> error(String code, String message, T data) {
		return of(false, code, message, data);
	}
}
