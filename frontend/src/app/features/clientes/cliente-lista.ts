import { DatePipe, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';

import { Cliente, Pagina } from '../../core/models/cliente.model';
import { ClienteService } from '../../core/services/cliente.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

/** Evento de ordenacao emitido pela tabela do PrimeNG. */
interface EventoOrdenacao {
  sortField?: string | string[];
  sortOrder?: number | null;
}

@Component({
  selector: 'app-cliente-lista',
  imports: [
    FormsModule,
    RouterLink,
    Button,
    ButtonDirective,
    TableModule,
    Tag,
    Tooltip,
    IconField,
    InputIcon,
    InputText,
    NgIf,
    DatePipe,
  ],
  templateUrl: './cliente-lista.html',
  styleUrl: './cliente-lista.scss',
})
export class ClienteLista {
  private readonly clienteService = inject(ClienteService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly router = inject(Router);

  readonly clientes = signal<Cliente[]>([]);
  readonly totalRegistros = signal(0);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly termo = signal('');
  readonly primeiraPagina = signal(0);
  readonly tamanhoPagina = signal(10);
  readonly campoOrdenacao = signal('nome');
  readonly ordemOrdenacao = signal<1 | -1>(1);

  private readonly camposOrdenacaoValidos = [
    'nome',
    'email',
    'empresa',
    'dataCadastro',
    'dataAtualizacao',
    'totalOportunidades',
  ];

  constructor() {
    this.carregar();
  }

  buscar(valor: string): void {
    this.termo.set(valor);
    this.primeiraPagina.set(0);
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    const ordenacao = `${this.campoOrdenacao()},${this.ordemOrdenacao() === 1 ? 'asc' : 'desc'}`;

    this.clienteService
      .listar(this.termo(), this.primeiraPagina(), this.tamanhoPagina(), ordenacao)
      .subscribe({
        next: (pagina: Pagina<Cliente>) => {
          this.clientes.set(pagina.conteudo);
          this.totalRegistros.set(pagina.totalElementos);
          this.carregando.set(false);
        },
        error: (erro) => {
          this.erro.set(mensagemErro(erro, 'Nao foi possivel carregar a lista de clientes.'));
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

  confirmarExclusao(cliente: Cliente): void {
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

  private excluir(cliente: Cliente): void {
    this.clienteService.excluir(cliente.id).subscribe({
      next: () => {
        this.messageService.add({
          severity: 'success',
          summary: 'Cliente excluido',
          detail: `${cliente.nome} foi removido com sucesso.`,
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

  abrirDetalhe(cliente: Cliente): void {
    this.router.navigate(['/clientes', cliente.id]);
  }
}
