package com.isp.assistant.config;

import com.isp.assistant.controller.ChatController;
import com.isp.assistant.exception.ProblemDetailResponseWriter;
import com.isp.assistant.security.ApiKeyFilter;
import com.isp.assistant.security.RateLimitFilter;
import com.isp.assistant.service.ChatService;
import com.isp.assistant.web.RequestIdFilter;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@EnableConfigurationProperties({
        AppProperties.class,
        SpringDocConfigProperties.class,
        SwaggerUiConfigProperties.class,
        SwaggerUiOAuthProperties.class
})
@Import({
        OpenApiConfig.class,
        ProblemDetailResponseWriter.class,
        RequestIdFilter.class,
        ApiKeyFilter.class,
        RateLimitFilter.class
})
@ImportAutoConfiguration({
        SpringDocConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        SwaggerConfig.class
})
@TestPropertySource(properties = {
        "app.api-key=swagger-test-key",
        "app.rate-limit-per-minute=100"
})
class OpenApiValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @Test
    void publishesDocumentedChatContractAndConditionalApiKeyScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Como diagnosticar perda de pacotes?")))
                .andExpect(content().string(containsString("Verifique latência e perda por salto.")))
                .andExpect(jsonPath("$.info.title").value("ISP Support Assistant API"))
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.summary")
                        .value("Consulta o assistente técnico"))
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.requestBody.content['application/json']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['200']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['400']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['401']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['429']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['500']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['502']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['503']")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/v1/chat'].post.responses['504']")
                        .exists())
                .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.type").value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.name").value("X-API-Key"))
                .andExpect(jsonPath("$.components.securitySchemes.ApiKeyAuth.in").value("header"))
                .andExpect(jsonPath("$.security[0].ApiKeyAuth").isArray());
    }

    @Test
    void exposesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/index.html")));
    }
}
