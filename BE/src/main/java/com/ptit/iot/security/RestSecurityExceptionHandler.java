package com.ptit.iot.security;

import java.io.IOException;

import com.ptit.iot.common.api.ApiResponseFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RestSecurityExceptionHandler {
	private final ObjectMapper objectMapper;
	private final ApiResponseFactory responseFactory;

	public AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, exception) -> write(response, HttpStatus.UNAUTHORIZED,
				"IOT-401", "Cần đăng nhập để tiếp tục");
	}

	public AccessDeniedHandler accessDeniedHandler() {
		return (request, response, exception) -> write(response, HttpStatus.FORBIDDEN,
				"IOT-403", "Bạn không có quyền thực hiện thao tác này");
	}

	private void write(jakarta.servlet.http.HttpServletResponse response, HttpStatus status, String code, String message)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), responseFactory.error(code, message, null));
	}
}
