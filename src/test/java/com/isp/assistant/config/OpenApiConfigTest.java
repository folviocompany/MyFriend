package com.isp.assistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    @Test
    void omitsApiKeySchemeWhenApiKeyIsNotConfigured() {
        OpenAPI openApi = new OpenApiConfig().applicationOpenApi(properties(""));

        assertThat(openApi.getComponents()).isNull();
        assertThat(openApi.getSecurity()).isNullOrEmpty();
    }

    @Test
    void includesApiKeySchemeWhenApiKeyIsConfigured() {
        OpenAPI openApi = new OpenApiConfig().applicationOpenApi(properties("test-key"));

        assertThat(openApi.getComponents().getSecuritySchemes())
                .containsKey(OpenApiConfig.API_KEY_SCHEME);
        assertThat(openApi.getSecurity()).hasSize(1);
    }

    private AppProperties properties(String apiKey) {
        return new AppProperties(
                new AppProperties.Ai("gemini", 1024, 30),
                new AppProperties.Chat(2000),
                apiKey,
                20,
                new AppProperties.Knowledge(3, 6000));
    }
}
