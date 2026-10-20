package com.nexus.crm.service;

import com.nexus.crm.dto.ClienteRequest;
import com.nexus.crm.dto.ClienteResponse;
import com.nexus.crm.exception.ConflitoDeRegraException;
import com.nexus.crm.exception.RecursoNaoEncontradoException;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.repository.ClienteRepository;
import com.nexus.crm.repository.OportunidadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Regras de negocio do modulo de clientes")
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private OportunidadeRepository oportunidadeRepository;

    @InjectMocks
    private ClienteService clienteService;

    private ClienteRequest requestValido() {
        return new ClienteRequest(
                "Maria Silva",
                "maria.silva@empresa.com.br",
                "(11) 98888-7777",
                "Empresa Exemplo",
                "Analista de Compras",
                "Contato preferencial por e-mail."
        );
    }

    @Test
    @DisplayName("Cadastra um cliente valido normalizando o e-mail")
    void deveCadastrarClienteValido() {
        when(clienteRepository.existsByEmailIgnoreCase("maria.silva@empresa.com.br")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocacao -> {
            Cliente cliente = invocacao.getArgument(0);
            cliente.setId(10L);
            return cliente;
        });
        when(oportunidadeRepository.countByClienteId(10L)).thenReturn(0L);

        ClienteResponse resposta = clienteService.criar(requestValido());

        assertThat(resposta.id()).isEqualTo(10L);
        assertThat(resposta.nome()).isEqualTo("Maria Silva");
        assertThat(resposta.email()).isEqualTo("maria.silva@empresa.com.br");
        assertThat(resposta.totalOportunidades()).isZero();
        verify(clienteRepository).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Recusa o cadastro quando o e-mail ja existe")
    void deveRecusarEmailDuplicado() {
        when(clienteRepository.existsByEmailIgnoreCase("maria.silva@empresa.com.br")).thenReturn(true);

        assertThatThrownBy(() -> clienteService.criar(requestValido()))
                .isInstanceOf(ConflitoDeRegraException.class)
                .hasMessageContaining("maria.silva@empresa.com.br");

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    @DisplayName("Retorna erro quando o cliente nao existe")
    void deveRetornarErroParaClienteInexistente() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Nao exclui cliente que possui oportunidades vinculadas")
    void deveImpedirExclusaoDeClienteComOportunidades() {
        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cliente.setNome("Cliente Protegido");
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(oportunidadeRepository.countByClienteId(5L)).thenReturn(3L);

        assertThatThrownBy(() -> clienteService.excluir(5L))
                .isInstanceOf(ConflitoDeRegraException.class)
                .hasMessageContaining("3 oportunidade(s)");

        verify(clienteRepository, never()).delete(any(Cliente.class));
    }

    @Test
    @DisplayName("Exclui cliente sem oportunidades vinculadas")
    void deveExcluirClienteSemOportunidades() {
        Cliente cliente = new Cliente();
        cliente.setId(5L);
        cliente.setNome("Cliente Livre");
        when(clienteRepository.findById(5L)).thenReturn(Optional.of(cliente));
        when(oportunidadeRepository.countByClienteId(5L)).thenReturn(0L);

        clienteService.excluir(5L);

        verify(clienteRepository).delete(cliente);
    }
}
