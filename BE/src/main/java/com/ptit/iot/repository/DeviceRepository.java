package com.ptit.iot.repository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

import com.ptit.iot.entity.Device;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, Long> {
	Optional<Device> findByDeviceCode(String deviceCode);
	List<Device> findAllByOrderByIdAsc();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT device FROM Device device WHERE device.deviceCode = :deviceCode")
	Optional<Device> findByDeviceCodeForUpdate(@Param("deviceCode") String deviceCode);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT device FROM Device device WHERE device.deviceCode IN :deviceCodes ORDER BY device.id")
	List<Device> findByDeviceCodesForUpdate(@Param("deviceCodes") Collection<String> deviceCodes);
}
