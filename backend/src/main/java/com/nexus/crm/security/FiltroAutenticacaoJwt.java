package com.nexus.crm.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica a requisicao a partir do token JWT.
 *
 * <p>Substitui o filtro de resource server do Spring Security porque este
 * aceita o token tanto no cabecalho {@code Authorization} (clientes
 * stateless) quanto no cookie de sessao (interface web). O controle do token
 * fica explicito aqui, o que permite manter a protecao CSRF ativa para as
 * requisicoes autenticadas por cookie — requisite para impedir que um site
 * externo use a sessao do usuario.</p>
 */
public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private final JwtDecoder jwtDecoder;
    private final ConversorJwtAuthentication conversor;
    private final BearerTokenCookieResolver resolver;
    private final AuthenticationEntryPoint pontoDeEntradaNaoAutenticado;

    public FiltroAutenticacaoJwt(JwtDecoder jwtDecoder,
                                  ConversorJwtAuthentication conversor,
                                  BearerTokenCookieResolver resolver,
                                  AuthenticationEntryPoint pontoDeEntradaNaoAutenticado) {
        this.jwtDecoder = jwtDecoder;
        this.conversor = conversor;
        this.resolver = resolver;
        this.pontoDeEntradaNaoAutenticado = pontoDeEntradaNaoAutenticado;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = resolver.resolve(request);
        if (token == null) {
            // Sem credencial: a cadeia segue e a autorizacao decide o que fazer.
            chain.doFilter(request, response);
            return;
        }

        try {
            Jwt jwt = jwtDecoder.decode(token);
            Authentication autenticacao = conversor.convert(jwt);
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (JwtException ex) {
            SecurityContextHolder.clearContext();
            pontoDeEntradaNaoAutenticado.commence(request, response,
                    new BadCredentialsException("Token invalido ou expirado.", ex));
            return;
        } catch (BadCredentialsException ex) {
            SecurityContextHolder.clearContext();
            pontoDeEntradaNaoAutenticado.commence(request, response, ex);
            return;
        }

        chain.doFilter(request, response);
    }
}
