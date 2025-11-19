package com.example.demo.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "telemetry_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TelemetryLog extends  BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết tới Device
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false, foreignKey = @ForeignKey(name = "fk_telemetry_device"))
    private DeviceEntity device;

    @Column(name = "log_time", nullable = false)
    private Instant logTime;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "air_humidity", precision = 5, scale = 2)
    private BigDecimal airHumidity;

    @Column(name = "light", precision = 10, scale = 2)
    private BigDecimal light;

    @Column(name = "soil_moisture", precision = 5, scale = 2)
    private BigDecimal soilMoisture;
}
