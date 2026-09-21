package com.isp.assistant.config;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.errors.ServerException;
import com.google.genai.types.HttpOptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class AiConfigTest {

    private final AiConfig config = new AiConfig();

    @Test
    void configuresTimeoutAndDisablesSdkRetryLayer() {
        HttpOptions options = AiConfig.buildHttpOptions(30);

        assertThat(options.timeout()).contains(30_000);
        assertThat(options.retryOptions()).isPresent();
        assertThat(options.retryOptions().orElseThrow().attempts()).contains(1);
        assertThat(options.retryOptions().orElseThrow().httpStatusCodes().orElseThrow())
                .containsExactly(500, 502, 503, 504);
    }

    @Test
    void retriesServerFailureOnlyOnce() {
        AtomicInteger attempts = new AtomicInteger();

        executeIgnoringFailure(config.retryTemplate(), attempts,
                new ServerException(503, "UNAVAILABLE", "provider unavailable"));

        assertThat(attempts).hasValue(2);
    }

    @Test
    void doesNotRetryClientOrRateLimitErrors() {
        AtomicInteger clientAttempts = new AtomicInteger();
        AtomicInteger quotaAttempts = new AtomicInteger();

        executeIgnoringFailure(config.retryTemplate(), clientAttempts,
                new ClientException(400, "INVALID_ARGUMENT", "invalid request"));
        executeIgnoringFailure(config.retryTemplate(), quotaAttempts,
                new ClientException(429, "RESOURCE_EXHAUSTED", "quota exceeded"));

        assertThat(clientAttempts).hasValue(1);
        assertThat(quotaAttempts).hasValue(1);
    }

    @Test
    void retriesConnectionFailureButNotTimeout() {
        AtomicInteger connectionAttempts = new AtomicInteger();
        AtomicInteger timeoutAttempts = new AtomicInteger();

        executeIgnoringFailure(config.retryTemplate(), connectionAttempts,
                new GenAiIOException(new ConnectException("connection refused")));
        executeIgnoringFailure(config.retryTemplate(), timeoutAttempts,
                new GenAiIOException(new SocketTimeoutException("read timed out")));

        assertThat(connectionAttempts).hasValue(2);
        assertThat(timeoutAttempts).hasValue(1);
    }

    private void executeIgnoringFailure(
            RetryTemplate retryTemplate,
            AtomicInteger attempts,
            RuntimeException failure) {
        catchThrowable(() -> retryTemplate.invoke(() -> {
            attempts.incrementAndGet();
            throw failure;
        }));
    }
}
