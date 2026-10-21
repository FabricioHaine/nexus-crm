package com.nexus.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.crm.dto.ClienteRequest;
import com.nexus.crm.model.Cliente;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Endpoints REST de clientes")
class ClienteControllerIT extends TesteAutenticado {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @BeforeEach
    void limparBase() {
        oportunidadeRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    private ClienteRequest request(String nome, String email) {
        return new ClienteRequest(nome, email, "(11) 90000-0000", "Empresa Teste", "Analista", null);
    }

    @Test
    @DisplayName("Cadastra um cliente e devolve 201 com o id gerado")
    void deveCadastrarCliente() throws Exception {
        String corpo = objectMapper.writeValueAsString(request("Maria Souza", "maria@empresa.com.br"));

        mockMvc.perform(autenticado(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.email").value("maria@empresa.com.br"))
                .andExpect(jsonPath("$.dataCadastro").exists());

        assertThat(clienteRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Devolve 400 quando o e-mail informado e invalido")
    void deveRecusarEmailInvalido() throws Exception {
        String corpo = objectMapper.writeValueAsString(request("Maria Souza", "email-invalido"));

        mockMvc.perform(autenticado(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos.email").exists());
    }

    @Test
    @DisplayName("Devolve 400 quando campos obrigatorios ficam em branco")
    void deveRecusarNomeVazio() throws Exception {
        String corpo = objectMapper.writeValueAsString(request("  ", "maria@empresa.com.br"));

        mockMvc.perform(autenticado(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists());
    }

    @Test
    @DisplayName("Devolve 409 quando o e-mail ja esta cadastrado")
    void deveRecusarEmailDuplicado() throws Exception {
        Cliente existente = new Cliente();
        existente.setNome("Outra Pessoa");
        existente.setEmail("maria@empresa.com.br");
        clienteRepository.save(existente);

        String corpo = objectMapper.writeValueAsString(request("Maria Souza", "maria@empresa.com.br"));

        mockMvc.perform(autenticado(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("maria@empresa.com.br")));
    }

    @Test
    @DisplayName("Devolve 404 ao consultar cliente inexistente")
    void deveDevolver404ParaClienteInexistente() throws Exception {
        mockMvc.perform(autenticado(get("/api/clientes/999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Lista clientes com paginacao e busca por nome")
    void deveListarClientesComBusca() throws Exception {
        clienteRepository.save(cliente("Ana Lima", "ana@empresa.com.br"));
        clienteRepository.save(cliente("Bruno Dias", "bruno@empresa.com.br"));

        mockMvc.perform(autenticado(get("/api/clientes").param("termo", "bruno")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].nome").value("Bruno Dias"))
                .andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    @DisplayName("Atualiza os dados de um cliente")
    void deveAtualizarCliente() throws Exception {
        Cliente existente = new Cliente();
        existente.setNome("Nome Antigo");
        existente.setEmail("antigo@empresa.com.br");
        clienteRepository.save(existente);

        String corpo = objectMapper.writeValueAsString(request("Nome Novo", "novo@empresa.com.br"));

        mockMvc.perform(autenticado(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/clientes/" + existente.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    @Test
    @DisplayName("Exclui um cliente sem oportunidades e devolve 204")
    void deveExcluirCliente() throws Exception {
        Cliente existente = new Cliente();
        existente.setNome("Cliente Sem Oportunidades");
        existente.setEmail("sem.oportunidades@empresa.com.br");
        clienteRepository.save(existente);

        mockMvc.perform(autenticado(delete("/api/clientes/" + existente.getId())))
                .andExpect(status().isNoContent());

        assertThat(clienteRepository.existsById(existente.getId())).isFalse();
    }

    private Cliente cliente(String nome, String email) {
        Cliente cliente = new Cliente();
        cliente.setNome(nome);
        cliente.setEmail(email);
        return cliente;
    }
}
