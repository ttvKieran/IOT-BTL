// src/main/java/com/example/smartgarden/config/RedisConfig.java

package com.example.demo.configuration;

import com.example.demo.dto.DeviceStateDTO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, DeviceStateDTO> deviceStateRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, DeviceStateDTO> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer: Use String for the device UID keys
        template.setKeySerializer(new StringRedisSerializer());

        // Value Serializer: Use Jackson to serialize DeviceStateDTO into JSON
        Jackson2JsonRedisSerializer<DeviceStateDTO> jsonSerializer =
                new Jackson2JsonRedisSerializer<>(DeviceStateDTO.class);
        template.setValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}