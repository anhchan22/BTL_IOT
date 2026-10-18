package com.ptit.iot.service;

import com.ptit.iot.dto.request.LoginRequest;
import com.ptit.iot.dto.response.LoginResponse;
import com.ptit.iot.dto.request.ProfileUpdateRequest;
import com.ptit.iot.dto.response.UserData;
import com.ptit.iot.security.AuthenticatedUser;

public interface AuthService {
	LoginResponse login(LoginRequest request);
	UserData getProfile(String username, AuthenticatedUser principal);
	UserData updateProfile(Long id, ProfileUpdateRequest request, AuthenticatedUser principal);
}
