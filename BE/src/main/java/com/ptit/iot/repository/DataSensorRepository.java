package com.ptit.iot.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.ptit.iot.entity.DataSensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataSensorRepository extends JpaRepository<DataSensor, Long> {
	@Query("""
			SELECT new com.ptit.iot.repository.SensorChartProjection(
				reading.recordedAt,
				MAX(CASE WHEN sensor.sensorKey = 'temperature' THEN reading.value ELSE NULL END),
				MAX(CASE WHEN sensor.sensorKey = 'humidity' THEN reading.value ELSE NULL END),
				MAX(CASE WHEN sensor.sensorKey = 'light' THEN reading.value ELSE NULL END))
			FROM DataSensor reading
			JOIN reading.sensor sensor
			WHERE reading.recordedAt >= :fromTime
			GROUP BY reading.recordedAt
			ORDER BY reading.recordedAt ASC
			""")
	List<SensorChartProjection> findChartPointsSince(@Param("fromTime") LocalDateTime fromTime);

	@Query(value = """
			SELECT reading
			FROM DataSensor reading
			LEFT JOIN FETCH reading.sensor sensor
			WHERE (:cutoffTime IS NULL OR reading.recordedAt > :cutoffTime
			   OR (reading.recordedAt = :cutoffTime AND reading.id >= :cutoffId))
			  AND (:sensorKey IS NULL OR sensor.sensorKey = :sensorKey)
			  AND (:minValue IS NULL OR reading.value >= :minValue)
			  AND (:maxValue IS NULL OR reading.value <= :maxValue)
			  AND (:fromTime IS NULL OR reading.recordedAt >= :fromTime)
			  AND (:toTime IS NULL OR reading.recordedAt < :toTime)
			""",
		countQuery = """
			SELECT COUNT(reading)
			FROM DataSensor reading
			LEFT JOIN reading.sensor sensor
			WHERE (:cutoffTime IS NULL OR reading.recordedAt > :cutoffTime
			   OR (reading.recordedAt = :cutoffTime AND reading.id >= :cutoffId))
			  AND (:sensorKey IS NULL OR sensor.sensorKey = :sensorKey)
			  AND (:minValue IS NULL OR reading.value >= :minValue)
			  AND (:maxValue IS NULL OR reading.value <= :maxValue)
			  AND (:fromTime IS NULL OR reading.recordedAt >= :fromTime)
			  AND (:toTime IS NULL OR reading.recordedAt < :toTime)
			""")
	Page<DataSensor> findWithFilters(
			@Param("cutoffTime") LocalDateTime cutoffTime,
			@Param("cutoffId") Long cutoffId,
			@Param("sensorKey") String sensorKey,
			@Param("minValue") BigDecimal minValue,
			@Param("maxValue") BigDecimal maxValue,
			@Param("fromTime") LocalDateTime fromTime,
			@Param("toTime") LocalDateTime toTime,
			Pageable pageable);

	@Query(value = "SELECT recorded_at AS recordedAt, id AS id FROM data_sensor "
			+ "ORDER BY recorded_at DESC, id DESC LIMIT 1 OFFSET 9999", nativeQuery = true)
	Optional<SensorRecordCutoff> findRecentRecordsCutoff();

	List<DataSensor> findByStatusIsNull();
	List<DataSensor> findBySensorIsNull();

	Optional<DataSensor> findTopBySensor_SensorKeyOrderByRecordedAtDescIdDesc(String sensorKey);

	List<DataSensor> findByRecordedAtGreaterThanEqualOrderByRecordedAtAscIdAsc(LocalDateTime fromTime);
}
