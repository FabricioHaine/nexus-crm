package com.nexus.crm.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A aplicacao nao pode subir, fora do ambiente local, com os segredos de
 * desenvolvimento.
 */
@DisplayName("Validacao dos segredos de producao")
class ValidacaoDeSegredosTest {

    private static final String CHAVE_PADRAO =
            "nexus-crm-chave-de-desenvolvimento-troque-em-producao-2026";
    private static final String SENHA_PADRAO = "Nexus@2026";

    @Test
    @DisplayName("No perfil dev os valores padrao sao aceitos")
    void deveAceitarPadroesNoPerfilDev() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("dev"),
                CHAVE_PADRAO, SENHA_PADRAO, "admin@nexuscrm.local");

        assertThatCode(() -> validacao.run(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("No perfil test os valores padrao sao aceitos")
    void deveAceitarPadroesNoPerfilTest() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("test"),
                CHAVE_PADRAO, SENHA_PADRAO, "admin@nexuscrm.local");

        assertThatCode(() -> validacao.run(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Fora do ambiente local a chave padrao e recusada")
    void deveRecusarChavePadrao() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("postgres", "prod"),
                CHAVE_PADRAO, "uma-senha-forte-2026", "admin@empresa.com");

        assertThatThrownBy(() -> validacao.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET");
    }

    @Test
    @DisplayName("Fora do ambiente local a senha padrao e recusada")
    void deveRecusarSenhaPadrao() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("postgres", "prod"),
                "uma-chave-aleatoria-de-no-minimo-32-caracteres", SENHA_PADRAO, "admin@empresa.com");

        assertThatThrownBy(() -> validacao.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ADMIN_SENHA");
    }

    @Test
    @DisplayName("Fora do ambiente local o e-mail de desenvolvimento e recusado")
    void deveRecusarEmailDeDesenvolvimento() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("postgres", "prod"),
                "uma-chave-aleatoria-de-no-minimo-32-caracteres", "uma-senha-forte-2026",
                "admin@nexuscrm.local");

        assertThatThrownBy(() -> validacao.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_ADMIN_EMAIL");
    }

    @Test
    @DisplayName("Com segredos proprios a aplicacao sobe normalmente")
    void deveAceitarSegredosProprios() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("postgres", "prod"),
                "uma-chave-aleatoria-de-no-minimo-32-caracteres", "uma-senha-forte-2026",
                "admin@empresa.com");

        assertThatCode(() -> validacao.run(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Todos os problemas sao relatados de uma vez")
    void deveRelatarTodosOsProblemas() {
        ValidacaoDeSegredos validacao = new ValidacaoDeSegredos(ambiente("prod"),
                CHAVE_PADRAO, SENHA_PADRAO, "admin@nexuscrm.local");

        assertThatThrownBy(() -> validacao.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_JWT_SECRET")
                .hasMessageContaining("APP_ADMIN_SENHA")
                .hasMessageContaining("APP_ADMIN_EMAIL");
    }

    private MockEnvironment ambiente(String... perfis) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(perfis);
        return environment;
    }
}
