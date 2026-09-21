package com.isp.assistant.ai;

public final class AiProviderRateLimitException extends AiProviderException {

    private final Integer retryAfterSeconds;

    public AiProviderRateLimitException(Integer retryAfterSeconds, Throwable cause) {
        super("O provedor de IA excedeu o limite de uso.", cause);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
