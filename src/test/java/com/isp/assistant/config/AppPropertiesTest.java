package com.isp.assistant.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AppPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class)
            .withPropertyValues(
                    "app.ai.provider=gemini",
                    "app.ai.max-output-tokens=1024",
                    "app.ai.timeout-seconds=30",
                    "app.chat.max-message-length=2000",
                    "app.api-key=",
                    "app.rate-limit-per-minute=20",
                    "app.knowledge.max-articles=3",
                    "app.knowledge.max-chars=6000");

    @Test
    void bindsValidProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            AppProperties properties = context.getBean(AppProperties.class);
            assertThat(properties.ai().provider()).isEqualTo("gemini");
            assertThat(properties.ai().timeoutSeconds()).isEqualTo(30);
            assertThat(properties.chat().maxMessageLength()).isEqualTo(2000);
        });
    }

    @Test
    void rejectsInvalidTimeout() {
        contextRunner
                .withPropertyValues("app.ai.timeout-seconds=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootCause(context.getStartupFailure()).getMessage())
                            .contains("ai.timeoutSeconds");
                });
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AppProperties.class)
    static class PropertiesConfiguration {
    }
}
