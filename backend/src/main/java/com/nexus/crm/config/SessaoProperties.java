package com.nexus.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracao da sessao baseada em cookie.
 *
 * <p>Os valores sao lidos do prefixo {@code app.session} e podem ser
 * sobrescritos por variaveis de ambiente.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.session")
public class SessaoProperties {

    /** Nome do cookie que carrega o token de acesso. */
    private String cookieNome = "NEXUS_SESSION";

    /** Nome do cookie que carrega o token de renovacao. */
    private String refreshCookieNome = "NEXUS_REFRESH";

    /** Marca os cookies como {@code Secure}. Ligue em producao (exige HTTPS). */
    private boolean cookieSecure = false;

    /**
     * Politica {@code SameSite} dos cookies. {@code Strict} exige que a API e o
     * front-end fiquem no mesmo site (mesmo dominio, portas diferentes ok).
     */
    private String cookieSameSite = "Strict";

    /** Validade do token de renovacao, em dias. */
    private int refreshExpiracaoDias = 7;

    /** Caminho do cookie de renovacao: restrito as rotas de autenticacao. */
    private String refreshCookieCaminho = "/api/auth";

    public String getCookieNome() {
        return cookieNome;
    }

    public void setCookieNome(String cookieNome) {
        this.cookieNome = cookieNome;
    }

    public String getRefreshCookieNome() {
        return refreshCookieNome;
    }

    public void setRefreshCookieNome(String refreshCookieNome) {
        this.refreshCookieNome = refreshCookieNome;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public String getCookieSameSite() {
        return cookieSameSite;
    }

    public void setCookieSameSite(String cookieSameSite) {
        this.cookieSameSite = cookieSameSite;
    }

    public int getRefreshExpiracaoDias() {
        return refreshExpiracaoDias;
    }

    public void setRefreshExpiracaoDias(int refreshExpiracaoDias) {
        this.refreshExpiracaoDias = refreshExpiracaoDias;
    }

    public String getRefreshCookieCaminho() {
        return refreshCookieCaminho;
    }

    public void setRefreshCookieCaminho(String refreshCookieCaminho) {
        this.refreshCookieCaminho = refreshCookieCaminho;
    }
}
