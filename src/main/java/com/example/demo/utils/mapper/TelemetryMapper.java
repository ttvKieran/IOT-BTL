package com.example.demo.utils.mapper;

import com.example.demo.dto.DeviceStateDTO;
import com.example.demo.dto.TelemetryLogDto;
import com.example.demo.model.entity.TelemetryLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TelemetryMapper {

    @Mapping(source = "lightLevel", target = "light")
    TelemetryLog toEntity(TelemetryLogDto dto);

    @Mapping(source = "light", target = "lightLevel")
    @Mapping(target = "device", ignore = true)
    TelemetryLogDto toDto(TelemetryLog entity);

    TelemetryLog toEntity(DeviceStateDTO.SensorData dto);



}
