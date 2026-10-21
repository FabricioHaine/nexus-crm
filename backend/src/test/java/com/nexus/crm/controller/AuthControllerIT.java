package com.nexus.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.support.TesteAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Autenticacao, sessao e perfis de acesso")
class AuthControllerIT extends TesteAutenticado {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararBase() {
        clienteRepository.deleteAll();
        usuarioRepository.findAll().stream()
                .filter(usuario -> !"admin@teste.local".equals(usuario.getEmail()))
                .forEach(usuarioRepository::delete);
    }

    private String corpoLogin(String email, String senha) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "senha", senha));
    }

    private Usuario usuarioComPerfil(Papel papel, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario " + papel);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode("Senha@123"));
        usuario.setPapel(papel);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Autentica com credenciais validas e devolve o token")
    void deveAutenticarComCredenciaisValidas() throws Exception {
        usuarioComPerfil(Papel.GERENTE, "gerente.teste@empresa.com.br");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("gerente.teste@empresa.com.br", "Senha@123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").exists())
                .andExpect(jsonPath("$.usuario.email").value("gerente.teste@empresa.com.br"))
                .andExpect(jsonPath("$.usuario.papel").value("GERENTE"))
                .andExpect(jsonPath("$.usuario.senhaHash").doesNotExist());
    }

    @Test
    @DisplayName("Devolve 401 quando a senha esta incorreta")
    void deveRecusarSenhaIncorreta() throws Exception {
        usuarioComPerfil(Papel.VENDEDOR, "vendedor.teste@empresa.com.br");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("vendedor.teste@empresa.com.br", "senha-errada")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(containsString("E-mail ou senha invalidos")));
    }

    @Test
    @DisplayName("Devolve 401 quando o e-mail nao existe")
    void deveRecusarEmailInexistente() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("ninguem@empresa.com.br", "Senha@123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devolve 401 quando o usuario esta desativado")
    void deveRecusarUsuarioDesativado() throws Exception {
        Usuario usuario = usuarioComPerfil(Papel.VENDEDOR, "desativado.teste@empresa.com.br");
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("desativado.teste@empresa.com.br", "Senha@123")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(containsString("desativado")));
    }

    @Test
    @DisplayName("Cadastra um novo usuario com perfil Vendedor")
    void deveRegistrarNovoUsuario() throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "Maria Souza",
                "email", "maria.nova@empresa.com.br",
                "senha", "Senha@123"));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.usuario.nome").value("Maria Souza"))
                .andExpect(jsonPath("$.usuario.papel").value("VENDEDOR"))
                .andExpect(jsonPath("$.usuario.ativo").value(true));

        assertThat(usuarioRepository.existsByEmailIgnoreCase("maria.nova@empresa.com.br")).isTrue();
    }

    @Test
    @DisplayName("Devolve 409 quando o e-mail do cadastro ja existe")
    void deveRecusarEmailDuplicadoNoCadastro() throws Exception {
        usuarioComPerfil(Papel.VENDEDOR, "repetido@empresa.com.br");

        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "Outra Pessoa",
                "email", "repetido@empresa.com.br",
                "senha", "Senha@123"));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("repetido@empresa.com.br")));
    }

    @Test
    @DisplayName("Devolve 400 quando a senha do cadastro e fraca")
    void deveRecusarSenhaFraca() throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of(
                "nome", "Pessoa Teste",
                "email", "fraco@empresa.com.br",
                "senha", "abc"));

        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    @DisplayName("Devolve 401 ao acessar um endpoint protegido sem token")
    void deveExigirTokenNasRequisicoesProtegidas() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value(containsString("autenticar-se")));
    }

    @Test
    @DisplayName("Devolve 401 quando o token informado e invalido")
    void deveRecusarTokenInvalido() throws Exception {
        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Devolve os dados do usuario autenticado em /auth/me")
    void deveDevolverUsuarioAutenticado() throws Exception {
        Usuario usuario = usuarioComPerfil(Papel.GERENTE, "sessao.teste@empresa.com.br");
        String token = tokenDe(Papel.GERENTE);

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.papel").value("GERENTE"));

        assertThat(usuario.getId()).isNotNull();
    }

    @Test
    @DisplayName("Devolve 403 quando o vendedor tenta excluir um cliente")
    void deveBloquearExclusaoParaVendedor() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Comum");
        cliente.setEmail("cliente.comum@empresa.com.br");
        clienteRepository.save(cliente);

        mockMvc.perform(autenticadoComo(Papel.VENDEDOR, delete("/api/clientes/" + cliente.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Permite ao gerente excluir um cliente")
    void devePermitirExclusaoParaGerente() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente Sem Oportunidades");
        cliente.setEmail("cliente.livre@empresa.com.br");
        clienteRepository.save(cliente);

        mockMvc.perform(autenticadoComo(Papel.GERENTE, delete("/api/clientes/" + cliente.getId())))
                .andExpect(status().isNoContent());

        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
    }

    @Test
    @DisplayName("Restringe a gestao de usuarios ao perfil ADMIN")
    void deveRestringirGestaoDeUsuariosAoAdmin() throws Exception {
        mockMvc.perform(autenticadoComo(Papel.VENDEDOR, get("/api/usuarios")))
                .andExpect(status().isForbidden());

        mockMvc.perform(autenticadoComo(Papel.GERENTE, get("/api/usuarios")))
                .andExpect(status().isForbidden());

        mockMvc.perform(autenticado(get("/api/usuarios")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").isNumber());
    }

    @Test
    @DisplayName("Desativa um usuario e bloqueia o acesso dele")
    void deveDesativarUsuarioEDerrubarOToken() throws Exception {
        Usuario alvo = usuarioComPerfil(Papel.VENDEDOR, "alvo.teste@empresa.com.br");
        String tokenDoAlvo = tokenDo(alvo);

        mockMvc.perform(autenticado(get("/api/clientes")))
                .andExpect(status().isOk());

        mockMvc.perform(autenticado(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/usuarios/" + alvo.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("ativo", false)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));

        // O token continua assinado, mas o usuario desativado perde o acesso.
        mockMvc.perform(get("/api/clientes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDoAlvo))
                .andExpect(status().isUnauthorized());
    }
}
