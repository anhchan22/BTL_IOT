package com.ptit.iot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "devices")
public class Device {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "device_code", nullable = false, unique = true, length = 30)
	private String deviceCode;

	@Column(name = "device_name", nullable = false, length = 100)
	private String deviceName;

	@Column(length = 100)
	private String location;

	@Column(length = 255)
	private String description;

	@Column(name = "alert_threshold_rain", precision = 10, scale = 2)
	private BigDecimal alertThresholdRain;

	@Enumerated(EnumType.STRING)
	@Column(name = "current_status", nullable = false, length = 10)
	private DeviceStatus currentStatus = DeviceStatus.OFF;

	@Enumerated(EnumType.STRING)
	@Column(name = "last_confirmed_status", nullable = false, length = 10)
	private DeviceStatus lastConfirmedStatus = DeviceStatus.OFF;

	@Column(name = "last_changed_at")
	private LocalDateTime lastChangedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = LocalDateTime.now();
	}

}
