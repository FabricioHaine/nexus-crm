import { CurrencyPipe, DatePipe, NgFor, NgIf } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { Skeleton } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';

import { Cliente } from '../../core/models/cliente.model';
import { Oportunidade } from '../../core/models/oportunidade.model';
import { etapaInfo } from '../../core/models/oportunidade.model';
import { ClienteService } from '../../core/services/cliente.service';
import { OportunidadeService } from '../../core/services/oportunidade.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-cliente-detalhes',
  imports: [
    RouterLink,
    Button,
    ButtonDirective,
    TableModule,
    Tag,
    Tooltip,
    Skeleton,
    NgIf,
    DatePipe,
    CurrencyPipe,
  ],
  templateUrl: './cliente-detalhes.html',
  styleUrl: './cliente-detalhes.scss',
})
export class ClienteDetalhes implements OnInit {
  /** Parametro de rota (clientes/:clienteId). */
  readonly clienteId = input<string>();

  private readonly clienteService = inject(ClienteService);
  private readonly oportunidadeService = inject(OportunidadeService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  readonly cliente = signal<Cliente | null>(null);
  readonly oportunidades = signal<Oportunidade[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  ngOnInit(): void {
    const id = Number(this.clienteId());
    this.carregar(id);
  }

  carregar(id: number): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.clienteService.buscarPorId(id).subscribe({
      next: (cliente) => {
        this.cliente.set(cliente);
        this.carregarOportunidades(id);
      },
      error: (erro) => {
        this.erro.set(mensagemErro(erro, 'Cliente nao encontrado.'));
        this.carregando.set(false);
      },
    });
  }

  confirmarExclusao(): void {
    const cliente = this.cliente();
    if (!cliente) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Excluir cliente',
      message: `Deseja realmente excluir <strong>${cliente.nome}</strong>? Esta acao nao pode ser desfeita.`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Excluir',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.excluir(cliente),
    });
  }

  etapaSeveridade(etapa: Oportunidade['etapa']) {
    return etapaInfo(etapa).severidade;
  }

  private carregarOportunidades(id: number): void {
    this.oportunidadeService.listar({ clienteId: id }, 0, 50, 'dataCriacao,desc').subscribe({
      next: (pagina) => {
        this.oportunidades.set(pagina.conteudo);
        this.carregando.set(false);
      },
      error: () => {
        this.oportunidades.set([]);
        this.carregando.set(false);
      },
    });
  }

  private excluir(cliente: Cliente): void {
    this.clienteService.excluir(cliente.id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Cliente excluido',
          detail: `${cliente.nome} foi removido com sucesso.`,
        });
        this.router.navigate(['/clientes']);
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
}
