package com.ptit.iot.controller.api;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.dto.request.LoginRequest;
import com.ptit.iot.dto.response.LoginResponse;
import com.ptit.iot.dto.request.ProfileUpdateRequest;
import com.ptit.iot.dto.response.UserData;
import com.ptit.iot.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Authentication and profile")
@RequestMapping("/api/auth")
public interface AuthApi {
	@Operation(summary = "Đăng nhập")
	@PostMapping("/login")
	ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request);
	@Operation(summary = "Lấy hồ sơ người dùng")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/profile/{username}")
	ApiResponse<UserData> getProfile(@PathVariable String username,
			@AuthenticationPrincipal AuthenticatedUser principal);
	@Operation(summary = "Cập nhật hồ sơ người dùng")
	@SecurityRequirement(name = "bearerAuth")
	@PutMapping("/profile/{id}")
	ApiResponse<UserData> updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdateRequest request,
			@AuthenticationPrincipal AuthenticatedUser principal);
	@Operation(summary = "Tải ảnh đại diện")
	@SecurityRequirement(name = "bearerAuth")
	@PostMapping(value = "/profile/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	ApiResponse<UserData> uploadAvatar(@PathVariable Long id, @RequestPart("file") MultipartFile file,
			@AuthenticationPrincipal AuthenticatedUser principal);
}
