package com.nexus.crm.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato padronizado das respostas de erro da API.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        Map<String, String> campos
) {

    public static ErrorResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErrorResponse(LocalDateTime.now(), status, erro, mensagem, caminho, Map.of());
    }

    public static ErrorResponse de(int status, String erro, String mensagem, String caminho,
                                   Map<String, String> campos) {
        return new ErrorResponse(LocalDateTime.now(), status, erro, mensagem, caminho, campos);
    }
}
