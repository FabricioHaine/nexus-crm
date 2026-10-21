package com.nexus.crm.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

/**
 * Resolve o token de acesso em uma requisicao.
 *
 * <p>Usa o cabecalho {@code Authorization: Bearer} quando presente — esse e o
 * caminho dos clientes stateless (curl, Swagger, integracoes) — e, na
 * ausencia dele, recorre ao cookie de sessao, que e o caminho da interface
 * web.</p>
 */
public class BearerTokenCookieResolver implements BearerTokenResolver {

    private final BearerTokenResolver delegate = new DefaultBearerTokenResolver();
    private final String cookieNome;

    public BearerTokenCookieResolver(String cookieNome) {
        this.cookieNome = cookieNome;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        String token = delegate.resolve(request);
        if (token != null) {
            return token;
        }
        return lerCookie(request, cookieNome);
    }

    /** Le um cookie pelo nome, ou null quando ausente. */
    public static String lerCookie(HttpServletRequest request, String nome) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (nome.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
