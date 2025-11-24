package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "threshold_settings")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ThresholdSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_uid", nullable = false, unique = true)
    private String deviceUid;

    @Column(name = "min_soil_moisture")
    private BigDecimal minSoilMoisture; // Ví dụ: 30.0 (30%)

    @Column(name = "max_pump_duration_seconds")
    private Integer maxPumpDurationSeconds; // Ví dụ: 15s

    @Column(name = "is_active")
    private Boolean isActive; // True = Đang bật chế độ tự động theo ngưỡng này
}
