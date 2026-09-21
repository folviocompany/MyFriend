package com.isp.assistant.live;

import com.isp.assistant.dto.ChatResponse;
import com.isp.assistant.service.ChatService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("live-ai")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_AI_TESTS", matches = "(?i)true")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class GeminiLiveAiTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ChatService chatService;

    @Test
    void answersUsingGeminiAndKnowledgeContext() {
        ChatResponse response = chatService.chat(
                "Qual é a diferença entre CGNAT e endereço IP público?");

        assertThat(response.response()).isNotBlank();
        assertThat(response.provider()).isEqualTo("gemini");
    }
}
