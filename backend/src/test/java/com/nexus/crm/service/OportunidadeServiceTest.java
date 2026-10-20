package com.nexus.crm.service;

import com.nexus.crm.dto.AlterarEtapaRequest;
import com.nexus.crm.dto.OportunidadeRequest;
import com.nexus.crm.dto.OportunidadeResponse;
import com.nexus.crm.exception.ConflitoDeRegraException;
import com.nexus.crm.exception.ErroDeValidacaoException;
import com.nexus.crm.exception.RecursoNaoEncontradoException;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Regras de negocio do modulo de oportunidades")
class OportunidadeServiceTest {

    @Mock
    private OportunidadeRepository oportunidadeRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private OportunidadeService oportunidadeService;

    private Cliente cliente() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Empresa Alfa");
        cliente.setEmail("alfa@empresa.com.br");
        return cliente;
    }

    private Oportunidade oportunidade(EtapaOportunidade etapa) {
        Oportunidade oportunidade = new Oportunidade();
        oportunidade.setId(7L);
        oportunidade.setTitulo("Contrato anual");
        oportunidade.setCliente(cliente());
        oportunidade.setValorEstimado(BigDecimal.valueOf(15000));
        oportunidade.setEtapa(etapa);
        oportunidade.setProbabilidade(etapa.getProbabilidadePadrao());
        return oportunidade;
    }

    private OportunidadeRequest requestValido() {
        return new OportunidadeRequest(
                "Contrato anual",
                "Assinatura anual da plataforma.",
                1L,
                BigDecimal.valueOf(15000),
                EtapaOportunidade.NOVO_CONTATO,
                10,
                LocalDate.now().plusDays(30),
                null
        );
    }

    @Test
    @DisplayName("Cadastra uma oportunidade vinculada a um cliente existente")
    void deveCadastrarOportunidade() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente()));
        when(oportunidadeRepository.save(any(Oportunidade.class))).thenAnswer(invocacao -> {
            Oportunidade oportunidade = invocacao.getArgument(0);
            oportunidade.setId(20L);
            return oportunidade;
        });

        OportunidadeResponse resposta = oportunidadeService.criar(requestValido());

        assertThat(resposta.id()).isEqualTo(20L);
        assertThat(resposta.clienteId()).isEqualTo(1L);
        assertThat(resposta.etapa()).isEqualTo(EtapaOportunidade.NOVO_CONTATO);
        assertThat(resposta.valorEstimado()).isEqualByComparingTo("15000");
    }

    @Test
    @DisplayName("Recusa a criacao quando o cliente nao existe")
    void deveRecusarOportunidadeSemClienteExistente() {
        when(clienteRepository.findById(404L)).thenReturn(Optional.empty());

        OportunidadeRequest request = new OportunidadeRequest(
                "Contrato anual", null, 404L, BigDecimal.valueOf(1000),
                EtapaOportunidade.NOVO_CONTATO, 10, LocalDate.now().plusDays(10), null);

        assertThatThrownBy(() -> oportunidadeService.criar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("404");

        verify(oportunidadeRepository, never()).save(any(Oportunidade.class));
    }

    @Test
    @DisplayName("Move a oportunidade para GANHO e registra a data de fechamento")
    void deveAlterarEtapaParaGanho() {
        Oportunidade existente = oportunidade(EtapaOportunidade.PROPOSTA);
        when(oportunidadeRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(oportunidadeRepository.save(any(Oportunidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        OportunidadeResponse resposta = oportunidadeService.alterarEtapa(7L, new AlterarEtapaRequest(EtapaOportunidade.GANHO, null));

        assertThat(resposta.etapa()).isEqualTo(EtapaOportunidade.GANHO);
        assertThat(resposta.probabilidade()).isEqualTo(100);
        assertThat(resposta.dataFechamento()).isNotNull();
    }

    @Test
    @DisplayName("Move a oportunidade para PERDIDO exigindo o motivo da perda")
    void deveAlterarEtapaParaPerdido() {
        Oportunidade existente = oportunidade(EtapaOportunidade.PROPOSTA);
        when(oportunidadeRepository.findById(7L)).thenReturn(Optional.of(existente));
        when(oportunidadeRepository.save(any(Oportunidade.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        OportunidadeResponse resposta = oportunidadeService.alterarEtapa(
                7L, new AlterarEtapaRequest(EtapaOportunidade.PERDIDO, "Escolheu a concorrencia"));

        assertThat(resposta.etapa()).isEqualTo(EtapaOportunidade.PERDIDO);
        assertThat(resposta.motivoPerda()).isEqualTo("Escolheu a concorrencia");
        assertThat(resposta.dataFechamento()).isNotNull();
    }

    @Test
    @DisplayName("Exige o motivo da perda ao marcar a oportunidade como PERDIDO")
    void deveExigirMotivoDaPerda() {
        Oportunidade existente = oportunidade(EtapaOportunidade.PROPOSTA);
        when(oportunidadeRepository.findById(7L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> oportunidadeService.alterarEtapa(
                7L, new AlterarEtapaRequest(EtapaOportunidade.PERDIDO, "  ")))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("motivo da perda");

        verify(oportunidadeRepository, never()).save(any(Oportunidade.class));
    }

    @Test
    @DisplayName("Impede transicoes que nao respeitam o fluxo do funil")
    void deveImpedirTransicaoInvalida() {
        Oportunidade existente = oportunidade(EtapaOportunidade.NOVO_CONTATO);
        when(oportunidadeRepository.findById(7L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> oportunidadeService.alterarEtapa(
                7L, new AlterarEtapaRequest(EtapaOportunidade.GANHO, null)))
                .isInstanceOf(ConflitoDeRegraException.class)
                .hasMessageContaining("Transicao invalida");

        verify(oportunidadeRepository, never()).save(any(Oportunidade.class));
    }

    @Test
    @DisplayName("Impede reabrir uma oportunidade ja ganha")
    void deveImpedirReaberturaDeOportunidadeGanha() {
        Oportunidade existente = oportunidade(EtapaOportunidade.GANHO);
        when(oportunidadeRepository.findById(7L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> oportunidadeService.alterarEtapa(
                7L, new AlterarEtapaRequest(EtapaOportunidade.PROPOSTA, null)))
                .isInstanceOf(ConflitoDeRegraException.class);
    }
}
