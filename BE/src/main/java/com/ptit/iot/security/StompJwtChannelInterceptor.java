package com.ptit.iot.security;

import java.security.Principal;

import com.ptit.iot.service.JwtService;
import lombok.RequiredArgsConstructor;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
		if (accessor == null || accessor.getCommand() == null) return message;

		if (accessor.getCommand() == StompCommand.CONNECT) {
			accessor.setUser(authenticate(accessor));
		} else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
			if (accessor.getUser() == null || !isAllowedTopic(accessor.getDestination())) {
				throw new AccessDeniedException("STOMP subscription requires authentication and an allowed topic");
			}
		} else if (accessor.getCommand() == StompCommand.SEND) {
			throw new AccessDeniedException("Client messages are not supported");
		}
		return message;
	}

	private Authentication authenticate(StompHeaderAccessor accessor) {
		String authorization = accessor.getFirstNativeHeader("Authorization");
		if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
			throw new AccessDeniedException("A bearer token is required to connect to STOMP");
		}
		try {
			Claims claims = jwtService.parse(authorization.substring(7).trim());
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());
			return org.springframework.security.authentication.UsernamePasswordAuthenticationToken
					.authenticated(user, null, user.getAuthorities());
		} catch (RuntimeException exception) {
			throw new AccessDeniedException("The STOMP bearer token is invalid or expired", exception);
		}
	}

	private boolean isAllowedTopic(String destination) {
		return "/topic/sensor".equals(destination)
				|| "/topic/device-status".equals(destination)
				|| "/topic/action-log".equals(destination);
	}
}
