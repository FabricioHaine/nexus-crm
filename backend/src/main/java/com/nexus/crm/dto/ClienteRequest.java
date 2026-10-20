package com.nexus.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para criacao e atualizacao de clientes.
 */
public record ClienteRequest(
        @NotBlank(message = "O nome do cliente e obrigatorio")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String nome,

        @NotBlank(message = "O e-mail do cliente e obrigatorio")
        @Email(message = "O e-mail informado e invalido")
        @Size(max = 180, message = "O e-mail deve ter no maximo 180 caracteres")
        String email,

        @Size(max = 30, message = "O telefone deve ter no maximo 30 caracteres")
        String telefone,

        @Size(max = 150, message = "A empresa deve ter no maximo 150 caracteres")
        String empresa,

        @Size(max = 100, message = "O cargo deve ter no maximo 100 caracteres")
        String cargo,

        @Size(max = 1000, message = "As observacoes devem ter no maximo 1000 caracteres")
        String observacoes
) {
}
