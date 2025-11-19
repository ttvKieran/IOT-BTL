package com.example.demo.utils.mapper;

import com.example.demo.dto.DeviceDto;
import com.example.demo.model.entity.DeviceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceEntity toEntity(DeviceDto dto);

    DeviceDto toDto(DeviceEntity entity);
}
