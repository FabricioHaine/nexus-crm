package com.nexus.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Pedido de envio do link de recuperacao de senha.
 */
public record RecuperarSenhaRequest(

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail valido.")
        String email
) {
}
