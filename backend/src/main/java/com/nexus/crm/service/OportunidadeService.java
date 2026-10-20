package com.nexus.crm.service;

import com.nexus.crm.dto.AlterarEtapaRequest;
import com.nexus.crm.dto.OportunidadeRequest;
import com.nexus.crm.dto.OportunidadeResponse;
import com.nexus.crm.dto.PaginaResponse;
import com.nexus.crm.exception.ConflitoDeRegraException;
import com.nexus.crm.exception.ErroDeValidacaoException;
import com.nexus.crm.exception.RecursoNaoEncontradoException;
import com.nexus.crm.mapper.OportunidadeMapper;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regras de negocio do modulo de oportunidades comerciais.
 */
@Service
public class OportunidadeService {

    private final OportunidadeRepository oportunidadeRepository;
    private final ClienteRepository clienteRepository;

    public OportunidadeService(OportunidadeRepository oportunidadeRepository, ClienteRepository clienteRepository) {
        this.oportunidadeRepository = oportunidadeRepository;
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<OportunidadeResponse> listar(Long clienteId, EtapaOportunidade etapa,
                                                       LocalDateTime dataInicial, LocalDateTime dataFinal,
                                                       Pageable pageable) {
        Page<Oportunidade> pagina = oportunidadeRepository.buscarComFiltros(
                clienteId, etapa, dataInicial, dataFinal, pageable);
        return PaginaResponse.de(pagina.map(OportunidadeMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public OportunidadeResponse buscarPorId(Long id) {
        return OportunidadeMapper.toResponse(obrigatorio(id));
    }

    @Transactional(readOnly = true)
    public List<OportunidadeResponse> listarPorCliente(Long clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente nao encontrado com o id " + clienteId + ".");
        }
        return oportunidadeRepository.listarPorCliente(clienteId).stream()
                .map(OportunidadeMapper::toResponse)
                .toList();
    }

    @Transactional
    public OportunidadeResponse criar(OportunidadeRequest request) {
        Cliente cliente = clienteObrigatorio(request.clienteId());
        if (request.etapa() == EtapaOportunidade.PERDIDO) {
            validarMotivoPerda(request.motivoPerda());
        }

        Oportunidade oportunidade = OportunidadeMapper.toEntity(request, cliente, null);
        registrarFechamento(oportunidade, request.etapa());
        return OportunidadeMapper.toResponse(oportunidadeRepository.save(oportunidade));
    }

    @Transactional
    public OportunidadeResponse atualizar(Long id, OportunidadeRequest request) {
        Oportunidade oportunidade = obrigatorio(id);
        Cliente cliente = clienteObrigatorio(request.clienteId());

        EtapaOportunidade etapaAtual = oportunidade.getEtapa();
        EtapaOportunidade novaEtapa = request.etapa();
        if (!etapaAtual.permiteTransicaoPara(novaEtapa)) {
            throw new ConflitoDeRegraException(
                    "Transicao invalida: nao e possivel mover a oportunidade de " + etapaAtual.getDescricao()
                            + " para " + novaEtapa.getDescricao() + ".");
        }
        if (novaEtapa == EtapaOportunidade.PERDIDO) {
            validarMotivoPerda(request.motivoPerda());
        }

        OportunidadeMapper.toEntity(request, cliente, oportunidade);
        if (novaEtapa == EtapaOportunidade.PERDIDO) {
            oportunidade.setMotivoPerda(request.motivoPerda().trim());
        }
        registrarFechamento(oportunidade, novaEtapa);
        return OportunidadeMapper.toResponse(oportunidadeRepository.save(oportunidade));
    }

    /**
     * Altera apenas a etapa da oportunidade, respeitando o fluxo do funil.
     */
    @Transactional
    public OportunidadeResponse alterarEtapa(Long id, AlterarEtapaRequest request) {
        Oportunidade oportunidade = obrigatorio(id);
        EtapaOportunidade etapaAtual = oportunidade.getEtapa();
        EtapaOportunidade novaEtapa = request.etapa();

        if (!etapaAtual.permiteTransicaoPara(novaEtapa)) {
            throw new ConflitoDeRegraException(
                    "Transicao invalida: nao e possivel mover a oportunidade de " + etapaAtual.getDescricao()
                            + " para " + novaEtapa.getDescricao() + ".");
        }
        if (novaEtapa == EtapaOportunidade.PERDIDO) {
            validarMotivoPerda(request.motivoPerda());
        }

        OportunidadeMapper.aplicarEtapa(oportunidade, novaEtapa, request.motivoPerda());
        registrarFechamento(oportunidade, novaEtapa);
        return OportunidadeMapper.toResponse(oportunidadeRepository.save(oportunidade));
    }

    @Transactional
    public void excluir(Long id) {
        Oportunidade oportunidade = obrigatorio(id);
        oportunidadeRepository.delete(oportunidade);
    }

    private void registrarFechamento(Oportunidade oportunidade, EtapaOportunidade etapa) {
        if (etapa == EtapaOportunidade.GANHO || etapa == EtapaOportunidade.PERDIDO) {
            if (oportunidade.getDataFechamento() == null) {
                oportunidade.setDataFechamento(LocalDateTime.now());
            }
        } else {
            oportunidade.setDataFechamento(null);
            oportunidade.setMotivoPerda(null);
        }
    }

    private void validarMotivoPerda(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ErroDeValidacaoException("Informe o motivo da perda ao marcar a oportunidade como PERDIDO.");
        }
    }

    private Cliente clienteObrigatorio(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente nao encontrado com o id " + clienteId + ". A oportunidade exige um cliente existente."));
    }

    private Oportunidade obrigatorio(Long id) {
        return oportunidadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Oportunidade nao encontrada com o id " + id + "."));
    }
}
