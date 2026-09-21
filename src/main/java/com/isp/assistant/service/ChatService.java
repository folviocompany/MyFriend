package com.isp.assistant.service;

import com.isp.assistant.ai.TechnicalAssistant;
import com.isp.assistant.config.AppProperties;
import com.isp.assistant.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);
    private static final String NO_KNOWLEDGE_CONTEXT = "Nenhum artigo relevante encontrado.";
    private static final String USER_PROMPT_TEMPLATE = """
            <contexto_base_conhecimento>
            %s
            </contexto_base_conhecimento>

            <pergunta_analista>
            %s
            </pergunta_analista>
            """;

    private final TechnicalAssistant technicalAssistant;
    private final AppProperties properties;

    public ChatService(TechnicalAssistant technicalAssistant, AppProperties properties) {
        this.technicalAssistant = technicalAssistant;
        this.properties = properties;
    }

    public ChatResponse chat(String message) {
        String normalizedMessage = message == null ? "" : message.strip();
        if (normalizedMessage.isEmpty()) {
            throw new ChatValidationException("O campo 'message' é obrigatório e não pode ser vazio.");
        }
        if (normalizedMessage.length() > properties.chat().maxMessageLength()) {
            throw new ChatValidationException("O campo 'message' deve ter no máximo "
                    + properties.chat().maxMessageLength() + " caracteres.");
        }

        String userPrompt = USER_PROMPT_TEMPLATE.formatted(
                NO_KNOWLEDGE_CONTEXT,
                neutralizeDelimiters(normalizedMessage));
        long startedAt = System.nanoTime();
        try {
            String response = technicalAssistant.answer(userPrompt);
            LOGGER.info("Chat concluído requestId={} result=success provider={} promptChars={} latencyMs={}",
                    MDC.get("requestId"), properties.ai().provider(), userPrompt.length(), elapsedMillis(startedAt));
            return new ChatResponse(response, properties.ai().provider());
        }
        catch (RuntimeException exception) {
            LOGGER.info("Chat concluído requestId={} result={} provider={} promptChars={} latencyMs={}",
                    MDC.get("requestId"), exception.getClass().getSimpleName(), properties.ai().provider(),
                    userPrompt.length(), elapsedMillis(startedAt));
            throw exception;
        }
    }

    private String neutralizeDelimiters(String value) {
        return value
                .replace("<contexto_base_conhecimento>", "&lt;contexto_base_conhecimento&gt;")
                .replace("</contexto_base_conhecimento>", "&lt;/contexto_base_conhecimento&gt;")
                .replace("<pergunta_analista>", "&lt;pergunta_analista&gt;")
                .replace("</pergunta_analista>", "&lt;/pergunta_analista&gt;");
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
