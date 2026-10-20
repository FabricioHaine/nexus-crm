package com.nexus.crm.service;

import com.nexus.crm.dto.DashboardResumoResponse;
import com.nexus.crm.dto.OportunidadePorEtapaResponse;
import com.nexus.crm.dto.VendasPorPeriodoResponse;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Monta os indicadores do dashboard a partir dos dados reais persistidos.
 */
@Service
public class DashboardService {

    private static final DateTimeFormatter FORMATO_PERIODO = DateTimeFormatter.ofPattern("yyyy-MM");

    private final ClienteRepository clienteRepository;
    private final OportunidadeRepository oportunidadeRepository;

    public DashboardService(ClienteRepository clienteRepository, OportunidadeRepository oportunidadeRepository) {
        this.clienteRepository = clienteRepository;
        this.oportunidadeRepository = oportunidadeRepository;
    }

    /**
     * Indicadores gerais do painel.
     *
     * <p>Taxa de conversao = ganhas / (ganhas + perdidas) * 100. Se nao houver
     * vendas encerradas a taxa e zero.</p>
     */
    @Transactional(readOnly = true)
    public DashboardResumoResponse resumo() {
        long ganhas = oportunidadeRepository.countByEtapa(EtapaOportunidade.GANHO);
        long perdidas = contarPerdidas();
        long abertas = oportunidadeRepository.countByEtapaIn(EtapaOportunidade.etapasAbertas());

        BigDecimal valorAberto = oportunidadeRepository.somarValorPorEtapas(EtapaOportunidade.etapasAbertas());
        BigDecimal valorGanho = oportunidadeRepository.somarValorPorEtapas(
                List.of(EtapaOportunidade.GANHO));
        BigDecimal valorPerdido = oportunidadeRepository.somarValorPorEtapas(
                List.of(EtapaOportunidade.PERDIDO));

        double taxaConversao = 0.0;
        long encerradas = ganhas + perdidas;
        if (encerradas > 0) {
            taxaConversao = BigDecimal.valueOf(ganhas)
                    .divide(BigDecimal.valueOf(encerradas), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new DashboardResumoResponse(
                clienteRepository.count(),
                oportunidadeRepository.count(),
                abertas,
                ganhas,
                perdidas,
                valorAberto,
                valorGanho,
                valorPerdido,
                taxaConversao
        );
    }

    /**
     * Distribuicao das oportunidades por etapa. Todas as etapas do funil sao
     * devolvidas, mesmo as que nao possuem registros, para que o grafico mantenha
     * a mesma estrutura.
     */
    @Transactional(readOnly = true)
    public List<OportunidadePorEtapaResponse> oportunidadesPorEtapa() {
        Map<EtapaOportunidade, OportunidadeRepository.ProjecaoEtapa> mapa = new EnumMap<>(EtapaOportunidade.class);
        for (OportunidadeRepository.ProjecaoEtapa projecao : oportunidadeRepository.agruparPorEtapa()) {
            mapa.put(projecao.getEtapa(), projecao);
        }

        List<OportunidadePorEtapaResponse> resposta = new ArrayList<>();
        for (EtapaOportunidade etapa : EtapaOportunidade.values()) {
            OportunidadeRepository.ProjecaoEtapa projecao = mapa.get(etapa);
            long quantidade = projecao == null ? 0L : projecao.getTotal();
            BigDecimal valor = projecao == null ? BigDecimal.ZERO : projecao.getValor();
            resposta.add(new OportunidadePorEtapaResponse(etapa, etapa.getDescricao(), quantidade, valor));
        }
        return resposta;
    }

    /**
     * Evolucao mensal das vendas ganhas nos ultimos {@code meses} meses.
     * Meses sem vendas sao devolvidos com valores zerados para manter o grafico continuo.
     */
    @Transactional(readOnly = true)
    public List<VendasPorPeriodoResponse> vendasPorPeriodo(int meses) {
        int quantidadeMeses = Math.max(1, Math.min(meses, 24));
        YearMonth ultimoMes = YearMonth.now();
        YearMonth primeiroMes = ultimoMes.minusMonths(quantidadeMeses - 1L);

        LocalDateTime inicio = primeiroMes.atDay(1).atStartOfDay();
        LocalDateTime fim = ultimoMes.plusMonths(1).atDay(1).atStartOfDay();

        Map<YearMonth, long[]> porMes = new java.util.HashMap<>();
        Map<YearMonth, BigDecimal> valoresPorMes = new java.util.HashMap<>();
        for (YearMonth mes = primeiroMes; !mes.isAfter(ultimoMes); mes = mes.plusMonths(1)) {
            porMes.put(mes, new long[1]);
            valoresPorMes.put(mes, BigDecimal.ZERO);
        }

        List<Oportunidade> ganhas = oportunidadeRepository.listarFechadasNoPeriodo(
                EtapaOportunidade.GANHO, inicio, fim);
        for (Oportunidade oportunidade : ganhas) {
            YearMonth mes = YearMonth.from(oportunidade.getDataFechamento());
            long[] contador = porMes.get(mes);
            if (contador != null) {
                contador[0]++;
                valoresPorMes.put(mes, valoresPorMes.get(mes).add(oportunidade.getValorEstimado()));
            }
        }

        List<VendasPorPeriodoResponse> resposta = new ArrayList<>();
        for (YearMonth mes = primeiroMes; !mes.isAfter(ultimoMes); mes = mes.plusMonths(1)) {
            resposta.add(new VendasPorPeriodoResponse(
                    mes.format(FORMATO_PERIODO),
                    porMes.get(mes)[0],
                    valoresPorMes.get(mes)
            ));
        }
        return resposta;
    }

    private long contarPerdidas() {
        return oportunidadeRepository.countByEtapa(EtapaOportunidade.PERDIDO);
    }
}
