package com.isp.assistant.ai;

public abstract class AiProviderException extends RuntimeException {

    protected AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
