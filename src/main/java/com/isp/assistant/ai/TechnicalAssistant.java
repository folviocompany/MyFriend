package com.isp.assistant.ai;

import java.io.InterruptedIOException;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TechnicalAssistant {

    private static final Logger LOGGER = LoggerFactory.getLogger(TechnicalAssistant.class);

    private final ChatClient chatClient;

    public TechnicalAssistant(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String answer(String userPrompt) {
        try {
            ChatResponse response = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .chatResponse();
            String content = response == null || response.getResult() == null
                    ? null
                    : response.getResult().getOutput().getText();
            if (!StringUtils.hasText(content)) {
                throw new AiProviderUnavailableException(null);
            }
            logSuccess(response);
            return content;
        }
        catch (AiProviderException exception) {
            throw exception;
        }
        catch (ApiException exception) {
            logHttpFailure(exception);
            throw translateApiException(exception);
        }
        catch (GenAiIOException exception) {
            if (hasCause(exception, InterruptedIOException.class)) {
                logTransportFailure("timeout");
                throw new AiProviderTimeoutException(exception);
            }
            logTransportFailure("connection");
            throw new AiProviderUnavailableException(exception);
        }
        catch (RuntimeException exception) {
            ApiException apiException = findCause(exception, ApiException.class);
            if (apiException != null) {
                logHttpFailure(apiException);
                throw translateApiException(apiException);
            }
            GenAiIOException ioException = findCause(exception, GenAiIOException.class);
            if (ioException != null) {
                if (hasCause(ioException, InterruptedIOException.class)) {
                    logTransportFailure("timeout");
                    throw new AiProviderTimeoutException(ioException);
                }
                logTransportFailure("connection");
                throw new AiProviderUnavailableException(ioException);
            }
            logTransportFailure("unexpected");
            throw new AiProviderUnavailableException(exception);
        }
    }

    private void logSuccess(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();
        LOGGER.info(
                "Gemini concluído requestId={} providerStatus={} model={} promptTokens={} completionTokens={} totalTokens={}",
                MDC.get("requestId"), response.getResult().getMetadata().getFinishReason(), metadata.getModel(),
                usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
    }

    private void logHttpFailure(ApiException exception) {
        LOGGER.warn("Gemini falhou requestId={} providerStatus={} category={} retryable={}",
                MDC.get("requestId"), exception.code(), failureCategory(exception.code()),
                isRetryableStatus(exception.code()));
    }

    private void logTransportFailure(String category) {
        LOGGER.warn("Gemini falhou requestId={} providerStatus=unavailable category={} retryable={}",
                MDC.get("requestId"), category, "connection".equals(category));
    }

    private String failureCategory(int status) {
        return switch (status) {
            case 408 -> "timeout";
            case 429 -> "rate_limit";
            case 500, 502, 503, 504 -> "server_error";
            case 400, 404 -> "request_rejected";
            case 401, 403 -> "authentication";
            default -> "http_error";
        };
    }

    private boolean isRetryableStatus(int status) {
        return status == 500 || status == 502 || status == 503 || status == 504;
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
        return findCause(throwable, type) != null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
