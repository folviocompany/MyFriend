package com.isp.assistant;

import com.isp.assistant.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class IspAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(IspAssistantApplication.class, args);
    }
}
