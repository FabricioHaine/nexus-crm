package com.nexus.crm.mapper;

import com.nexus.crm.dto.ClienteRequest;
import com.nexus.crm.dto.ClienteResponse;
import com.nexus.crm.model.Cliente;

/**
 * Conversao entre DTOs e entidade de cliente.
 */
public final class ClienteMapper {

    private ClienteMapper() {
    }

    /**
     * Aplica os dados recebidos sobre a entidade. Se {@code destino} for nulo, uma nova
     * entidade e criada.
     */
    public static Cliente toEntity(ClienteRequest request, Cliente destino) {
        Cliente cliente = destino != null ? destino : new Cliente();
        cliente.setNome(request.nome().trim());
        cliente.setEmail(request.email().trim().toLowerCase());
        cliente.setTelefone(textoVazioParaNull(request.telefone()));
        cliente.setEmpresa(textoVazioParaNull(request.empresa()));
        cliente.setCargo(textoVazioParaNull(request.cargo()));
        cliente.setObservacoes(textoVazioParaNull(request.observacoes()));
        return cliente;
    }

    public static ClienteResponse toResponse(Cliente cliente, long totalOportunidades) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getEmpresa(),
                cliente.getCargo(),
                cliente.getObservacoes(),
                cliente.getDataCadastro(),
                cliente.getDataAtualizacao(),
                totalOportunidades
        );
    }

    private static String textoVazioParaNull(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
