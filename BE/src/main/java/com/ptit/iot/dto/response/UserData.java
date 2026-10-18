package com.ptit.iot.dto.response;

public record UserData(Long id, String username, String fullName, String studentCode, String className, String email,
		String avatarUrl, String githubUrl, String figmaUrl, String postmanUrl, String reportUrl) { }
