package com.isp.assistant.ai;

public final class AiProviderTimeoutException extends AiProviderException {

    public AiProviderTimeoutException(Throwable cause) {
        super("O provedor de IA excedeu o tempo limite.", cause);
    }
}
