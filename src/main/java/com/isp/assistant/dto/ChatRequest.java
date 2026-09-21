package com.isp.assistant.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank(message = "O campo 'message' é obrigatório e não pode ser vazio.")
        String message) {
}
