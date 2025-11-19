package com.example.demo.model.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@SuperBuilder
@Data
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
@Schema(description = "Base entity class for common properties")
public class BaseEntity {

    @Schema(description = "Timestamp when the entity was created", example = "2023-01-01T12:00:00Z")
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Schema(description = "Timestamp when the entity was last updated", example = "2023-01-02T12:00:00Z")
    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @Schema(description = "Timestamp when the entity was deleted", example = "2023-01-03T12:00:00Z")
    @Column(name = "deleted_at")
    private Instant deletedAt;
}
