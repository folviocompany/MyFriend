package com.isp.assistant.ai;

public final class AiProviderUnavailableException extends AiProviderException {

    public AiProviderUnavailableException(Throwable cause) {
        super("O provedor de IA está indisponível.", cause);
    }
}
