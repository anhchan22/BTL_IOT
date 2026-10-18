package com.ptit.iot.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.ptit.iot.dto.response.ActionLogDTO;
import com.ptit.iot.dto.response.DeviceDTO;
import com.ptit.iot.dto.response.DeviceStatusDTO;
import com.ptit.iot.dto.response.HistoryRecordDTO;
import com.ptit.iot.dto.response.SensorRecordResponse;
import com.ptit.iot.dto.response.UserData;
import com.ptit.iot.entity.ActionLog;
import com.ptit.iot.entity.DataSensor;
import com.ptit.iot.entity.Device;
import com.ptit.iot.entity.User;
import com.ptit.iot.repository.SensorChartProjection;
import com.ptit.iot.dto.response.SensorChartPoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IoTMapper {
	ZoneId CLASSROOM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

	@Mapping(target = "studentCode", source = "studentId")
	UserData toUserData(User user);

	@Mapping(target = "name", source = "deviceName")
	@Mapping(target = "status", source = "currentStatus")
	DeviceDTO toDeviceDTO(Device device);

	@Mapping(target = "status", source = "currentStatus")
	DeviceStatusDTO toDeviceStatusDTO(Device device);

	@Mapping(target = "deviceCode", source = "device.deviceCode")
	@Mapping(target = "username", source = "user.username")
	ActionLogDTO toActionLogDTO(ActionLog actionLog);

	@Mapping(target = "device", source = "device.deviceCode")
	@Mapping(target = "performedBy", source = "user.fullName")
	@Mapping(target = "time", source = "requestedAt")
	@Mapping(target = "responseTimeSeconds", expression = "java(actionLog.getResponseTimeMs() == null ? null : actionLog.getResponseTimeMs() / 1000.0)")
	HistoryRecordDTO toHistoryRecordDTO(ActionLog actionLog);

	@Mapping(target = "sensorCode", source = "sensor.sensorKey")
	@Mapping(target = "sensorName", source = "sensor.sensorName")
	@Mapping(target = "unit", source = "sensor.unit")
	@Mapping(target = "recordedAt", source = "recordedAt")
	SensorRecordResponse toSensorRecordResponse(DataSensor dataSensor);

	@Mapping(target = "id", expression = "java(point.recordedAt().atZone(CLASSROOM_ZONE).toInstant().toEpochMilli())")
	@Mapping(target = "time", source = "recordedAt")
	SensorChartPoint toSensorChartPoint(SensorChartProjection point);

	default OffsetDateTime toOffsetDateTime(LocalDateTime value) {
		return value == null ? null : value.atZone(CLASSROOM_ZONE).toOffsetDateTime();
	}
}
