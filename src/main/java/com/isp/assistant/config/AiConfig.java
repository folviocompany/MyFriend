package com.isp.assistant.config;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Set;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai", matchIfMissing = true)
public class AiConfig {

    private static final Set<Integer> RETRYABLE_HTTP_STATUS_CODES = Set.of(500, 502, 503, 504);

    @Bean(destroyMethod = "close")
    Client googleGenAiClient(
            @Value("${spring.ai.google.genai.api-key:}") String apiKey,
            AppProperties properties) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("GEMINI_API_KEY deve ser definida para iniciar o cliente Gemini.");
        }

        return Client.builder()
                .apiKey(apiKey)
                .httpOptions(buildHttpOptions(properties.ai().timeoutSeconds()))
                .build();
    }

    @Bean
    RetryTemplate retryTemplate() {
        RetryPolicy retryPolicy = RetryPolicy.builder()
                .maxRetries(1)
                .delay(Duration.ofMillis(250))
                .predicate(AiConfig::isRetryable)
                .build();
        return new RetryTemplate(retryPolicy);
    }

    @Bean
    ChatClient chatClient(
            ChatClient.Builder builder,
            @Value("classpath:prompts/system-prompt.txt") Resource systemPrompt) {
        return builder.defaultSystem(systemPrompt).build();
    }

    static HttpOptions buildHttpOptions(int timeoutSeconds) {
        HttpRetryOptions retryOptions = HttpRetryOptions.builder()
                .attempts(1)
                .httpStatusCodes(RETRYABLE_HTTP_STATUS_CODES.stream().sorted().toList())
                .build();

        return HttpOptions.builder()
                .timeout(Math.multiplyExact(timeoutSeconds, 1_000))
                .retryOptions(retryOptions)
                .build();
    }

    static boolean isRetryable(Throwable throwable) {
        if (throwable instanceof ApiException apiException) {
            return RETRYABLE_HTTP_STATUS_CODES.contains(apiException.code());
        }
        if (throwable instanceof GenAiIOException) {
            return hasCause(throwable, ConnectException.class)
                    || hasCause(throwable, NoRouteToHostException.class)
                    || hasCause(throwable, UnknownHostException.class)
                    || hasNonTimeoutSocketFailure(throwable);
        }
        return false;
    }

    private static boolean hasNonTimeoutSocketFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return false;
            }
            if (current instanceof SocketException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
