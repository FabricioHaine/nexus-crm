package com.nexus.crm.exception;

/**
 * Dados invalidos enviados pelo cliente. Resultado HTTP 400.
 */
public class ErroDeValidacaoException extends RuntimeException {

    public ErroDeValidacaoException(String mensagem) {
        super(mensagem);
    }
}
