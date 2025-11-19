package com.example.demo.repository;

import com.example.demo.model.entity.DeviceEntity;
import com.example.demo.model.entity.TelemetryLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TelemetryRepository extends  BaseRepository<TelemetryLog, Long> {
    // findAllByDeviceAndLogTimeBetween(device, from, to)
    @Query("SELECT t FROM TelemetryLog t WHERE t.device = ?1 AND t.logTime BETWEEN ?2 AND ?3")
    List<TelemetryLog> findAllByDeviceAndLogTimeBetween(DeviceEntity device, Instant from, Instant to);
}
