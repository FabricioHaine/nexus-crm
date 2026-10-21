package com.nexus.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Endpoints REST de oportunidades")
class OportunidadeControllerIT extends TesteAutenticado {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    private Cliente cliente;

    @BeforeEach
    void prepararBase() {
        oportunidadeRepository.deleteAll();
        clienteRepository.deleteAll();

        cliente = new Cliente();
        cliente.setNome("Cliente Integracao");
        cliente.setEmail("integracao@empresa.com.br");
        clienteRepository.save(cliente);
    }

    private Map<String, Object> corpoOportunidade(Double valor, String etapa, Long clienteId) {
        return Map.of(
                "titulo", "Implantacao do CRM",
                "descricao", "Projeto de implantacao",
                "clienteId", clienteId,
                "valorEstimado", valor,
                "etapa", etapa,
                "probabilidade", 10,
                "dataPrevistaFechamento", LocalDate.now().plusDays(30).toString()
        );
    }

    @Test
    @DisplayName("Cadastra uma oportunidade e devolve 201")
    void deveCadastrarOportunidade() throws Exception {
        String corpo = objectMapper.writeValueAsString(corpoOportunidade(25000.0, "NOVO_CONTATO", cliente.getId()));

        mockMvc.perform(autenticado(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId()))
                .andExpect(jsonPath("$.etapa").value("NOVO_CONTATO"))
                .andExpect(jsonPath("$.valorEstimado").value(25000.00));

        assertThat(oportunidadeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Devolve 400 quando o valor estimado e zero")
    void deveRecusarValorZero() throws Exception {
        String corpo = objectMapper.writeValueAsString(corpoOportunidade(0.0, "NOVO_CONTATO", cliente.getId()));

        mockMvc.perform(autenticado(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.valorEstimado").exists());
    }

    @Test
    @DisplayName("Devolve 404 quando o cliente informado nao existe")
    void deveRecusarClienteInexistente() throws Exception {
        String corpo = objectMapper.writeValueAsString(corpoOportunidade(1000.0, "NOVO_CONTATO", 999L));

        mockMvc.perform(autenticado(post("/api/oportunidades").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value(containsString("999")));
    }

    @Test
    @DisplayName("Move a oportunidade de etapa com sucesso")
    void deveAlterarEtapa() throws Exception {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setTitulo("Projeto em qualificacao");
        oportunidade.setCliente(cliente);
        oportunidade.setValorEstimado(BigDecimal.valueOf(5000));
        oportunidade.setEtapa(EtapaOportunidade.QUALIFICACAO);
        oportunidade.setProbabilidade(35);
        oportunidadeRepository.save(oportunidade);

        mockMvc.perform(autenticado(patch("/api/oportunidades/" + oportunidade.getId() + "/etapa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("etapa", "PROPOSTA")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapa").value("PROPOSTA"));
    }

    @Test
    @DisplayName("Devolve 409 em transicao invalida de etapa")
    void deveRecusarTransicaoInvalida() throws Exception {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setTitulo("Projeto novo");
        oportunidade.setCliente(cliente);
        oportunidade.setValorEstimado(BigDecimal.valueOf(5000));
        oportunidade.setEtapa(EtapaOportunidade.NOVO_CONTATO);
        oportunidade.setProbabilidade(10);
        oportunidadeRepository.save(oportunidade);

        mockMvc.perform(autenticado(patch("/api/oportunidades/" + oportunidade.getId() + "/etapa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("etapa", "GANHO")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("Transicao invalida")));
    }

    @Test
    @DisplayName("Filtra as oportunidades por etapa e por cliente")
    void deveFiltrarOportunidades() throws Exception {
        Oportunidade ganha = new Oportunidade();
        ganha.setTitulo("Negociacao vencida");
        ganha.setCliente(cliente);
        ganha.setValorEstimado(BigDecimal.valueOf(9000));
        ganha.setEtapa(EtapaOportunidade.GANHO);
        ganha.setProbabilidade(100);
        oportunidadeRepository.save(ganha);

        mockMvc.perform(autenticado(get("/api/oportunidades").param("etapa", "GANHO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].etapa").value("GANHO"));

        mockMvc.perform(autenticado(get("/api/oportunidades").param("clienteId", String.valueOf(cliente.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("Exclui uma oportunidade e devolve 204")
    void deveExcluirOportunidade() throws Exception {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setTitulo("Projeto cancelado");
        oportunidade.setCliente(cliente);
        oportunidade.setValorEstimado(BigDecimal.valueOf(100));
        oportunidade.setEtapa(EtapaOportunidade.NOVO_CONTATO);
        oportunidade.setProbabilidade(10);
        oportunidadeRepository.save(oportunidade);

        mockMvc.perform(autenticado(delete("/api/oportunidades/" + oportunidade.getId())))
                .andExpect(status().isNoContent());

        assertThat(oportunidadeRepository.count()).isZero();
    }
}
