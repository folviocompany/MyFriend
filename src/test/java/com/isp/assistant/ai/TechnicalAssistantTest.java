package com.isp.assistant.ai;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import com.google.genai.errors.ClientException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.errors.ServerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicalAssistantTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    void returnsProviderContent() {
        when(chatClient.prompt().user("prompt").call().content()).thenReturn("resposta técnica");

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThat(assistant.answer("prompt")).isEqualTo("resposta técnica");
    }

    @Test
    void mapsAuthenticationFailure() {
        when(chatClient.prompt().user("prompt").call().content())
                .thenThrow(new ClientException(401, "UNAUTHENTICATED", "invalid key"));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderAuthenticationException.class);
    }

    @Test
    void mapsRejectedRequestAndQuotaFailure() {
        when(chatClient.prompt().user("bad-request").call().content())
                .thenThrow(new ClientException(404, "NOT_FOUND", "model not found"));
        when(chatClient.prompt().user("quota").call().content())
                .thenThrow(new ClientException(429, "RESOURCE_EXHAUSTED", "quota"));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("bad-request"))
                .isInstanceOf(AiProviderRequestException.class);
        assertThatThrownBy(() -> assistant.answer("quota"))
                .isInstanceOf(AiProviderRateLimitException.class);
    }

    @Test
    void mapsServerAndNetworkFailures() {
        when(chatClient.prompt().user("server").call().content())
                .thenThrow(new ServerException(503, "UNAVAILABLE", "down"));
        when(chatClient.prompt().user("network").call().content())
                .thenThrow(new GenAiIOException(new ConnectException("refused")));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("server"))
                .isInstanceOf(AiProviderUnavailableException.class);
        assertThatThrownBy(() -> assistant.answer("network"))
                .isInstanceOf(AiProviderUnavailableException.class);
    }

    @Test
    void mapsTimeoutWithoutExposingProviderMessage() {
        when(chatClient.prompt().user("prompt").call().content())
                .thenThrow(new GenAiIOException(new SocketTimeoutException("sensitive provider detail")));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderTimeoutException.class)
                .hasMessageNotContaining("sensitive provider detail");
    }

    @Test
    void rejectsEmptyProviderResponse() {
        when(chatClient.prompt().user("prompt").call().content()).thenReturn(" ");

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderUnavailableException.class);
    }
}
