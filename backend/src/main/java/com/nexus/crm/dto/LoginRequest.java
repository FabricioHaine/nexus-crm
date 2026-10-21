package com.nexus.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Credenciais enviadas no login.
 */
public record LoginRequest(

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail valido.")
        String email,

        @NotBlank(message = "Informe a senha.")
        String senha
) {
}
