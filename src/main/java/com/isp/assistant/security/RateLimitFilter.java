package com.isp.assistant.security;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.isp.assistant.config.AppProperties;
import com.isp.assistant.exception.ProblemDetailResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final URI PROBLEM_TYPE = URI.create("urn:problem:api-rate-limit");
    private static final long WINDOW_SECONDS = 60;

    private final long limit;
    private final String apiKeyIdentity;
    private final ProblemDetailResponseWriter problemWriter;
    private final Clock clock;
    private final Map<String, Window> counters = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupWindow = new AtomicLong(Long.MIN_VALUE);

    @Autowired
    public RateLimitFilter(AppProperties properties, ProblemDetailResponseWriter problemWriter) {
        this(properties, problemWriter, Clock.systemUTC());
    }

    RateLimitFilter(
            AppProperties properties,
            ProblemDetailResponseWriter problemWriter,
            Clock clock) {
        this.limit = properties.rateLimitPerMinute();
        this.apiKeyIdentity = StringUtils.hasText(properties.apiKey())
                ? "key:" + sha256(properties.apiKey())
                : null;
        this.problemWriter = problemWriter;
        this.clock = clock;
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
        long epochSecond = clock.instant().getEpochSecond();
        long currentWindow = Math.floorDiv(epochSecond, WINDOW_SECONDS);
        cleanExpiredEntries(currentWindow);

        Window window = counters.compute(clientIdentity(request), (identity, current) -> {
            if (current == null || current.minute() != currentWindow) {
                return new Window(currentWindow, 1);
            }
            return new Window(currentWindow, current.requests() + 1);
        });

        if (window.requests() <= limit) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = WINDOW_SECONDS - Math.floorMod(epochSecond, WINDOW_SECONDS);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
        problemWriter.write(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "Limite de requisições excedido",
                "Aguarde antes de enviar uma nova requisição.",
                PROBLEM_TYPE);
    }

    private String clientIdentity(HttpServletRequest request) {
        if (apiKeyIdentity != null) {
            return apiKeyIdentity;
        }
        String remoteAddress = request.getRemoteAddr();
        return "ip:" + (StringUtils.hasText(remoteAddress) ? remoteAddress : "unknown");
    }

    private void cleanExpiredEntries(long currentWindow) {
        long previousCleanup = lastCleanupWindow.get();
        if (currentWindow > previousCleanup
                && lastCleanupWindow.compareAndSet(previousCleanup, currentWindow)) {
            counters.entrySet().removeIf(entry -> entry.getValue().minute() < currentWindow - 1);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }

    private boolean isChatPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "/api/v1/chat".equals(path) || "/api/v1/chat/".equals(path);
    }

    private record Window(long minute, long requests) {
    }
}
