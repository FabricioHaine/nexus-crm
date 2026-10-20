package com.nexus.crm.exception;

/**
 * Conflito com uma regra de negocio (e-mail duplicado, cliente com oportunidades,
 * transicao de etapa invalida). Resultado HTTP 409.
 */
public class ConflitoDeRegraException extends RuntimeException {

    public ConflitoDeRegraException(String mensagem) {
        super(mensagem);
    }
}
