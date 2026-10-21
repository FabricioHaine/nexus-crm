package com.nexus.crm.service;

import com.nexus.crm.model.TokenRecuperacao;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.TokenRecuperacaoRepository;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Recuperacao de senha por token de uso unico.
 *
 * <p>O fluxo nao revela se o e-mail existe: a resposta e sempre a mesma. O
 * token e um valor opaco de 256 bits, valido por uma hora, guardado apenas
 * como hash. Ao redefinir a senha, todas as sessoes do usuario sao revogadas.</p>
 */
@Service
public class RecuperacaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacaoSenhaService.class);

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacaoRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificacaoService notificacaoService;
    private final SessaoService sessaoService;
    private final SecureRandom random = new SecureRandom();
    private final int validadeMinutos;
    private final String urlFrontEnd;

    public RecuperacaoSenhaService(UsuarioRepository usuarioRepository,
                                   TokenRecuperacaoRepository tokenRepository,
                                   PasswordEncoder passwordEncoder,
                                   NotificacaoService notificacaoService,
                                   SessaoService sessaoService,
                                   @Value("${app.recuperacao.validade-minutos:60}") int validadeMinutos,
                                   @Value("${app.recuperacao.url-front-end:http://localhost:4200}") String urlFrontEnd) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificacaoService = notificacaoService;
        this.sessaoService = sessaoService;
        this.validadeMinutos = validadeMinutos;
        this.urlFrontEnd = urlFrontEnd;
    }

    /**
     * Envia o link de recuperacao, se o e-mail existir.
     *
     * <p>Nunca revela se o e-mail esta cadastrado: a resposta e a mesma nos
     * dois casos, para nao permitir enumeracao de usuarios.</p>
     */
    @Transactional
    public void solicitarRecuperacao(String email) {
        String normalizado = email.trim().toLowerCase();

        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(normalizado)
                .filter(Usuario::isAtivo);

        if (usuario.isEmpty()) {
            log.info("Pedido de recuperacao para um e-mail sem usuario ativo: {}", normalizado);
            return;
        }

        Usuario alvo = usuario.get();
        tokenRepository.invalidarAnteriores(alvo.getId());

        String token = gerarToken();

        TokenRecuperacao registro = new TokenRecuperacao();
        registro.setUsuarioId(alvo.getId());
        registro.setTokenHash(hash(token));
        registro.setExpiraEm(LocalDateTime.now().plusMinutes(validadeMinutos));
        registro.setUsado(false);
        tokenRepository.save(registro);

        String link = urlFrontEnd + "/redefinir-senha?token=" + token;
        notificacaoService.enviarLinkDeRecuperacao(alvo.getEmail(), alvo.getNome(), link);
    }

    /**
     * Redefine a senha com o token recebido.
     */
    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        if (token == null || token.isBlank()) {
            throw new com.nexus.crm.exception.ErroDeValidacaoException("Informe o token de recuperacao.");
        }

        TokenRecuperacao registro = tokenRepository.findByTokenHash(hash(token.trim()))
                .orElseThrow(() -> new com.nexus.crm.exception.ErroDeValidacaoException(
                        "Token de recuperacao invalido ou ja utilizado."));

        if (registro.isUsado()) {
            throw new com.nexus.crm.exception.ErroDeValidacaoException(
                    "Token de recuperacao invalido ou ja utilizado.");
        }
        if (registro.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new com.nexus.crm.exception.ErroDeValidacaoException(
                    "Token de recuperacao expirado. Solicite um novo link.");
        }

        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId())
                .orElseThrow(() -> new com.nexus.crm.exception.ErroDeValidacaoException(
                        "Token de recuperacao invalido."));

        registro.setUsado(true);
        tokenRepository.save(registro);

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        // As sessoes existentes deixam de valer com a troca de senha.
        sessaoService.revogarSessoesDoUsuario(usuario.getId());
    }

    private String gerarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 nao disponivel nesta plataforma.", ex);
        }
    }
}
