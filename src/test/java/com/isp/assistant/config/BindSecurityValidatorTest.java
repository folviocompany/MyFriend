package com.isp.assistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class BindSecurityValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withPropertyValues(
                    "app.ai.provider=gemini",
                    "app.ai.max-output-tokens=1024",
                    "app.ai.timeout-seconds=30",
                    "app.chat.max-message-length=2000",
                    "app.rate-limit-per-minute=20",
                    "app.knowledge.max-articles=3",
                    "app.knowledge.max-chars=6000");

    @Test
    void startsOnLoopbackWithoutApiKey() {
        contextRunner
                .withPropertyValues("server.address=127.0.0.1", "app.api-key=")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void failsContextWithoutStartingServerOnNonLoopbackAndEmptyApiKey() {
        contextRunner
                .withPropertyValues("server.address=0.0.0.0", "app.api-key=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage(
                                    "APP_API_KEY deve ser definida quando SERVER_ADDRESS não é um endereço de loopback.");
                });
    }

    @Test
    void startsOnNonLoopbackWhenApiKeyIsConfigured() {
        contextRunner
                .withPropertyValues("server.address=0.0.0.0", "app.api-key=test-key")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    @Import(BindSecurityValidator.class)
    static class TestConfiguration {
    }
}
