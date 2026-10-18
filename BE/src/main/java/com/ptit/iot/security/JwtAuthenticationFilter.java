package com.ptit.iot.security;

import java.io.IOException;
import java.util.List;

import com.ptit.iot.entity.User;
import com.ptit.iot.repository.UserRepository;
import com.ptit.iot.service.JwtService;
import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authorization != null && authorization.startsWith("Bearer ")
				&& SecurityContextHolder.getContext().getAuthentication() == null) {
			String token = authorization.substring(7).trim();
			try {
				Claims claims = jwtService.parse(token);
				String username = claims.getSubject();
				Number userIdClaim = claims.get("uid", Number.class);
				if (username != null && userIdClaim != null) {
					User user = userRepository.findByUsername(username).orElse(null);
					if (user != null && user.getId().equals(userIdClaim.longValue())) {
						AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getUsername());
						var authentication = new UsernamePasswordAuthenticationToken(
								principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
						SecurityContextHolder.getContext().setAuthentication(authentication);
					}
				}
			} catch (JwtException | IllegalArgumentException ignored) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}
}
