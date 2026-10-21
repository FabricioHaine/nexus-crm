import { CurrencyPipe, DatePipe, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { Dialog } from 'primeng/dialog';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { Tooltip } from 'primeng/tooltip';

import { Cliente, Pagina } from '../../core/models/cliente.model';
import {
  ETAPAS,
  EtapaOportunidade,
  Oportunidade,
  etapaInfo,
  proximasEtapas,
} from '../../core/models/oportunidade.model';
import { ClienteService } from '../../core/services/cliente.service';
import { AuthService } from '../../core/services/auth.service';
import { OportunidadeService } from '../../core/services/oportunidade.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

/** Evento de ordenacao emitido pela tabela do PrimeNG. */
interface EventoOrdenacao {
  sortField?: string | string[];
  sortOrder?: number | null;
}

@Component({
  selector: 'app-oportunidade-lista',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    ButtonDirective,
    TableModule,
    Tag,
    Tooltip,
    Select,
    DatePicker,
    Dialog,
    Textarea,
    NgIf,
    DatePipe,
    CurrencyPipe,
  ],
  templateUrl: './oportunidade-lista.html',
  styleUrl: './oportunidade-lista.scss',
})
export class OportunidadeLista {
  private readonly oportunidadeService = inject(OportunidadeService);
  private readonly clienteService = inject(ClienteService);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  /** Apenas ADMIN e GERENTE podem excluir registros. */
  readonly podeExcluir = this.authService.podeExcluir;

  readonly etapas = ETAPAS;
  readonly oportunidades = signal<Oportunidade[]>([]);
  readonly clientes = signal<Cliente[]>([]);
  readonly totalRegistros = signal(0);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly filtroEtapa = signal<EtapaOportunidade | null>(null);
  readonly filtroCliente = signal<number | null>(null);
  readonly filtroDataInicial = signal<Date | null>(null);
  readonly filtroDataFinal = signal<Date | null>(null);

  readonly primeiraPagina = signal(0);
  readonly tamanhoPagina = signal(10);
  readonly campoOrdenacao = signal('dataCriacao');
  readonly ordemOrdenacao = signal<1 | -1>(-1);

  /** Dialogo de motivo da perda. */
  readonly dialogoPerdaAberto = signal(false);
  readonly motivoPerda = signal('');
  readonly oportunidadeEmPerda = signal<Oportunidade | null>(null);

  readonly opcoesEtapa = computed(() =>
    this.etapas.map((etapa) => ({ label: etapa.rotulo, value: etapa.valor })),
  );

  readonly opcoesCliente = computed(() =>
    this.clientes().map((cliente) => ({
      label: cliente.empresa ? `${cliente.nome} - ${cliente.empresa}` : cliente.nome,
      value: cliente.id,
    })),
  );

  readonly filtrosAtivos = computed(() => {
    const quantidade =
      (this.filtroEtapa() ? 1 : 0) +
      (this.filtroCliente() ? 1 : 0) +
      (this.filtroDataInicial() ? 1 : 0) +
      (this.filtroDataFinal() ? 1 : 0);
    return quantidade;
  });

  private readonly camposOrdenacaoValidos = [
    'titulo',
    'valorEstimado',
    'etapa',
    'dataCriacao',
    'dataPrevistaFechamento',
    'probabilidade',
  ];

  constructor() {
    this.carregar();
    this.carregarClientes();
  }

  aplicarFiltros(): void {
    this.primeiraPagina.set(0);
    this.carregar();
  }

  limparFiltros(): void {
    this.filtroEtapa.set(null);
    this.filtroCliente.set(null);
    this.filtroDataInicial.set(null);
    this.filtroDataFinal.set(null);
    this.primeiraPagina.set(0);
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    const ordenacao = `${this.campoOrdenacao()},${this.ordemOrdenacao() === 1 ? 'asc' : 'desc'}`;
    const dataInicial = this.filtroDataInicial();
    const dataFinal = this.filtroDataFinal();

    this.oportunidadeService
      .listar(
        {
          etapa: this.filtroEtapa(),
          clienteId: this.filtroCliente(),
          dataInicial: dataInicial ? this.formatarData(dataInicial) : null,
          dataFinal: dataFinal ? this.formatarData(dataFinal) : null,
        },
        this.primeiraPagina(),
        this.tamanhoPagina(),
        ordenacao,
      )
      .subscribe({
        next: (pagina: Pagina<Oportunidade>) => {
          this.oportunidades.set(pagina.conteudo);
          this.totalRegistros.set(pagina.totalElementos);
          this.carregando.set(false);
        },
        error: (erro) => {
          this.erro.set(mensagemErro(erro, 'Nao foi possivel carregar as oportunidades.'));
          this.carregando.set(false);
        },
      });
  }

