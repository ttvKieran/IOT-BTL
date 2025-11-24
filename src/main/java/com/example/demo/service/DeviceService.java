package com.example.demo.service;

import com.example.demo.dto.DeviceDto;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;

@Service
public interface DeviceService {
    List<DeviceDto> getAllDevices();
    public DeviceDto createDevice(DeviceDto deviceDto);
    public DeviceDto updateDevice(String deviceUid, DeviceDto deviceDto);
    public void softDeleteDevice(String deviceUid);
    public void restoreDevice(String deviceUid);
    public void setAutoMode(String deviceUid, boolean autoMode);
}
