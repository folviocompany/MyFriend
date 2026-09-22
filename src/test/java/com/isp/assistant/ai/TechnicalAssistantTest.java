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
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class TechnicalAssistantTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Test
    void returnsProviderContentAndLogsMetadata(CapturedOutput output) {
        when(chatClient.prompt().user("entrada-sensivel").call().chatResponse())
                .thenReturn(response("resposta técnica", "STOP", "gemini-test"));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThat(assistant.answer("entrada-sensivel")).isEqualTo("resposta técnica");
        assertThat(output).contains("providerStatus=STOP", "model=gemini-test");
        assertThat(output).doesNotContain("resposta técnica", "entrada-sensivel");
    }

    @Test
    void mapsAuthenticationFailure() {
        when(chatClient.prompt().user("prompt").call().chatResponse())
                .thenThrow(new ClientException(401, "UNAUTHENTICATED", "invalid key"));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderAuthenticationException.class);
    }

    @Test
    void mapsRejectedRequestAndQuotaFailure() {
        when(chatClient.prompt().user("bad-request").call().chatResponse())
                .thenThrow(new ClientException(404, "NOT_FOUND", "model not found"));
        when(chatClient.prompt().user("quota").call().chatResponse())
                .thenThrow(new ClientException(429, "RESOURCE_EXHAUSTED", "quota"));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("bad-request"))
                .isInstanceOf(AiProviderRequestException.class);
        assertThatThrownBy(() -> assistant.answer("quota"))
                .isInstanceOf(AiProviderRateLimitException.class);
    }

    @Test
    void mapsServerAndNetworkFailures(CapturedOutput output) {
        when(chatClient.prompt().user("server").call().chatResponse())
                .thenThrow(new ServerException(503, "UNAVAILABLE", "down"));
        when(chatClient.prompt().user("network").call().chatResponse())
                .thenThrow(new GenAiIOException(new ConnectException("refused")));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("server"))
                .isInstanceOf(AiProviderUnavailableException.class);
        assertThatThrownBy(() -> assistant.answer("network"))
                .isInstanceOf(AiProviderUnavailableException.class);
        assertThat(output).contains("providerStatus=503", "category=server_error", "category=connection");
    }

    @Test
    void mapsTimeoutWithoutExposingProviderMessage(CapturedOutput output) {
        when(chatClient.prompt().user("prompt").call().chatResponse())
                .thenThrow(new GenAiIOException(new SocketTimeoutException("sensitive provider detail")));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderTimeoutException.class)
                .hasMessageNotContaining("sensitive provider detail");
        assertThat(output).contains("category=timeout", "retryable=false")
                .doesNotContain("sensitive provider detail");
    }

    @Test
    void unwrapsRuntimeExceptionWithProviderHttpCause(CapturedOutput output) {
        when(chatClient.prompt().user("prompt").call().chatResponse())
                .thenThrow(new RuntimeException(
                        new ClientException(429, "RESOURCE_EXHAUSTED", "sensitive quota detail")));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderRateLimitException.class);
        assertThat(output).contains("providerStatus=429", "category=rate_limit", "retryable=false")
                .doesNotContain("sensitive quota detail");
    }

    @Test
    void unwrapsRuntimeExceptionWithTimeoutCause(CapturedOutput output) {
        when(chatClient.prompt().user("prompt").call().chatResponse())
                .thenThrow(new RuntimeException(
                        new GenAiIOException(new SocketTimeoutException("sensitive timeout detail"))));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderTimeoutException.class);
        assertThat(output).contains("category=timeout", "retryable=false")
                .doesNotContain("sensitive timeout detail");
    }

    @Test
    void rejectsEmptyProviderResponse() {
        when(chatClient.prompt().user("prompt").call().chatResponse())
                .thenReturn(response(" ", "STOP", "gemini-test"));

        TechnicalAssistant assistant = new TechnicalAssistant(chatClient);

        assertThatThrownBy(() -> assistant.answer("prompt"))
                .isInstanceOf(AiProviderUnavailableException.class);
    }

    private ChatResponse response(String content, String finishReason, String model) {
        Generation generation = new Generation(
                new AssistantMessage(content),
                ChatGenerationMetadata.builder().finishReason(finishReason).build());
        return new ChatResponse(
                java.util.List.of(generation),
                ChatResponseMetadata.builder().model(model).build());
    }
}
