package com.ptit.iot.config;

import com.ptit.iot.entity.User;
import com.ptit.iot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapUserConfiguration {

	@Bean
	ApplicationRunner bootstrapUser(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${app.bootstrap.username:}") String username,
			@Value("${app.bootstrap.password:}") String password,
			@Value("${app.bootstrap.full-name:}") String fullName) {
		return args -> {
			if (username.isBlank()) return;
			if (password.isBlank()) {
				throw new IllegalStateException("BOOTSTRAP_PASSWORD is required when BOOTSTRAP_USERNAME is set");
			}
			if (userRepository.findByUsername(username.trim()).isPresent()) return;

			User user = new User();
			user.setUsername(username.trim());
			user.setPasswordHash(passwordEncoder.encode(password));
			user.setFullName(fullName.isBlank() ? username.trim() : fullName.trim());
			userRepository.save(user);
		};
	}
}
