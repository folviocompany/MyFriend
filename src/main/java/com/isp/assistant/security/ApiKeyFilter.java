package com.isp.assistant.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.isp.assistant.config.AppProperties;
import com.isp.assistant.exception.ProblemDetailResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-API-Key";
    private static final URI PROBLEM_TYPE = URI.create("urn:problem:api-key");

    private final String configuredApiKey;
    private final ProblemDetailResponseWriter problemWriter;

    public ApiKeyFilter(AppProperties properties, ProblemDetailResponseWriter problemWriter) {
        this.configuredApiKey = properties.apiKey();
        this.problemWriter = problemWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod()) || !isChatPath(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!StringUtils.hasText(configuredApiKey)
                || keysMatch(configuredApiKey, request.getHeader(HEADER_NAME))) {
            filterChain.doFilter(request, response);
            return;
        }

        problemWriter.write(
                response,
                HttpStatus.UNAUTHORIZED,
                "Chave de API inválida",
                "Informe uma chave de API válida no cabeçalho X-API-Key.",
                PROBLEM_TYPE);
    }

    private boolean keysMatch(String expected, String supplied) {
        if (supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isChatPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "/api/v1/chat".equals(path) || "/api/v1/chat/".equals(path);
    }
}
