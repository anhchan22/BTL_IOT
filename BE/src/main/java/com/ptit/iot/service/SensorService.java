package com.ptit.iot.service;

import java.math.BigDecimal;
import java.util.List;

import com.ptit.iot.dto.response.PageResponse;
import com.ptit.iot.dto.response.SensorChartPoint;
import com.ptit.iot.dto.response.SensorLatestResponse;
import com.ptit.iot.dto.response.SensorRecordResponse;

public interface SensorService {
	SensorLatestResponse getLatest();
	List<SensorChartPoint> getChartData(int hours);
	List<SensorChartPoint> getChartDataForMinutes(int minutes);
	PageResponse<SensorRecordResponse> getSensorData(int page, int pageSize, String sensorCode,
			BigDecimal minValue, BigDecimal maxValue, String from, String to, String sortBy, String sortDir);
}
