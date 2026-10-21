package com.nexus.crm.exception;

/**
 * Falha na autenticacao: e-mail inexistente, senha incorreta ou usuario desativado.
 * Resulta em HTTP 401.
 */
public class AutenticacaoFalhouException extends RuntimeException {

    public AutenticacaoFalhouException(String mensagem) {
        super(mensagem);
    }
}
