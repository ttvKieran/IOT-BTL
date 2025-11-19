package com.example.demo.service;

import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.dto.TelemetryLogDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public interface TelemetryService {
    public void saveTelemetryLog(DeviceStateDTO updatedState);

    public List<TelemetryLogDto> getHistory(String deviceUid, Instant from, Instant to);
}
