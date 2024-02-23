package net.itfeng.mqttdemoclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class MqttDemoClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(MqttDemoClientApplication.class, args);
    }

}
