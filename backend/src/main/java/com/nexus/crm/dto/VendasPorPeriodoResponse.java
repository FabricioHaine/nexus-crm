package com.nexus.crm.dto;

import java.math.BigDecimal;

/**
 * Evolucao mensal das vendas ganhas.
 */
public record VendasPorPeriodoResponse(
        String periodo,
        long quantidade,
        BigDecimal valorTotal
) {
}
