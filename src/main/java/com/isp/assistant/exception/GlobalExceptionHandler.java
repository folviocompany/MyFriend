package com.isp.assistant.exception;

import java.net.URI;
import java.util.UUID;

import com.isp.assistant.ai.AiProviderAuthenticationException;
import com.isp.assistant.ai.AiProviderRateLimitException;
import com.isp.assistant.ai.AiProviderRequestException;
import com.isp.assistant.ai.AiProviderTimeoutException;
import com.isp.assistant.ai.AiProviderUnavailableException;
import com.isp.assistant.service.ChatValidationException;
import com.isp.assistant.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final URI PROVIDER_RATE_LIMIT_TYPE = URI.create("urn:problem:ai-provider-rate-limit");

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("A requisição contém campos inválidos.");
        return response(HttpStatus.BAD_REQUEST, "Requisição inválida", detail);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ChatValidationException.class})
    ResponseEntity<ProblemDetail> handleBadRequest(Exception exception) {
        String detail = exception instanceof ChatValidationException
                ? exception.getMessage()
                : "O corpo da requisição é inválido.";
        return response(HttpStatus.BAD_REQUEST, "Requisição inválida", detail);
    }

    @ExceptionHandler({AiProviderAuthenticationException.class, AiProviderRequestException.class})
    ResponseEntity<ProblemDetail> handleProviderRejectedRequest() {
        return response(HttpStatus.BAD_GATEWAY, "Falha no provedor de IA",
                "Não foi possível processar a requisição no provedor de IA.");
    }

    @ExceptionHandler(AiProviderRateLimitException.class)
    ResponseEntity<ProblemDetail> handleProviderRateLimit(AiProviderRateLimitException exception) {
        ProblemDetail problem = problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Limite do provedor de IA excedido",
                "O provedor de IA está temporariamente indisponível por limite de uso.");
        problem.setType(PROVIDER_RATE_LIMIT_TYPE);

        HttpHeaders headers = new HttpHeaders();
        if (exception.getRetryAfterSeconds() != null) {
            headers.set(HttpHeaders.RETRY_AFTER, exception.getRetryAfterSeconds().toString());
        }
        return new ResponseEntity<>(problem, headers, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(AiProviderUnavailableException.class)
    ResponseEntity<ProblemDetail> handleProviderUnavailable() {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Provedor de IA indisponível",
                "O provedor de IA está temporariamente indisponível.");
    }

    @ExceptionHandler(AiProviderTimeoutException.class)
    ResponseEntity<ProblemDetail> handleProviderTimeout() {
        return response(HttpStatus.GATEWAY_TIMEOUT, "Tempo limite excedido",
                "O provedor de IA não respondeu dentro do tempo limite.");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        LOGGER.error("Erro inesperado requestId={} exceptionType={}",
                currentRequestId(), exception.getClass().getName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro interno inesperado.");
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status).body(problem(status, title, detail));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("requestId", currentRequestId());
        return problem;
    }

    private String currentRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }
}
