package com.ptit.iot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sensors")
public class Sensor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sensor_key", nullable = false, unique = true, length = 30)
	private String sensorKey;

	@Column(name = "sensor_name", nullable = false, length = 100)
	private String sensorName;

	@Column(nullable = false, length = 10)
	private String unit;

	@Column(name = "min_value", precision = 10, scale = 2)
	private BigDecimal minValue;

	@Column(name = "max_value", precision = 10, scale = 2)
	private BigDecimal maxValue;

	@Column(name = "is_active", nullable = false)
	private boolean active = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@jakarta.persistence.PrePersist
	void onCreate() {
		if (createdAt == null) createdAt = LocalDateTime.now();
	}

}