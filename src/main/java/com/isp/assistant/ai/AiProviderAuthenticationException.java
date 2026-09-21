package com.isp.assistant.ai;

public final class AiProviderAuthenticationException extends AiProviderException {

    public AiProviderAuthenticationException(Throwable cause) {
        super("O provedor de IA rejeitou as credenciais.", cause);
    }
}
