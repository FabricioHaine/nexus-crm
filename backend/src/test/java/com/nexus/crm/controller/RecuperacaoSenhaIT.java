package com.nexus.crm.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import com.nexus.crm.repository.TokenRecuperacaoRepository;
import com.nexus.crm.repository.TokenRefreshRepository;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.support.TesteAutenticado;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recuperacao de senha por token de uso unico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Recuperacao de senha")
class RecuperacaoSenhaIT extends TesteAutenticado {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacaoRepository tokenRecuperacaoRepository;

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
        tokenRecuperacaoRepository.deleteAll();
        tokenRefreshRepository.deleteAll();
        usuarioRepository.findAll().stream()
                .filter(usuario -> !"admin@teste.local".equals(usuario.getEmail()))
                .forEach(usuarioRepository::delete);
    }

    private Usuario usuarioDeTeste() {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario Recuperacao");
        usuario.setEmail("recuperacao.teste@empresa.com.br");
        usuario.setSenhaHash(passwordEncoder.encode("Senha@123"));
        usuario.setPapel(Papel.VENDEDOR);
        usuario.setAtivo(true);
        return usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("O pedido de recuperacao responde 202 mesmo para e-mail inexistente")
    void deveNaoRevelarSeOEmailExiste() throws Exception {
        mockMvc.perform(post("/api/auth/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "ninguem@empresa.com"))))
                .andExpect(status().isAccepted());

        assertThat(tokenRecuperacaoRepository.count()).isZero();
    }

    @Test
    @DisplayName("O pedido cria um token para o usuario ativo")
    void deveCriarTokenParaUsuarioAtivo() throws Exception {
        Usuario usuario = usuarioDeTeste();

        mockMvc.perform(post("/api/auth/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", usuario.getEmail()))))
                .andExpect(status().isAccepted());

        assertThat(tokenRecuperacaoRepository.count()).isEqualTo(1);
        assertThat(tokenRecuperacaoRepository.findAll().get(0).getUsuarioId()).isEqualTo(usuario.getId());
    }

    @Test
    @DisplayName("O token invalido e recusado")
    void deveRecusarTokenInvalido() throws Exception {
        usuarioDeTeste();

        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "token-que-nao-existe", "novaSenha", "OutraSenha@2026"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(containsString("invalido")));
    }

    @Test
    @DisplayName("A redefinicao troca a senha do usuario")
    void deveRedefinirASenha() throws Exception {
        Usuario usuario = usuarioDeTeste();
        String hashAntigo = usuario.getSenhaHash();

        // Sem SMTP, o token em claro vai para o log; aqui usamos o hash gravado
        // para simular a chegada do link por e-mail.
        mockMvc.perform(post("/api/auth/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", usuario.getEmail()))))
                .andExpect(status().isAccepted());

        // O token em claro nao e persistido, entao a redefinicao so acontece
        // com o valor real, enviado por e-mail. Verificamos que a senha muda
        // simulando a chegada do link.
        String tokenEmClaro = "token-de-teste";
        trocarHashDosTokens(tokenEmClaro);
        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", tokenEmClaro, "novaSenha", "OutraSenha@2026"))))
                .andExpect(status().isNoContent());

        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(atualizado.getSenhaHash()).isNotEqualTo(hashAntigo);
        assertThat(passwordEncoder.matches("OutraSenha@2026", atualizado.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("O token usado nao pode ser reaproveitado")
    void deveRecusarTokenReutilizado() throws Exception {
        Usuario usuario = usuarioDeTeste();
        String token = "token-de-teste";

        // Primeiro o pedido cria o registro do token.
        mockMvc.perform(post("/api/auth/recuperar-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", usuario.getEmail()))))
                .andExpect(status().isAccepted());

        trocarHashDosTokens(token);

        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "novaSenha", "OutraSenha@2026"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/redefinir-senha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token, "novaSenha", "TerceiraSenha@2026"))))
                .andExpect(status().isBadRequest());
    }

    /**
     * Substitui o hash gravado pelo hash de um token conhecido, para simular
     * a chegada do link por e-mail.
     */
    private void trocarHashDosTokens(String tokenEmClaro) {
        var registros = tokenRecuperacaoRepository.findAll();
        registros.forEach(registro -> registro.setTokenHash(hashDe(tokenEmClaro)));
        tokenRecuperacaoRepository.saveAll(registros);
    }

    private String hashDe(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of()
                    .formatHex(digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
