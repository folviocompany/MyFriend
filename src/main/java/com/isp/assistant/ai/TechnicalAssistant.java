package com.isp.assistant.ai;

import java.io.InterruptedIOException;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TechnicalAssistant {

    private final ChatClient chatClient;

    public TechnicalAssistant(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String answer(String userPrompt) {
        try {
            String response = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();
            if (!StringUtils.hasText(response)) {
                throw new AiProviderUnavailableException(null);
            }
            return response;
        }
        catch (AiProviderException exception) {
            throw exception;
        }
        catch (ApiException exception) {
            throw translateApiException(exception);
        }
        catch (GenAiIOException exception) {
            if (hasCause(exception, InterruptedIOException.class)) {
                throw new AiProviderTimeoutException(exception);
            }
            throw new AiProviderUnavailableException(exception);
        }
        catch (RuntimeException exception) {
            throw new AiProviderUnavailableException(exception);
        }
    }

    private AiProviderException translateApiException(ApiException exception) {
        return switch (exception.code()) {
            case 400, 404 -> new AiProviderRequestException(exception);
            case 401, 403 -> new AiProviderAuthenticationException(exception);
            case 408 -> new AiProviderTimeoutException(exception);
            case 429 -> new AiProviderRateLimitException(null, exception);
            case 500, 502, 503, 504 -> new AiProviderUnavailableException(exception);
            default -> new AiProviderUnavailableException(exception);
        };
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
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
