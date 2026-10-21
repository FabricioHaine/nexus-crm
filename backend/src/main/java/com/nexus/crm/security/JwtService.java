package com.nexus.crm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Emissao dos tokens JWT de acesso.
 *
 * <p>O token e assinado com HMAC-SHA256 e carrega o e-mail do usuario no
 * {@code sub}, o identificador em {@code uid} e os perfis em {@code papeis}.</p>
 */
@Service
public class JwtService {

    private static final String EMISSOR = "nexus-crm";

    private final JwtEncoder jwtEncoder;
    private final Duration expiracao;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiracao-minutos:480}") long expiracaoMinutos) {
        this.jwtEncoder = jwtEncoder;
        this.expiracao = Duration.ofMinutes(expiracaoMinutos);
    }

    public String gerarToken(UsuarioPrincipal usuario) {
        Instant agora = Instant.now();
        Instant expiracaoEm = agora.plus(expiracao);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISSOR)
                .issuedAt(agora)
                .expiresAt(expiracaoEm)
                .subject(usuario.getUsername())
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("papeis", List.of(usuario.getPapel().name()))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public Instant getExpiracao() {
        return Instant.now().plus(expiracao);
    }

    public long getExpiracaoMinutos() {
        return expiracao.toMinutes();
    }
}
