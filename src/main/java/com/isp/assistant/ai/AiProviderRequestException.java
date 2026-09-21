package com.isp.assistant.ai;

public final class AiProviderRequestException extends AiProviderException {

    public AiProviderRequestException(Throwable cause) {
        super("O provedor de IA rejeitou a requisição.", cause);
    }
}
