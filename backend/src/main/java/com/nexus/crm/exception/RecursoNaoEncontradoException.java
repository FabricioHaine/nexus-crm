package com.nexus.crm.exception;

/**
 * Recurso solicitado nao existe no banco de dados.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
