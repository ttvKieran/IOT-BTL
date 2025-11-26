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
     * Lấy dự báo thời tiết với thông tin hiện tại và tương lai (3 giờ tới).
     * (Ví dụ: "Hanoi,VN")
     */
    public WeatherForecast getForecast(String location) {
        // Cung cấp giá trị mặc định nếu location bị null
        String effectiveLocation = (location != null && !location.isEmpty()) ? location : "Hanoi,VN";

        try {
            // Gọi Forecast API: .../forecast?q=Hanoi,VN&appid=...&units=metric&lang=vi&cnt=2
            // cnt=2 lấy 2 time slots (hiện tại + 3 giờ tới)
            ForecastApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("")
                            .queryParam("q", effectiveLocation)
                            .queryParam("appid", weatherApiKey)
                            .queryParam("units", "metric") // Lấy nhiệt độ C
                            .queryParam("lang", "vi") // Lấy mô tả Tiếng Việt
                            .queryParam("cnt", "2") // Lấy 2 khoảng thời gian (0h và 3h tới)
                            .build())
                    .retrieve()
                    .body(ForecastApiResponse.class);

            if (response != null && response.getList() != null && !response.getList().isEmpty()) {
                WeatherForecast forecast = new WeatherForecast();
                
                // Lấy thông tin thời tiết hiện tại (time slot đầu tiên)
                ForecastItem current = response.getList().get(0);
                forecast.setDescription(current.getWeather().get(0).getDescription());
                forecast.setTemperature(current.getMain().getTemp());
                forecast.setHumidity(current.getMain().getHumidity());
                
                // Lấy thông tin dự báo 3 giờ tới (nếu có)
                if (response.getList().size() > 1) {
                    ForecastItem next = response.getList().get(1);
                    forecast.setNextDescription(next.getWeather().get(0).getDescription());
                    forecast.setNextTemperature(next.getMain().getTemp());
                    
                    // Kiểm tra xác suất mưa trong 3 giờ tới
                    if (next.getRain() != null && next.getRain().get3h() != null && next.getRain().get3h() > 0) {
                        forecast.setRainExpected(true);
                        forecast.setRainAmount(next.getRain().get3h());
                    } else {
                        forecast.setRainExpected(false);
                    }
                } else {
                    // Không có dữ liệu dự báo, giả định không mưa
                    forecast.setNextDescription(forecast.getDescription());
                    forecast.setNextTemperature(forecast.getTemperature());
                    forecast.setRainExpected(false);
                }
                
                log.info("Weather forecast for {}: Current: {}°C, {}. Next 3h: {}°C, {}. Rain expected: {}", 
                    effectiveLocation, forecast.getTemperature(), forecast.getDescription(),
                    forecast.getNextTemperature(), forecast.getNextDescription(), forecast.isRainExpected());
                
                return forecast;
            }
        } catch (Exception e) {
            log.error("Lỗi khi gọi Forecast API cho {}: {}", effectiveLocation, e.getMessage());
        }
        // Trả về DTO rỗng (hoặc mặc định) nếu có lỗi
        return new WeatherForecast("Không rõ (Lỗi API)", 0, 0, false, "Không rõ", 0, 0);
    }

    // --- DTOs nội bộ để parse JSON từ OpenWeatherMap Forecast API ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastApiResponse {
        private List<ForecastItem> list;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastItem {
        private MainStats main;
        private List<WeatherDescription> weather;
        private RainInfo rain;
    }

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

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RainInfo {
        @com.fasterxml.jackson.annotation.JsonProperty("3h")
        private Double _3h;
        
        public Double get3h() {
            return _3h;
        }
    }

    // --- DTO trả về (bổ sung thông tin dự báo tương lai) ---
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherForecast {
        // Thông tin hiện tại
        private String description;
        private double temperature;
        private int humidity;
        
        // Thông tin dự báo 3 giờ tới
        private boolean rainExpected;
        private String nextDescription;
        private double nextTemperature;
        private double rainAmount; // mm

        public WeatherForecast(String description, double temperature, int humidity, 
                              boolean rainExpected, String nextDescription, double nextTemperature, double rainAmount) {
            this.description = description;
            this.temperature = temperature;
            this.humidity = humidity;
            this.rainExpected = rainExpected;
            this.nextDescription = nextDescription;
            this.nextTemperature = nextTemperature;
            this.rainAmount = rainAmount;
        }
        public WeatherForecast() {}
    }
}