package com.ptit.iot.controller;

import java.math.BigDecimal;
import java.util.List;

import com.ptit.iot.common.api.ApiResponse;
import com.ptit.iot.common.api.ApiResponseFactory;
import com.ptit.iot.controller.api.SensorApi;
import com.ptit.iot.dto.response.PageResponse;
import com.ptit.iot.dto.response.SensorChartPoint;
import com.ptit.iot.dto.response.SensorLatestResponse;
import com.ptit.iot.dto.response.SensorRecordResponse;
import com.ptit.iot.service.SensorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SensorController implements SensorApi {
	private final SensorService sensorService;
	private final ApiResponseFactory responseFactory;

	@Override
	public ApiResponse<SensorLatestResponse> latest() {
		return responseFactory.success(sensorService.getLatest());
	}

	@Override
	public ApiResponse<List<SensorChartPoint>> chart(Integer hours, Integer minutes) {
		return responseFactory.success(minutes != null
				? sensorService.getChartDataForMinutes(minutes)
				: sensorService.getChartData(hours == null ? 1 : hours));
	}

	@Override
	public ApiResponse<PageResponse<SensorRecordResponse>> search(int page, int pageSize, String sensorCode,
			String sensorKey, BigDecimal valueMin, BigDecimal min, BigDecimal valueMax, BigDecimal max,
			String from, String to, String sortBy, String sortDir) {
		return responseFactory.success(sensorService.getSensorData(page, pageSize,
				sensorCode != null ? sensorCode : sensorKey,
				valueMin != null ? valueMin : min, valueMax != null ? valueMax : max,
				from, to, sortBy, sortDir));
	}
}
