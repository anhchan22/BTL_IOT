package com.ptit.iot.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
		@NotBlank(message = "Họ và tên không được để trống") String fullName,
		@NotBlank(message = "Mã sinh viên không được để trống") String studentCode,
		String className,
		@Email(message = "Email không hợp lệ") String email,
		String avatarUrl,
		String githubUrl,
		String figmaUrl,
		String postmanUrl,
		String reportUrl) {
}
