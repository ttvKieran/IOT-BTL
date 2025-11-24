-- Bảng lưu cài đặt ngưỡng cho từng thiết bị
CREATE TABLE IF NOT EXISTS threshold_settings (
                                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                  device_uid VARCHAR(100) NOT NULL,

    -- Cài đặt ngưỡng
    min_soil_moisture DECIMAL(5,2) DEFAULT 0, -- Dưới mức này sẽ bật bơm
    max_pump_duration_seconds INT DEFAULT 10, -- Thời gian bơm tối đa (giây)

-- Trạng thái kích hoạt tính năng này
    is_active BOOLEAN DEFAULT FALSE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Đảm bảo mỗi thiết bị chỉ có 1 dòng cài đặt
    UNIQUE KEY unique_device_threshold (device_uid)
    );