  aoMudarPagina(evento: TableLazyLoadEvent): void {
    this.primeiraPagina.set(evento.first ?? 0);
    this.tamanhoPagina.set(evento.rows ?? 10);
    this.carregar();
  }

  aoOrdenar(evento: EventoOrdenacao): void {
    const campo = Array.isArray(evento.sortField) ? evento.sortField[0] : evento.sortField;
    if (!campo || !this.camposOrdenacaoValidos.includes(campo)) {
      return;
    }
    this.campoOrdenacao.set(campo);
    this.ordemOrdenacao.set(evento.sortOrder === -1 ? -1 : 1);
    this.primeiraPagina.set(0);
    this.carregar();
  }

  etapaSeveridade(etapa: EtapaOportunidade) {
    return etapaInfo(etapa).severidade;
  }

  etapasDisponiveis(etapa: EtapaOportunidade): { label: string; value: EtapaOportunidade }[] {
    return proximasEtapas(etapa).map((valor) => ({
      label: etapaInfo(valor).rotulo,
      value: valor,
    }));
  }

  /** Avanca a oportunidade para a proxima etapa valida. */
  moverEtapa(oportunidade: Oportunidade, novaEtapa: EtapaOportunidade): void {
    if (novaEtapa === 'PERDIDO') {
      this.oportunidadeEmPerda.set(oportunidade);
      this.motivoPerda.set('');
      this.dialogoPerdaAberto.set(true);
      return;
    }
    this.aplicarEtapa(oportunidade, novaEtapa, null);
  }

  confirmarPerda(): void {
    const oportunidade = this.oportunidadeEmPerda();
    if (!oportunidade) {
      return;
    }
    if (!this.motivoPerda().trim()) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Motivo obrigatorio',
        detail: 'Informe o motivo da perda para registrar a oportunidade.',
      });
      return;
    }
    this.aplicarEtapa(oportunidade, 'PERDIDO', this.motivoPerda().trim());
    this.dialogoPerdaAberto.set(false);
  }

  confirmarExclusao(oportunidade: Oportunidade): void {
    this.confirmationService.confirm({
      header: 'Excluir oportunidade',
      message: `Deseja realmente excluir <strong>${oportunidade.titulo}</strong>?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Excluir',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(oportunidade),
    });
  }

  private aplicarEtapa(
    oportunidade: Oportunidade,
    novaEtapa: EtapaOportunidade,
    motivo: string | null,
  ): void {
    this.oportunidadeService
      .alterarEtapa(oportunidade.id, { etapa: novaEtapa, motivoPerda: motivo })
      .subscribe({
        next: (atualizada) => {
          this.messageService.add({
            severity: 'success',
            summary: 'Etapa atualizada',
            detail: `"${atualizada.titulo}" agora esta em ${atualizada.etapaDescricao}.`,
          });
          this.carregar();
        },
        error: (erro) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Nao foi possivel alterar a etapa',
            detail: mensagemErro(erro),
          });
        },
      });
  }

  private excluir(oportunidade: Oportunidade): void {
    this.oportunidadeService.excluir(oportunidade.id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Oportunidade excluida',
          detail: `"${oportunidade.titulo}" foi removida.`,
        });
        this.carregar();
      },
      error: (erro) => {
        this.messageService.add({
          severity: 'error',
          summary: 'Nao foi possivel excluir',
          detail: mensagemErro(erro),
        });
      },
    });
  }

  private carregarClientes(): void {
    this.clienteService.listar(null, 0, 100, 'nome,asc').subscribe({
      next: (pagina) => this.clientes.set(pagina.conteudo),
      error: () => this.clientes.set([]),
    });
  }

  private formatarData(data: Date): string {
    const mes = `${data.getMonth() + 1}`.padStart(2, '0');
    const dia = `${data.getDate()}`.padStart(2, '0');
    return `${data.getFullYear()}-${mes}-${dia}`;
  }
}
