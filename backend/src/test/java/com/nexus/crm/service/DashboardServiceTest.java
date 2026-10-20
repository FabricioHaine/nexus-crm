package com.nexus.crm.service;

import com.nexus.crm.dto.DashboardResumoResponse;
import com.nexus.crm.dto.OportunidadePorEtapaResponse;
import com.nexus.crm.dto.VendasPorPeriodoResponse;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Calculos dos indicadores do dashboard")
class DashboardServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OportunidadeRepository oportunidadeRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Retorna indicadores zerados sem divisao por zero quando nao ha dados")
    void deveRetornarZerosSemDados() {
        when(clienteRepository.count()).thenReturn(0L);
        when(oportunidadeRepository.count()).thenReturn(0L);
        when(oportunidadeRepository.countByEtapa(EtapaOportunidade.GANHO)).thenReturn(0L);
        when(oportunidadeRepository.countByEtapa(EtapaOportunidade.PERDIDO)).thenReturn(0L);
        when(oportunidadeRepository.countByEtapaIn(anyCollection())).thenReturn(0L);
        when(oportunidadeRepository.somarValorPorEtapas(anyCollection())).thenReturn(BigDecimal.ZERO);

        DashboardResumoResponse resumo = dashboardService.resumo();

        assertThat(resumo.totalClientes()).isZero();
        assertThat(resumo.totalOportunidades()).isZero();
        assertThat(resumo.vendasGanhas()).isZero();
        assertThat(resumo.vendasPerdidas()).isZero();
        assertThat(resumo.taxaConversao()).isEqualTo(0.0);
        assertThat(resumo.valorTotalAbertas()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Calcula a taxa de conversao a partir das vendas ganhas e perdidas")
    void deveCalcularTaxaDeConversao() {
        when(clienteRepository.count()).thenReturn(3L);
        when(oportunidadeRepository.count()).thenReturn(5L);
        when(oportunidadeRepository.countByEtapa(EtapaOportunidade.GANHO)).thenReturn(3L);
        when(oportunidadeRepository.countByEtapa(EtapaOportunidade.PERDIDO)).thenReturn(1L);
        when(oportunidadeRepository.countByEtapaIn(anyCollection())).thenReturn(1L);
        when(oportunidadeRepository.somarValorPorEtapas(anyCollection()))
                .thenAnswer(invocacao -> {
                    var etapas = invocacao.getArgument(0, java.util.Collection.class);
                    return etapas.contains(EtapaOportunidade.GANHO)
                            ? BigDecimal.valueOf(90000)
                            : BigDecimal.valueOf(20000);
                });

        DashboardResumoResponse resumo = dashboardService.resumo();

        assertThat(resumo.totalClientes()).isEqualTo(3L);
        assertThat(resumo.totalOportunidades()).isEqualTo(5L);
        assertThat(resumo.vendasGanhas()).isEqualTo(3L);
        assertThat(resumo.vendasPerdidas()).isEqualTo(1L);
        assertThat(resumo.totalOportunidadesAbertas()).isEqualTo(1L);
        assertThat(resumo.taxaConversao()).isEqualTo(75.0);
        assertThat(resumo.valorTotalGanho()).isEqualByComparingTo("90000");
    }

    @Test
    @DisplayName("Devolve todas as etapas do funil, mesmo as que nao possuem registros")
    void deveDevolverTodasAsEtapas() {
        when(oportunidadeRepository.agruparPorEtapa()).thenReturn(List.of(
                new OportunidadeRepository.ProjecaoEtapa() {
                    @Override
                    public EtapaOportunidade getEtapa() {
                        return EtapaOportunidade.PROPOSTA;
                    }

                    @Override
                    public Long getTotal() {
                        return 4L;
                    }

                    @Override
                    public BigDecimal getValor() {
                        return BigDecimal.valueOf(10000);
                    }
                }));

        List<OportunidadePorEtapaResponse> etapas = dashboardService.oportunidadesPorEtapa();

        assertThat(etapas).hasSize(EtapaOportunidade.values().length);
        assertThat(etapas.get(0).etapa()).isEqualTo(EtapaOportunidade.NOVO_CONTATO);
        assertThat(etapas.get(0).quantidade()).isZero();
        assertThat(etapas.get(2).etapa()).isEqualTo(EtapaOportunidade.PROPOSTA);
        assertThat(etapas.get(2).quantidade()).isEqualTo(4L);
        assertThat(etapas.get(2).valorTotal()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("Agrupa as vendas ganhas por mes mantendo os meses sem vendas")
    void deveAgruparVendasPorMes() {
        when(oportunidadeRepository.listarFechadasNoPeriodo(any(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        List<VendasPorPeriodoResponse> periodos = dashboardService.vendasPorPeriodo(6);

        assertThat(periodos).hasSize(6);
        assertThat(periodos).allSatisfy(periodo -> {
            assertThat(periodo.quantidade()).isZero();
            assertThat(periodo.valorTotal()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(periodos.get(0).periodo()).matches("\\d{4}-\\d{2}");
    }
}
