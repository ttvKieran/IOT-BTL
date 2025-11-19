package com.example.demo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Slf4j
public class WeatherService {

    private final RestClient restClient;
    private final String weatherApiKey;

    public WeatherService(RestClient.Builder restClientBuilder,
                          @Value("${weather.api.url}") String weatherApiUrl,
                          @Value("${weather.api.key}") String weatherApiKey) {
        // Khởi tạo RestClient với Base URL
        this.restClient = restClientBuilder.baseUrl(weatherApiUrl).build();
        this.weatherApiKey = weatherApiKey;
    }

    /**
     * Lấy dự báo thời tiết đơn giản cho một vị trí.
     * (Ví dụ: "Hanoi,VN")
     */
    public WeatherForecast getForecast(String location) {
        // Cung cấp giá trị mặc định nếu location bị null
        String effectiveLocation = (location != null && !location.isEmpty()) ? location : "Hanoi,VN";

        try {
            // Xây dựng URL: .../weather?q=Hanoi,VN&appid=...&units=metric&lang=vi
            WeatherApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("")
                            .queryParam("q", effectiveLocation)
                            .queryParam("appid", weatherApiKey)
                            .queryParam("units", "metric") // Lấy nhiệt độ C
                            .queryParam("lang", "vi") // Lấy mô tả Tiếng Việt
                            .build())
                    .retrieve()
                    .body(WeatherApiResponse.class);

            if (response != null && response.getWeather() != null && !response.getWeather().isEmpty()) {
                WeatherForecast forecast = new WeatherForecast();
                forecast.setDescription(response.getWeather().get(0).getDescription());
                forecast.setTemperature(response.getMain().getTemp());
                forecast.setHumidity(response.getMain().getHumidity());
                return forecast;
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Weather API cho {}: {}", effectiveLocation, e.getMessage());
        }
        // Trả về DTO rỗng (hoặc mặc định) nếu có lỗi
        return new WeatherForecast("Không rõ (Lỗi API)", 0, 0);
    }

    // --- DTOs nội bộ để parse JSON từ OpenWeatherMap ---
    // (Bỏ qua các trường không cần thiết)
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherApiResponse {
        private List<WeatherDescription> weather;
        private MainStats main;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherDescription {
        private String description;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MainStats {
        private double temp;
        private int humidity;
    }

    // --- DTO trả về (đơn giản hóa, khớp với DTO của Python) ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherForecast {
        private String description;
        private double temperature;
        private int humidity;

        public WeatherForecast(String description, double temperature, int humidity) {
            this.description = description;
            this.temperature = temperature;
            this.humidity = humidity;
        }
        public WeatherForecast() {}
    }
}