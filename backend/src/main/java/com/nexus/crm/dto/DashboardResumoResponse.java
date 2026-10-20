package com.nexus.crm.dto;

import java.math.BigDecimal;

/**
 * Indicadores principais do dashboard.
 *
 * <p>Taxa de conversao = vendas ganhas / (vendas ganhas + vendas perdidas) * 100.
 * Quando nao houver vendas encerradas o valor retornado e zero.</p>
 */
public record DashboardResumoResponse(
        long totalClientes,
        long totalOportunidades,
        long totalOportunidadesAbertas,
        long vendasGanhas,
        long vendasPerdidas,
        BigDecimal valorTotalAbertas,
        BigDecimal valorTotalGanho,
        BigDecimal valorTotalPerdido,
        double taxaConversao
) {
}
