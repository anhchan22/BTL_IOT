package com.ptit.iot.exception;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.ptit.iot.common.api.ApiErrorDetails;
import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.common.api.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {
	private final ApiResponseFactory responseFactory;

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<ApiErrorDetails>> handleBusinessException(BusinessException exception, HttpServletRequest request) {
		DomainCode domainCode = exception.getDomainCode();
		log.warn("Request rejected with code={} path={}", domainCode.code(), request.getRequestURI());
		return response(domainCode.status(), domainCode.code(), exception.getMessage(), Map.of(), request);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiResponse<ApiErrorDetails>> handleConflict(DataIntegrityViolationException exception, HttpServletRequest request) {
		return response(DomainCode.CONFLICT.status(), DomainCode.CONFLICT.code(), DomainCode.CONFLICT.formatMessage(), Map.of(), request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<ApiErrorDetails>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors().forEach(error -> details.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return response(HttpStatus.BAD_REQUEST, DomainCode.INVALID_REQUEST.code(), DomainCode.INVALID_REQUEST.formatMessage(), details, request);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<ApiErrorDetails>> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, DomainCode.INVALID_REQUEST.code(), DomainCode.INVALID_REQUEST.formatMessage(), Map.of("request", exception.getMessage()), request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<ApiErrorDetails>> handleUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, DomainCode.INVALID_REQUEST.code(), "Nội dung request không hợp lệ", Map.of(), request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<ApiErrorDetails>> handleUnexpected(Exception exception, HttpServletRequest request) {
		log.error("Unhandled request failure path={}", request.getRequestURI(), exception);
		return response(DomainCode.INTERNAL_ERROR.status(), DomainCode.INTERNAL_ERROR.code(), DomainCode.INTERNAL_ERROR.formatMessage(), Map.of(), request);
	}

	private ResponseEntity<ApiResponse<ApiErrorDetails>> response(HttpStatus status, String code, String message,
			Map<String, String> details, HttpServletRequest request) {
		ApiErrorDetails error = new ApiErrorDetails(OffsetDateTime.now(), status.value(), request.getRequestURI(), details);
		return ResponseEntity.status(status).body(responseFactory.error(code, message, error));
	}
}
