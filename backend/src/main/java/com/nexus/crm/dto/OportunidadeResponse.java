package com.nexus.crm.dto;

import com.nexus.crm.model.EtapaOportunidade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representacao de saida de uma oportunidade comercial.
 */
public record OportunidadeResponse(
        Long id,
        String titulo,
        String descricao,
        Long clienteId,
        String clienteNome,
        String clienteEmpresa,
        BigDecimal valorEstimado,
        EtapaOportunidade etapa,
        String etapaDescricao,
        Integer probabilidade,
        LocalDateTime dataCriacao,
        LocalDate dataPrevistaFechamento,
        LocalDateTime dataAtualizacao,
        LocalDateTime dataFechamento,
        String motivoPerda
) {
}
