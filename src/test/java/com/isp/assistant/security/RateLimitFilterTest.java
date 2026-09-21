package com.isp.assistant.security;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.isp.assistant.config.AppProperties;
import com.isp.assistant.exception.ProblemDetailResponseWriter;
import jakarta.servlet.FilterChain;
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

class RateLimitFilterTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-21T12:34:30Z"), ZoneOffset.UTC);

    private final ProblemDetailResponseWriter problemWriter =
            new ProblemDetailResponseWriter(new ObjectMapper());

    @Test
    void rejectsRequestsAboveLimitWithRetryAfter() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties("", 2), problemWriter, FIXED_CLOCK);

        assertAllowed(filter, "192.0.2.10");
        assertAllowed(filter, "192.0.2.10");

        MockHttpServletRequest request = chatRequest("192.0.2.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("Limite de requisições excedido")
                .contains("urn:problem:api-rate-limit")
                .contains("requestId");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void keepsSeparateLimitsByIpWhenApiKeyIsDisabled() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties("", 1), problemWriter, FIXED_CLOCK);

        assertAllowed(filter, "192.0.2.10");
        assertAllowed(filter, "192.0.2.11");
    }

    @Test
    void appliesLimitByConfiguredApiKeyAcrossIps() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(
                properties("shared-key", 1), problemWriter, FIXED_CLOCK);

        assertAllowed(filter, "192.0.2.10");

        MockHttpServletResponse response = perform(filter, "192.0.2.11", mock(FilterChain.class));
        assertThat(response.getStatus()).isEqualTo(429);
    }

    @Test
    void doesNotLimitNonChatEndpoints() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(properties("", 1), problemWriter, FIXED_CLOCK);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(any(), any());
    }

    private void assertAllowed(RateLimitFilter filter, String remoteAddress) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse response = perform(filter, remoteAddress, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }

    private MockHttpServletResponse perform(
            RateLimitFilter filter,
            String remoteAddress,
            FilterChain chain) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(chatRequest(remoteAddress), response, chain);
        return response;
    }

    private MockHttpServletRequest chatRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat");
        request.setRemoteAddr(remoteAddress);
        return request;
    }

    private AppProperties properties(String apiKey, int limit) {
        return new AppProperties(
                new AppProperties.Ai("gemini", 1024, 30),
                new AppProperties.Chat(2000),
                apiKey,
                limit,
                new AppProperties.Knowledge(3, 6000));
    }
}
