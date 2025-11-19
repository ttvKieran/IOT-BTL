// src/main/java/com/example/smartgarden/dto/CommandRequestDTO.java

package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class CommandRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Action must not be empty")
    private String action; // Ví dụ: "SET_PUMP_STATE", "SET_MODE"

    // Payload (dữ liệu chi tiết của lệnh)
    // Ví dụ: { "state": "ON" } hoặc { "mode": "MANUAL" }
    private Map<String, Object> payload;
}