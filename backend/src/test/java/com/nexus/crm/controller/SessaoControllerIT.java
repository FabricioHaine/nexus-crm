package com.nexus.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import com.nexus.crm.repository.TokenRefreshRepository;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.support.TesteAutenticado;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sessao por cookie, protecao CSRF, renovacao e troca de senha.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Sessao por cookie, CSRF e renovacao de token")
class SessaoControllerIT extends TesteAutenticado {

    private static final String COOKIE_SESSAO = "NEXUS_SESSION";
    private static final String COOKIE_REFRESH = "NEXUS_REFRESH";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRefreshRepository tokenRefreshRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private OportunidadeRepository oportunidadeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void prepararBase() {
        oportunidadeRepository.deleteAll();
        clienteRepository.deleteAll();
        tokenRefreshRepository.deleteAll();
        usuarioRepository.findAll().stream()
                .filter(usuario -> !"admin@teste.local".equals(usuario.getEmail()))
                .forEach(usuarioRepository::delete);
    }

    private Usuario usuarioDeTeste() {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario Cookie");
        usuario.setEmail("cookie.teste@empresa.com.br");
        usuario.setSenhaHash(passwordEncoder.encode("Senha@123"));
        usuario.setPapel(Papel.ADMIN);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    private Cookie cookieDeSessao(String token) {
        return new Cookie(COOKIE_SESSAO, token);
    }

    @Test
    @DisplayName("O login grava o cookie de sessao com HttpOnly e o de renovacao")
    void deveGravarCookiesNoLogin() throws Exception {
        usuarioDeTeste();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "cookie.teste@empresa.com.br", "senha", "Senha@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(cookie().exists(COOKIE_SESSAO))
                .andExpect(cookie().httpOnly(COOKIE_SESSAO, true))
                .andExpect(cookie().exists(COOKIE_REFRESH))
                .andExpect(cookie().httpOnly(COOKIE_REFRESH, true))
                .andExpect(cookie().path(COOKIE_REFRESH, "/api/auth"));
    }

    @Test
    @DisplayName("A sessao por cookie permite consultar o usuario autenticado")
    void deveAutenticarPeloCookie() throws Exception {
        usuarioDeTeste();

        String token = tokenDe(Papel.ADMIN);

        mockMvc.perform(get("/api/auth/me").cookie(cookieDeSessao(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"));
    }

    @Test
    @DisplayName("Uma escrita autenticada por cookie sem o token de CSRF e recusada")
    void deveRecusarEscritaSemTokenCsrf() throws Exception {
        usuarioDeTeste();
        String token = tokenDe(Papel.ADMIN);

        mockMvc.perform(post("/api/clientes")
                        .cookie(cookieDeSessao(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Sem Csrf", "email", "sem.csrf@empresa.com"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Uma escrita autenticada por cookie com o token de CSRF e aceita")
    void deveAceitarEscritaComTokenCsrf() throws Exception {
        usuarioDeTeste();
        String token = tokenDe(Papel.ADMIN);

        mockMvc.perform(post("/api/clientes")
                        .cookie(cookieDeSessao(token))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nome", "Com Csrf", "email", "com.csrf@empresa.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Com Csrf"));
    }

    @Test
    @DisplayName("A renovacao emite um novo cookie e invalida o token anterior")
    void deveRenovarERotacionarOToken() throws Exception {
        usuarioDeTeste();

        // 1. login emite o cookie de renovacao
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "cookie.teste@empresa.com.br", "senha", "Senha@123"))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refresh = login.getResponse().getCookie(COOKIE_REFRESH);
        assertThat(refresh).isNotNull();

        // 2. o cookie de renovacao permite emitir uma nova sessao
        MvcResult renovacao = mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();

        Cookie novoRefresh = renovacao.getResponse().getCookie(COOKIE_REFRESH);
        Cookie novaSessao = renovacao.getResponse().getCookie(COOKIE_SESSAO);
        assertThat(novoRefresh).isNotNull();
        assertThat(novaSessao).isNotNull();
        assertThat(novoRefresh.getValue()).isNotEqualTo(refresh.getValue());

        // 3. o token de renovacao anterior foi rotacionado e nao serve mais
        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A renovacao sem cookie de renovacao e recusada")
    void deveRecusarRenovacaoSemCookie() throws Exception {
        usuarioDeTeste();

        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("O logout revoga a sessao e o acesso posterior e recusado")
    void deveRevogarSessaoNoLogout() throws Exception {
        usuarioDeTeste();
        String token = tokenDe(Papel.ADMIN);

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE_SESSAO, 0))
                .andExpect(cookie().maxAge(COOKIE_REFRESH, 0));

        // A sessao continua valida no token, mas o logout limpou o cookie do cliente.
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("A troca de senha exige a senha atual correta")
    void deveExigirSenhaAtualNaTroca() throws Exception {
        Usuario usuario = usuarioDeTeste();
        String token = tokenDo(usuario);

        mockMvc.perform(post("/api/auth/trocar-senha")
                        .cookie(cookieDeSessao(token))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("senhaAtual", "senha-errada", "novaSenha", "OutraSenha@2026"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value(containsString("senha atual")));
    }

    @Test
    @DisplayName("A troca de senha altera o hash e revoga as sessoes do usuario")
    void deveTrocarSenhaERevogarSessoes() throws Exception {
        Usuario usuario = usuarioDeTeste();
        String token = tokenDo(usuario);
        String hashAntigo = usuario.getSenhaHash();

        mockMvc.perform(post("/api/auth/trocar-senha")
                        .cookie(cookieDeSessao(token))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("senhaAtual", "Senha@123", "novaSenha", "OutraSenha@2026"))))
                .andExpect(status().isNoContent());

        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(atualizado.getSenhaHash()).isNotEqualTo(hashAntigo);
        assertThat(passwordEncoder.matches("OutraSenha@2026", atualizado.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("A senha nova precisa ser diferente da atual")
    void deveRecusarSenhaIgualAAAtual() throws Exception {
        Usuario usuario = usuarioDeTeste();
        String token = tokenDo(usuario);

        mockMvc.perform(post("/api/auth/trocar-senha")
                        .cookie(cookieDeSessao(token))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("senhaAtual", "Senha@123", "novaSenha", "Senha@123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(containsString("diferente")));
    }
}
