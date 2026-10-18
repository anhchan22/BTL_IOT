package com.ptit.iot.controller;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.common.api.ApiResponseFactory;
import com.ptit.iot.controller.api.AuthApi;
import com.ptit.iot.dto.request.LoginRequest;
import com.ptit.iot.dto.response.LoginResponse;
import com.ptit.iot.dto.request.ProfileUpdateRequest;
import com.ptit.iot.dto.response.UserData;
import com.ptit.iot.security.AuthenticatedUser;
import com.ptit.iot.service.AuthService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {
	private final AuthService authService;
	private final ApiResponseFactory responseFactory;

	@Override
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return responseFactory.success(authService.login(request));
	}

	@Override
	public ApiResponse<UserData> getProfile(@PathVariable String username, @AuthenticationPrincipal AuthenticatedUser principal) {
		return responseFactory.success(authService.getProfile(username, principal));
	}

	@Override
	public ApiResponse<UserData> updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal) {
		return responseFactory.success(authService.updateProfile(id, request, principal));
	}
}
