package com.ptit.iot.repository;

import java.util.Optional;

import com.ptit.iot.entity.Sensor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorRepository extends JpaRepository<Sensor, Long> {
	Optional<Sensor> findBySensorKey(String sensorKey);
}
