package com.ptit.iot.controller.api;

import java.math.BigDecimal;
import java.util.List;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.dto.response.PageResponse;
import com.ptit.iot.dto.response.SensorChartPoint;
import com.ptit.iot.dto.response.SensorLatestResponse;
import com.ptit.iot.dto.response.SensorRecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sensors")
@RequestMapping("/api/sensor")
public interface SensorApi {
	@Operation(summary = "Đọc số liệu cảm biến mới nhất")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/latest")
	ApiResponse<SensorLatestResponse> latest();

	@Operation(summary = "Đọc dữ liệu biểu đồ")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping("/chart")
	ApiResponse<List<SensorChartPoint>> chart(
			@RequestParam(name = "hours", required = false) Integer hours,
			@RequestParam(name = "minutes", required = false) Integer minutes);

	@Operation(summary = "Tra cứu lịch sử cảm biến")
	@SecurityRequirement(name = "bearerAuth")
	@GetMapping
	ApiResponse<PageResponse<SensorRecordResponse>> search(
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(name = "page_size", defaultValue = "15") int pageSize,
			@RequestParam(name = "sensor_code", required = false) String sensorCode,
			@RequestParam(name = "sensorKey", required = false) String sensorKey,
			@RequestParam(name = "value_min", required = false) BigDecimal valueMin,
			@RequestParam(name = "min", required = false) BigDecimal min,
			@RequestParam(name = "value_max", required = false) BigDecimal valueMax,
			@RequestParam(name = "max", required = false) BigDecimal max,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(name = "sort_by", defaultValue = "time") String sortBy,
			@RequestParam(name = "sort_dir", defaultValue = "desc") String sortDir);
}
