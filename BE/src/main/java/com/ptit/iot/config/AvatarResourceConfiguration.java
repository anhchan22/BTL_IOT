package com.ptit.iot.config;

import com.ptit.iot.service.AvatarStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class AvatarResourceConfiguration implements WebMvcConfigurer {
	private final AvatarStorageService avatarStorageService;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/uploads/avatars/**")
				.addResourceLocations(avatarStorageService.resourceLocation());
	}
}
