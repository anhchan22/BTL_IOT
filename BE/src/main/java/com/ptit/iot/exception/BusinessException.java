package com.ptit.iot.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
	private final DomainCode domainCode;

	public BusinessException(DomainCode domainCode, Object... args) {
		super(domainCode.formatMessage(args));
		this.domainCode = domainCode;
	}
}
