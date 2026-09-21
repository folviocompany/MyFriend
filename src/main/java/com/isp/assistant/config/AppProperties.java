package com.isp.assistant.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Ai ai,
        @Valid @NotNull Chat chat,
        String apiKey,
        @Positive int rateLimitPerMinute,
        @Valid @NotNull Knowledge knowledge) {

    public record Ai(
            @NotBlank String provider,
            @Positive int maxOutputTokens,
            @Positive @Max(2_147_483) int timeoutSeconds) {
    }

    public record Chat(@Positive int maxMessageLength) {
    }

    public record Knowledge(
            @Positive int maxArticles,
            @Positive int maxChars) {
    }
}
