package com.ptit.iot.service.impl;

import com.ptit.iot.dto.request.LoginRequest;
import com.ptit.iot.dto.response.LoginResponse;
import com.ptit.iot.dto.request.ProfileUpdateRequest;
import com.ptit.iot.dto.response.UserData;
import com.ptit.iot.entity.User;
import com.ptit.iot.exception.BusinessException;
import com.ptit.iot.exception.DomainCode;
import com.ptit.iot.mapper.IoTMapper;
import com.ptit.iot.repository.UserRepository;
import com.ptit.iot.security.AuthenticatedUser;
import com.ptit.iot.service.AuthService;
import com.ptit.iot.service.JwtService;
import com.ptit.iot.service.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final IoTMapper mapper;
	private final AvatarStorageService avatarStorageService;

	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByUsername(request.username().trim())
				.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
				.orElseThrow(() -> new BusinessException(DomainCode.INVALID_CREDENTIALS));
		return new LoginResponse(jwtService.issueToken(user), "Bearer", jwtService.getExpirationMs(), mapper.toUserData(user));
	}

	@Override
	@Transactional(readOnly = true)
	public UserData getProfile(String username, AuthenticatedUser principal) {
		if (principal == null || !principal.username().equalsIgnoreCase(username)) {
			throw new BusinessException(DomainCode.FORBIDDEN);
		}
		return userRepository.findByUsername(username).map(mapper::toUserData)
				.orElseThrow(() -> new BusinessException(DomainCode.USER_NOT_FOUND));
	}

	@Override
	@Transactional
	public UserData updateProfile(Long id, ProfileUpdateRequest request, AuthenticatedUser principal) {
		if (principal == null || !principal.id().equals(id)) throw new BusinessException(DomainCode.FORBIDDEN);
		User user = userRepository.findById(id).orElseThrow(() -> new BusinessException(DomainCode.USER_NOT_FOUND));
		user.setFullName(request.fullName().trim());
		user.setStudentId(request.studentCode().trim());
		user.setClassName(clean(request.className()));
		user.setEmail(clean(request.email()));
		user.setAvatarUrl(clean(request.avatarUrl()));
		user.setGithubUrl(clean(request.githubUrl()));
		user.setFigmaUrl(clean(request.figmaUrl()));
		user.setPostmanUrl(clean(request.postmanUrl()));
		user.setReportUrl(clean(request.reportUrl()));
		return mapper.toUserData(userRepository.save(user));
	}

	@Override
	@Transactional
	public UserData uploadAvatar(Long id, MultipartFile file, AuthenticatedUser principal) {
		if (principal == null || !principal.id().equals(id)) throw new BusinessException(DomainCode.FORBIDDEN);
		User user = userRepository.findById(id).orElseThrow(() -> new BusinessException(DomainCode.USER_NOT_FOUND));
		user.setAvatarUrl(avatarStorageService.save(file));
		return mapper.toUserData(userRepository.save(user));
	}

	private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
