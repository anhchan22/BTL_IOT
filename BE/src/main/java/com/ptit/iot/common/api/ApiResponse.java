package com.ptit.iot.common.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
	private final boolean success;
	private final String code;
	private final String message;
	private final T data;
}
