package com.isp.assistant.security;

import java.io.IOException;

import com.isp.assistant.config.AppProperties;
import com.isp.assistant.exception.ProblemDetailResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ApiKeyFilterTest {

    private final ProblemDetailResponseWriter problemWriter =
            new ProblemDetailResponseWriter(new ObjectMapper());

    @Test
    void allowsRequestWhenApiKeyIsNotConfigured() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(properties(""), problemWriter);
        MockHttpServletRequest request = chatRequest();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void rejectsMissingOrInvalidApiKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(properties("expected-key"), problemWriter);

        assertUnauthorized(filter, null);
        assertUnauthorized(filter, "wrong-key");
    }

    @Test
    void allowsValidApiKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(properties("expected-key"), problemWriter);
        MockHttpServletRequest request = chatRequest();
        request.addHeader(ApiKeyFilter.HEADER_NAME, "expected-key");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    @Test
    void doesNotProtectSwaggerEndpoints() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(properties("expected-key"), problemWriter);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui.html");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    private void assertUnauthorized(ApiKeyFilter filter, String suppliedKey)
            throws ServletException, IOException {
        MockHttpServletRequest request = chatRequest();
        if (suppliedKey != null) {
            request.addHeader(ApiKeyFilter.HEADER_NAME, suppliedKey);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("Chave de API inválida")
                .contains("requestId");
        verify(chain, never()).doFilter(any(), any());
    }

    private MockHttpServletRequest chatRequest() {
        return new MockHttpServletRequest("POST", "/api/v1/chat");
    }

    private AppProperties properties(String apiKey) {
        return new AppProperties(
                new AppProperties.Ai("gemini", 1024, 30),
                new AppProperties.Chat(2000),
                apiKey,
                20,
                new AppProperties.Knowledge(3, 6000));
    }
}
