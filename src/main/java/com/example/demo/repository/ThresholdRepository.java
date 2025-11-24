package com.example.demo.repository;

import com.example.demo.model.entity.ThresholdSetting;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThresholdRepository extends BaseRepository<ThresholdSetting, Long>{
    Optional<ThresholdSetting> findByDeviceUid(String deviceUid);
}
