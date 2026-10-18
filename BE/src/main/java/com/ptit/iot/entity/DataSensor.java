package com.ptit.iot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "data_sensor")
public class DataSensor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "sensor_id", nullable = false)
	private Sensor sensor;

	@Column(name = "measured_value", nullable = false, precision = 10, scale = 2)
	private BigDecimal value;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SensorReadingStatus status;

	@Column(name = "recorded_at", nullable = false)
	private LocalDateTime recordedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@PrePersist
	@PreUpdate
	void updateDerivedFields() {
		if (recordedAt == null) recordedAt = LocalDateTime.now();
		if (createdAt == null) createdAt = LocalDateTime.now();
		if (sensor != null && value != null) {
			boolean belowMinimum = sensor.getMinValue() != null && value.compareTo(sensor.getMinValue()) < 0;
			boolean aboveMaximum = sensor.getMaxValue() != null && value.compareTo(sensor.getMaxValue()) > 0;
			status = belowMinimum || aboveMaximum
					? SensorReadingStatus.OUT_OF_RANGE
					: SensorReadingStatus.NORMAL;
		}
	}

}