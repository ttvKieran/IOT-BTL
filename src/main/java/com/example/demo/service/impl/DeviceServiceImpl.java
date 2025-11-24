package com.example.demo.service.impl;

import com.example.demo.dto.DeviceDto;
import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.entity.DeviceEntity;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.service.DeviceService;
import com.example.demo.utils.mapper.DeviceMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeviceServiceImpl implements DeviceService {
    private static final String REDIS_KEY_PREFIX = "device:autoMode:";
    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper; // MapStruct Mapper
    private final RedisTemplate<String, DeviceStateDTO> deviceStateRedisTemplate;


    private String createKey(String deviceUid) {
        return REDIS_KEY_PREFIX + deviceUid;
    }


    /**
     * 2. Lấy danh sách các thiết bị CỦA TÔI.
     */
    public List<DeviceDto> getAllDevices() {
        List<DeviceEntity> devices = deviceRepository.findAll();
        return devices.stream()
                .map(deviceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public DeviceDto createDevice(DeviceDto deviceDto) {
        log.info("createDevice");
        DeviceEntity deviceEntity = deviceMapper.toEntity(deviceDto);
        return deviceMapper.toDto(deviceRepository.save(deviceEntity));
    }

    @Transactional
    @Override
    public DeviceDto updateDevice(String deviceUid, DeviceDto deviceDto) {
        log.info("updateDevice");
        DeviceEntity existingDevice = deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        // Cập nhật các trường cần thiết
        existingDevice.setName(deviceDto.getName() != null ? deviceDto.getName() : existingDevice.getName());
        return deviceMapper.toDto(deviceRepository.save(existingDevice));
    }

    @Transactional
    @Override
    public void softDeleteDevice(String deviceUid) {
        log.info("softDeleteDevice");
        DeviceEntity existingDevice = deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        deviceRepository.softDeleteByIds(List.of(existingDevice.getId()));
        deviceRepository.save(existingDevice);
    }

    @Transactional
    @Override
    public void restoreDevice(String deviceUid) {
        log.info("restoreDevice");
        DeviceEntity existingDevice = deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        deviceRepository.restoreById(existingDevice.getId());
        deviceRepository.save(existingDevice);
    }

    @Override
    public void setAutoMode(String deviceUid, boolean autoMode) {
        String key = createKey(deviceUid);
        log.info("setAutoMode");
        DeviceEntity existingDevice = deviceRepository.findByDeviceUid(deviceUid)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        existingDevice.setAutoMode(autoMode);
        deviceRepository.save(existingDevice);
        deviceStateRedisTemplate.opsForValue().set(key, new DeviceStateDTO().builder().controlMode(autoMode == true ? "AUTO" : "MANUAL").build());

        log.info("setAutoMode completed");
    }
}
