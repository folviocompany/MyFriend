package com.isp.assistant.controller;

import com.isp.assistant.ai.AiProviderAuthenticationException;
import com.isp.assistant.ai.AiProviderRateLimitException;
import com.isp.assistant.ai.AiProviderTimeoutException;
import com.isp.assistant.ai.AiProviderUnavailableException;
import com.isp.assistant.config.AppProperties;
import com.isp.assistant.dto.ChatResponse;
import com.isp.assistant.exception.GlobalExceptionHandler;
import com.isp.assistant.exception.ProblemDetailResponseWriter;
import com.isp.assistant.security.ApiKeyFilter;
import com.isp.assistant.security.RateLimitFilter;
import com.isp.assistant.service.ChatService;
import com.isp.assistant.service.ChatValidationException;
import com.isp.assistant.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@EnableConfigurationProperties(AppProperties.class)
@Import({
        GlobalExceptionHandler.class,
        ProblemDetailResponseWriter.class,
        RequestIdFilter.class,
        ApiKeyFilter.class,
        RateLimitFilter.class
})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void returnsChatResponseAndRequestId() throws Exception {
        when(chatService.chat("Como verificar latência?"))
                .thenReturn(new ChatResponse("**Resumo**\nVerifique a rota.", "gemini"));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Como verificar latência?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists(RequestIdFilter.HEADER_NAME))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.response").value("**Resumo**\nVerifique a rota."))
                .andExpect(jsonPath("$.provider").value("gemini"));
    }

    @Test
    void returnsProblemDetailForBlankMessage() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.detail")
                        .value("O campo 'message' é obrigatório e não pode ser vazio."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void returnsProblemDetailForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O corpo da requisição é inválido."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void returnsProblemDetailForConfiguredLengthViolation() throws Exception {
        when(chatService.chat(anyString()))
                .thenThrow(new ChatValidationException("O campo 'message' deve ter no máximo 10 caracteres."));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"mensagem longa"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("O campo 'message' deve ter no máximo 10 caracteres."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsProviderAuthenticationToBadGateway() throws Exception {
        when(chatService.chat(anyString())).thenThrow(new AiProviderAuthenticationException(null));

        performValidRequest()
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail")
                        .value("Não foi possível processar a requisição no provedor de IA."))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsProviderQuotaToServiceUnavailableWithDistinctType() throws Exception {
        when(chatService.chat(anyString())).thenThrow(new AiProviderRateLimitException(15, null));

        performValidRequest()
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "15"))
                .andExpect(jsonPath("$.type").value("urn:problem:ai-provider-rate-limit"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void mapsProviderUnavailableAndTimeout() throws Exception {
        when(chatService.chat("indisponível")).thenThrow(new AiProviderUnavailableException(null));
        when(chatService.chat("timeout")).thenThrow(new AiProviderTimeoutException(null));

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"indisponível"}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.requestId").isNotEmpty());

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"timeout"}
                                """))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    private org.springframework.test.web.servlet.ResultActions performValidRequest() throws Exception {
        return mockMvc.perform(post("/api/v1/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"message":"teste"}
                        """));
    }
}
