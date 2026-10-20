package com.nexus.crm.dto;

import java.time.LocalDateTime;

/**
 * Representacao de saida de um cliente.
 */
public record ClienteResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        String empresa,
        String cargo,
        String observacoes,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao,
        long totalOportunidades
) {
}
