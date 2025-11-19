// src/main/java/com/example/smartgarden/config/MqttConfig.java

package com.example.demo.configuration;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;

@Configuration
public class MqttConfig {

    // Lấy giá trị từ application.yml
    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.inbound-topic}")
    private String inboundTopic; // "smartgarden/device/+/+"

    @Value("${mqtt.username}")
    private String username;

    @Value("${mqtt.password}")
    private String password;

    /**
     * 1. Factory tạo MqttClient
     * Cấu hình các thông số kết nối cơ bản đến Broker.
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});

        factory.setConnectionOptions(options);

        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true); // Đảm bảo session sạch khi kết nối
        options.setAutomaticReconnect(true); // Tự động kết nối lại
        // Có thể cấu hình username/password nếu broker yêu cầu
        return factory;
    }

    /**
     * 2. Kênh nhận dữ liệu từ MQTT
     * Là DirectChannel, tin nhắn được xử lý đồng bộ trên thread nhận.
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * 3. Adapter lắng nghe MQTT
     * Đăng ký với Broker để lắng nghe tin nhắn từ topic đã cấu hình.
     */
    @Bean
    public MessageProducer inbound() {
        // Tạo adapter với Client ID khác biệt cho luồng inbound
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        clientId + "_inbound",
                        mqttClientFactory(),
                        inboundTopic
                );

        // Thiết lập thời gian chờ hoàn thành
        adapter.setCompletionTimeout(5000);

        // Converter chuyển đổi payload thô sang String (hoặc byte[] nếu không dùng Default)
        adapter.setConverter(new DefaultPahoMessageConverter());

        // Chất lượng dịch vụ (QoS)
        adapter.setQos(1);

        // Đặt kênh đầu ra là kênh sẽ nhận tin nhắn
        adapter.setOutputChannel(mqttInputChannel());

        return adapter;
    }

    /**
     * 4. Kênh gửi dữ liệu ra MQTT
     * Channel này được dùng để gửi tin nhắn ra MQTT broker.
     */
    @Bean
    public MessageChannel mqttOutputChannel() {
        return new DirectChannel();
    }

    /**
     * 5. Handler để gửi đi
     * Lắng nghe mqttOutputChannel và chuyển tin nhắn thành tin nhắn MQTT Paho thực tế.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutputChannel")
    public MessageHandler outbound() {
        // Sử dụng một client ID khác để phân biệt client inbound và outbound
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId + "_outbound", mqttClientFactory());

        // Cho phép gửi tin nhắn bất đồng bộ (không block thread)
        messageHandler.setAsync(true);

        // Thiết lập QoS mặc định cho các tin nhắn đi
        messageHandler.setDefaultQos(1);

        // Thiết lập topic mặc định nếu không có trong Message Header (có thể bỏ qua nếu luôn gửi topic)
        // messageHandler.setDefaultTopic("smartgarden/default/command");

        return messageHandler;
    }

    /**
     * 6. Gateway Interface (để Service gọi dễ dàng)
     * Đây là interface mà các service (như CommandService) sẽ sử dụng.
     * Spring Integration sẽ tự động tạo một proxy class implement interface này.
     */
    @MessagingGateway(defaultRequestChannel = "mqttOutputChannel")
    public interface MqttOutboundGateway {

        /**
         * Gửi payload đến MQTT Broker trên một topic cụ thể.
         * @param payload Dữ liệu (thường là JSON String)
         * @param topic Topic MQTT đích
         */
        void sendToMqtt(String payload, @Header(MqttHeaders.TOPIC) String topic);
    }
}