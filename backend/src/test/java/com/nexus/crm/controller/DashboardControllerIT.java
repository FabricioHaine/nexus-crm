package com.nexus.crm.controller;

import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import com.nexus.crm.support.TesteAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Endpoints de indicadores do dashboard")
class DashboardControllerIT extends TesteAutenticado {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @BeforeEach
    void prepararBase() {
        oportunidadeRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    @DisplayName("Devolve o resumo com os indicadores reais")
    void deveDevolverResumo() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Indicador");
        cliente.setEmail("indicador@empresa.com.br");
        clienteRepository.save(cliente);

        Oportunidade ganho = new Oportunidade();
        ganho.setTitulo("Venda concluida");
        ganho.setCliente(cliente);
        ganho.setValorEstimado(BigDecimal.valueOf(10000));
        ganho.setEtapa(EtapaOportunidade.GANHO);
        ganho.setProbabilidade(100);
        oportunidadeRepository.save(ganho);

        mockMvc.perform(autenticado(get("/api/dashboard/resumo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClientes").value(1))
                .andExpect(jsonPath("$.totalOportunidades").value(1))
                .andExpect(jsonPath("$.vendasGanhas").value(1))
                .andExpect(jsonPath("$.vendasPerdidas").value(0))
                .andExpect(jsonPath("$.taxaConversao").value(100.0))
                .andExpect(jsonPath("$.valorTotalGanho").value(10000.00));
    }

    @Test
    @DisplayName("Devolve a distribuicao por etapa do funil")
    void deveDevolverOportunidadesPorEtapa() throws Exception {
        mockMvc.perform(autenticado(get("/api/dashboard/oportunidades-por-etapa")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].etapa").value("NOVO_CONTATO"))
                .andExpect(jsonPath("$[0].quantidade").value(0));
    }

    @Test
    @DisplayName("Devolve a evolucao de vendas por periodo")
    void deveDevolverVendasPorPeriodo() throws Exception {
        mockMvc.perform(autenticado(get("/api/dashboard/vendas-por-periodo").param("meses", "6")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].periodo").isString())
                .andExpect(jsonPath("$[0].quantidade").value(0));
    }
}
