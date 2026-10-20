package com.nexus.crm.controller;

import com.nexus.crm.dto.DashboardResumoResponse;
import com.nexus.crm.dto.OportunidadePorEtapaResponse;
import com.nexus.crm.dto.VendasPorPeriodoResponse;
import com.nexus.crm.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints dos indicadores do dashboard.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Indicadores comerciais calculados sobre os dados reais")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Indicadores gerais do painel")
    public DashboardResumoResponse resumo() {
        return dashboardService.resumo();
    }

    @GetMapping("/oportunidades-por-etapa")
    @Operation(summary = "Distribuicao das oportunidades por etapa do funil")
    public List<OportunidadePorEtapaResponse> oportunidadesPorEtapa() {
        return dashboardService.oportunidadesPorEtapa();
    }

    @GetMapping("/vendas-por-periodo")
    @Operation(summary = "Evolucao mensal das vendas ganhas")
    public List<VendasPorPeriodoResponse> vendasPorPeriodo(
            @RequestParam(defaultValue = "6") int meses) {
        return dashboardService.vendasPorPeriodo(meses);
    }
}
