package com.ptit.iot.exception;

import org.springframework.http.HttpStatus;

public enum DomainCode {
	INVALID_CREDENTIALS("IOT-401", HttpStatus.UNAUTHORIZED, "Sai tên đăng nhập hoặc mật khẩu"),
	FORBIDDEN("IOT-403", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),
	USER_NOT_FOUND("IOT-404", HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
	INVALID_PAGE("IOT-400", HttpStatus.BAD_REQUEST, "page phải lớn hơn hoặc bằng 1"),
	INVALID_PAGE_SIZE("IOT-400", HttpStatus.BAD_REQUEST, "page_size phải nằm trong khoảng từ 1 đến 100"),
	INVALID_VALUE_RANGE("IOT-400", HttpStatus.BAD_REQUEST, "value_min không được lớn hơn value_max"),
	INVALID_TIME_RANGE("IOT-400", HttpStatus.BAD_REQUEST, "from phải sớm hơn to"),
	INVALID_SORT("IOT-400", HttpStatus.BAD_REQUEST, "sort_by không được hỗ trợ: {}"),
	INVALID_SORT_DIRECTION("IOT-400", HttpStatus.BAD_REQUEST, "sort_dir chỉ nhận asc hoặc desc"),
	INVALID_TIME_FORMAT("IOT-400", HttpStatus.BAD_REQUEST, "{} phải là ISO 8601 datetime hoặc yyyy-MM-dd"),
	INVALID_SENSOR_CODE("IOT-400", HttpStatus.BAD_REQUEST, "sensor_code chỉ nhận temperature, humidity hoặc light"),
	INVALID_HOURS("IOT-400", HttpStatus.BAD_REQUEST, "hours phải nằm trong khoảng từ 1 đến 168"),
	INVALID_DEVICE_CODE("IOT-400", HttpStatus.BAD_REQUEST, "device chỉ nhận LED1, LED2 hoặc LED3"),
	INVALID_ACTION("IOT-400", HttpStatus.BAD_REQUEST, "action chỉ nhận ON hoặc OFF"),
	INVALID_STATUS("IOT-400", HttpStatus.BAD_REQUEST, "status chỉ nhận LOADING, SUCCESS hoặc FAILED"),
	INVALID_DEVICE_COMMAND("IOT-400", HttpStatus.BAD_REQUEST, "Lệnh không hợp lệ: {}"),
	DEVICE_NOT_CONFIGURED("IOT-500", HttpStatus.INTERNAL_SERVER_ERROR, "Thiếu cấu hình thiết bị LED"),
	DEVICE_BUSY("IOT-409", HttpStatus.CONFLICT, "Thiết bị đang chờ xác nhận lệnh trước đó"),
	DEVICE_OFFLINE("IOT-503", HttpStatus.SERVICE_UNAVAILABLE, "Thiết bị đang ngoại tuyến, không thể điều khiển"),
	SENSOR_NOT_CONFIGURED("IOT-500", HttpStatus.INTERNAL_SERVER_ERROR, "Chưa cấu hình cảm biến {}"),
	INVALID_REQUEST("IOT-400", HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ"),
	CONFLICT("IOT-409", HttpStatus.CONFLICT, "Dữ liệu đã tồn tại hoặc vi phạm ràng buộc"),
	INTERNAL_ERROR("IOT-500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại");

	private final String code;
	private final HttpStatus status;
	private final String message;

	DomainCode(String code, HttpStatus status, String message) {
		this.code = code;
		this.status = status;
		this.message = message;
	}

	public String code() { return code; }
	public HttpStatus status() { return status; }
	public String formatMessage(Object... args) {
		String formatted = message;
		for (Object argument : args) formatted = formatted.replaceFirst("\\{\\}", java.util.regex.Matcher.quoteReplacement(String.valueOf(argument)));
		return formatted;
	}
}
