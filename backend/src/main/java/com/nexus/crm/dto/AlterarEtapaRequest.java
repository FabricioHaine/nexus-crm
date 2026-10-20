package com.nexus.crm.dto;

import com.nexus.crm.model.EtapaOportunidade;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados de entrada para mudanca de etapa de uma oportunidade.
 */
public record AlterarEtapaRequest(
        @NotNull(message = "A nova etapa e obrigatoria")
        EtapaOportunidade etapa,

        @Size(max = 500, message = "O motivo da perda deve ter no maximo 500 caracteres")
        String motivoPerda
) {
}
