package com.ptit.iot.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ptit.iot.entity.ActionLog;
import com.ptit.iot.entity.ActionLogStatus;
import com.ptit.iot.entity.DeviceAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActionLogRepository extends JpaRepository<ActionLog, Long> {
	@Query(value = """
			SELECT log
			FROM ActionLog log
			LEFT JOIN FETCH log.device device
			LEFT JOIN FETCH log.user user
			WHERE (:deviceCode IS NULL OR device.deviceCode = :deviceCode)
			  AND (:action IS NULL OR log.action = :action)
			  AND (:status IS NULL OR log.status = :status)
			  AND (:fromTime IS NULL OR log.requestedAt >= :fromTime)
			  AND (:toTime IS NULL OR log.requestedAt < :toTime)
			  AND (:username IS NULL OR user.username = :username)
			""",
		countQuery = """
			SELECT COUNT(log)
			FROM ActionLog log
			LEFT JOIN log.device device
			LEFT JOIN log.user user
			WHERE (:deviceCode IS NULL OR device.deviceCode = :deviceCode)
			  AND (:action IS NULL OR log.action = :action)
			  AND (:status IS NULL OR log.status = :status)
			  AND (:fromTime IS NULL OR log.requestedAt >= :fromTime)
			  AND (:toTime IS NULL OR log.requestedAt < :toTime)
			  AND (:username IS NULL OR user.username = :username)
			""")
	Page<ActionLog> findWithFilters(
			@Param("deviceCode") String deviceCode,
			@Param("action") DeviceAction action,
			@Param("status") ActionLogStatus status,
			@Param("fromTime") LocalDateTime fromTime,
			@Param("toTime") LocalDateTime toTime,
			@Param("username") String username,
			Pageable pageable);

	@Query(value = """
			SELECT log FROM ActionLog log
			LEFT JOIN FETCH log.device device
			LEFT JOIN FETCH log.user user
			WHERE device.deviceCode IN :deviceCodes
			  AND (:action IS NULL OR log.action = :action)
			  AND (:status IS NULL OR log.status = :status)
			  AND (:fromTime IS NULL OR log.requestedAt >= :fromTime)
			  AND (:toTime IS NULL OR log.requestedAt < :toTime)
			  AND (:username IS NULL OR user.username = :username)
			""",
		countQuery = """
			SELECT COUNT(log) FROM ActionLog log
			LEFT JOIN log.device device
			LEFT JOIN log.user user
			WHERE device.deviceCode IN :deviceCodes
			  AND (:action IS NULL OR log.action = :action)
			  AND (:status IS NULL OR log.status = :status)
			  AND (:fromTime IS NULL OR log.requestedAt >= :fromTime)
			  AND (:toTime IS NULL OR log.requestedAt < :toTime)
			  AND (:username IS NULL OR user.username = :username)
			""")
	Page<ActionLog> findWithDeviceCodes(
			@Param("deviceCodes") List<String> deviceCodes,
			@Param("action") DeviceAction action,
			@Param("status") ActionLogStatus status,
			@Param("fromTime") LocalDateTime fromTime,
			@Param("toTime") LocalDateTime toTime,
			@Param("username") String username,
			Pageable pageable);

	Optional<ActionLog> findTopByDevice_DeviceCodeAndStatusOrderByRequestedAtDesc(
			String deviceCode, ActionLogStatus status);

	Optional<ActionLog> findTopByDevice_DeviceCodeAndStatusAndActionOrderByRequestedAtDesc(
			String deviceCode, ActionLogStatus status, DeviceAction action);

	List<ActionLog> findByBatchIdAndStatus(String batchId, ActionLogStatus status);
	List<ActionLog> findAllByStatusAndBatchIdIsNotNull(ActionLogStatus status);
	List<ActionLog> findAllByStatusAndRequestedAtBefore(ActionLogStatus status, LocalDateTime before);
}
