package com.ptit.iot.entity;

import java.time.LocalDateTime;
import java.util.UUID;

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
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "action_log")
public class ActionLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "device_id", nullable = false)
	private Device device;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private DeviceAction action;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ActionLogStatus status = ActionLogStatus.LOADING;

	@Column(name = "batch_id", length = 36, columnDefinition = "CHAR(36)")
	private String batchId;

	@Column(name = "requested_at", nullable = false)
	private LocalDateTime requestedAt;

	@Column(name = "responded_at")
	private LocalDateTime respondedAt;

	@Column(name = "response_time_ms")
	private Integer responseTimeMs;

	@Column(name = "error_message", length = 255)
	private String errorMessage;

	@PrePersist
	void onCreate() {
		if (requestedAt == null) requestedAt = LocalDateTime.now();
	}

}