package com.ptit.iot.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(name = "student_id", unique = true, length = 20)
	private String studentId;

	@Column(name = "class_name", length = 50)
	private String className;

	@Column(length = 100, unique = true)
	private String email;

	@Column(name = "avatar_url", length = 500)
	private String avatarUrl;

	@Column(name = "github_url", length = 500)
	private String githubUrl;

	@Column(name = "figma_url", length = 500)
	private String figmaUrl;

	@Column(name = "postman_url", length = 500)
	private String postmanUrl;

	@Column(name = "report_url", length = 500)
	private String reportUrl;

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