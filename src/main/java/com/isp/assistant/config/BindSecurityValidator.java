package com.isp.assistant.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class BindSecurityValidator {

    public BindSecurityValidator(
            @Value("${server.address:127.0.0.1}") String serverAddress,
            AppProperties properties) {
        validate(serverAddress, properties.apiKey());
    }

    static void validate(String serverAddress, String apiKey) {
        if (!isLoopback(serverAddress) && !StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "APP_API_KEY deve ser definida quando SERVER_ADDRESS não é um endereço de loopback.");
        }
    }

    private static boolean isLoopback(String serverAddress) {
        try {
            return InetAddress.getByName(serverAddress).isLoopbackAddress();
        }
        catch (UnknownHostException exception) {
            return false;
        }
    }
}
