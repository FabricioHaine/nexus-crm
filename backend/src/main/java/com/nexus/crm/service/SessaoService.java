package com.nexus.crm.service;

import com.nexus.crm.config.SessaoProperties;
import com.nexus.crm.model.TokenRefresh;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.TokenRefreshRepository;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.security.BearerTokenCookieResolver;
import com.nexus.crm.security.JwtService;
import com.nexus.crm.security.UsuarioPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Emissao e encerramento da sessao baseada em cookie.
 *
 * <p>O token de acesso vai para um cookie {@code HttpOnly}, invisivel ao
 * JavaScript: um eventual XSS nao consegue ler a credencial. O token de
 * renovacao e um valor opaco, guardado apenas como hash, e rotacionado a cada
 * uso.</p>
 */
@Service
public class SessaoService {

    private final UsuarioRepository usuarioRepository;
    private final TokenRefreshRepository tokenRefreshRepository;
    private final JwtService jwtService;
    private final SessaoProperties propriedades;
    private final SecureRandom random = new SecureRandom();

    public SessaoService(UsuarioRepository usuarioRepository,
                         TokenRefreshRepository tokenRefreshRepository,
                         JwtService jwtService,
                         SessaoProperties propriedades) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRefreshRepository = tokenRefreshRepository;
        this.jwtService = jwtService;
        this.propriedades = propriedades;
    }

    /**
     * Emite uma nova sessao: grava o cookie de acesso e o de renovacao.
     */
    @Transactional
    public void emitir(Usuario usuario, HttpServletResponse response) {
        String refreshToken = gerarTokenOpaque();

        TokenRefresh registro = new TokenRefresh();
        registro.setUsuarioId(usuario.getId());
        registro.setTokenHash(hash(refreshToken));
        registro.setExpiraEm(LocalDateTime.now().plusDays(propriedades.getRefreshExpiracaoDias()));
        registro.setRevogado(false);
        tokenRefreshRepository.save(registro);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieDeAcesso(usuario).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieDeRenovacao(refreshToken).toString());
    }

    /**
     * Renova a sessao a partir do cookie de renovacao, rotacionando o token.
     *
     * @return o usuario da sessao, ou null quando nao ha renovacao valida
     */
    @Transactional
    public Usuario renovar(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieDeRequest(request, propriedades.getRefreshCookieNome());
        if (token == null) {
            return null;
        }

        TokenRefresh registro = tokenRefreshRepository.findByTokenHash(hash(token)).orElse(null);
        if (registro == null || registro.isRevogado()
                || registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            return null;
        }

        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId()).orElse(null);
        if (usuario == null || !usuario.isAtivo()) {
            return null;
        }

        // Rotacao: o token usado e invalidado e um novo e emitido.
        registro.setRevogado(true);
        tokenRefreshRepository.save(registro);

        emitir(usuario, response);
        return usuario;
    }

    /**
     * Encerra a sessao: revoga o token de renovacao e limpa os cookies.
     */
    @Transactional
    public void encerrar(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieDeRequest(request, propriedades.getRefreshCookieNome());
        if (token != null) {
            tokenRefreshRepository.findByTokenHash(hash(token)).ifPresent(registro -> {
                registro.setRevogado(true);
                tokenRefreshRepository.save(registro);
            });
        }
        limpar(response);
    }

    /**
     * Revoga todos os tokens de renovacao de um usuario. Usado na troca de
     * senha, para que as outras sessoes sejam invalidadas.
     */
    @Transactional
    public void revogarSessoesDoUsuario(Long usuarioId) {
        tokenRefreshRepository.revogarTodosDoUsuario(usuarioId);
    }

    /** Remove os tokens expirados, evitando crescimento sem limite da tabela. */
    @Transactional
    public void limparExpirados() {
        tokenRefreshRepository.removerExpirados(LocalDateTime.now().minusDays(1));
    }

    private ResponseCookie cookieDeAcesso(Usuario usuario) {
        String token = jwtService.gerarToken(UsuarioPrincipal.de(usuario));
        return base(token, propriedades.getCookieNome(), "/", propriedades.getRefreshExpiracaoDias() * 24L * 60L);
    }

    private ResponseCookie cookieDeRenovacao(String token) {
        return base(token, propriedades.getRefreshCookieNome(), propriedades.getRefreshCookieCaminho(),
                propriedades.getRefreshExpiracaoDias() * 24L * 60L);
    }

    private ResponseCookie base(String valor, String nome, String caminho, long maxAgeSegundos) {
        return ResponseCookie.from(nome, valor)
                .httpOnly(true)
                .secure(propriedades.isCookieSecure())
                .sameSite(propriedades.getCookieSameSite())
                .path(caminho)
                .maxAge(java.time.Duration.ofSeconds(maxAgeSegundos))
                .build();
    }

    /** Limpa os dois cookies, com tempo de vida zero. */
    private void limpar(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                base("", propriedades.getCookieNome(), "/", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE,
                base("", propriedades.getRefreshCookieNome(), propriedades.getRefreshCookieCaminho(), 0).toString());
    }

    private String cookieDeRequest(HttpServletRequest request, String nome) {
        return BearerTokenCookieResolver.lerCookie(request, nome);
    }

    /** Gera um valor opaco de 256 bits, seguro para uso criptografico. */
    private String gerarTokenOpaque() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Calcula o hash SHA-256 do token, em hexadecimal. */
    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao disponivel nesta plataforma.", ex);
        }
    }
}
