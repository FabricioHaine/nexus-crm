package com.nexus.crm.service;

import com.nexus.crm.dto.ClienteRequest;
import com.nexus.crm.dto.ClienteResponse;
import com.nexus.crm.dto.PaginaResponse;
import com.nexus.crm.exception.ConflitoDeRegraException;
import com.nexus.crm.exception.RecursoNaoEncontradoException;
import com.nexus.crm.mapper.ClienteMapper;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de negocio do modulo de clientes.
 */
@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final OportunidadeRepository oportunidadeRepository;

    public ClienteService(ClienteRepository clienteRepository, OportunidadeRepository oportunidadeRepository) {
        this.clienteRepository = clienteRepository;
        this.oportunidadeRepository = oportunidadeRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ClienteResponse> listar(String termo, Pageable pageable) {
        String termoNormalizado = (termo == null || termo.isBlank()) ? null : termo.trim();
        Page<Cliente> pagina = clienteRepository.buscar(termoNormalizado, pageable);
        return PaginaResponse.de(pagina.map(this::paraResponse));
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return paraResponse(obrigatorio(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        String email = request.email().trim().toLowerCase();
        if (clienteRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoDeRegraException("Ja existe um cliente cadastrado com o e-mail " + email + ".");
        }
        Cliente cliente = ClienteMapper.toEntity(request, null);
        return paraResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = obrigatorio(id);
        String email = request.email().trim().toLowerCase();
        clienteRepository.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ConflitoDeRegraException("Ja existe um cliente cadastrado com o e-mail " + email + ".");
                });

        ClienteMapper.toEntity(request, cliente);
        return paraResponse(clienteRepository.save(cliente));
    }

    /**
     * Remove o cliente. Clientes com oportunidades vinculadas nao podem ser excluidos,
     * para nao perder o historico comercial (HTTP 409).
     */
    @Transactional
    public void excluir(Long id) {
        Cliente cliente = obrigatorio(id);
        long total = oportunidadeRepository.countByClienteId(id);
        if (total > 0) {
            throw new ConflitoDeRegraException(
                    "O cliente " + cliente.getNome() + " possui " + total
                            + " oportunidade(s) vinculada(s). Exclua ou transfira as oportunidades antes de remove-lo.");
        }
        clienteRepository.delete(cliente);
    }

    private Cliente obrigatorio(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado com o id " + id + "."));
    }

    private ClienteResponse paraResponse(Cliente cliente) {
        long total = cliente.getId() == null ? 0 : oportunidadeRepository.countByClienteId(cliente.getId());
        return ClienteMapper.toResponse(cliente, total);
    }
}
