package com.nexus.crm.dto;

import com.nexus.crm.model.EtapaOportunidade;

import java.math.BigDecimal;

/**
 * Quantidade e valor de oportunidades agrupados por etapa do funil.
 */
public record OportunidadePorEtapaResponse(
        EtapaOportunidade etapa,
        String etapaDescricao,
        long quantidade,
        BigDecimal valorTotal
) {
}
