-- Bảng thiết bị
CREATE TABLE IF NOT EXISTS devices (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       device_uid VARCHAR(100) NOT NULL UNIQUE,
                                       name VARCHAR(255) NOT NULL,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                       deleted_at TIMESTAMP NULL
);

-- Bảng telemetry
CREATE TABLE IF NOT EXISTS telemetry_logs (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              device_id BIGINT NOT NULL,
                                              log_time TIMESTAMP NOT NULL,
                                              temperature DECIMAL(5,2),
                                              air_humidity DECIMAL(5,2),
                                              light DECIMAL(10,2),
                                              soil_moisture DECIMAL(5,2),
                                              created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                              updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                              deleted_at TIMESTAMP NULL,
                                              FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE CASCADE
);

-- Index tăng tốc truy vấn
CREATE INDEX idx_telemetry_device_time ON telemetry_logs(device_id, log_time DESC);
CREATE INDEX idx_device_uid ON devices(device_uid);
