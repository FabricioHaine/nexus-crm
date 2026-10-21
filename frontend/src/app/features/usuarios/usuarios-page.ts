import { DatePipe, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { Select } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';

import { Papel, ROTULO_PAPEL, Usuario } from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-usuarios-page',
  imports: [
    FormsModule,
    Button,
    ButtonDirective,
    TableModule,
    Tag,
    Tooltip,
    Select,
    DatePipe,
    NgIf,
  ],
  templateUrl: './usuarios-page.html',
  styleUrl: './usuarios-page.scss',
})
export class UsuariosPage {
  private readonly usuarioService = inject(UsuarioService);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  readonly usuarios = signal<Usuario[]>([]);
  readonly carregando = signal(true);
  readonly erro = signal<string | null>(null);

  readonly opcoesPapel: { label: string; value: Papel }[] = (
    Object.keys(ROTULO_PAPEL) as Papel[]
  ).map((papel) => ({ label: ROTULO_PAPEL[papel], value: papel }));

  constructor() {
    this.carregar();
  }

  /** Rotulo do perfil de acesso. */
  rotuloPapel(papel: Papel): string {
    return ROTULO_PAPEL[papel] ?? papel;
  }

  /** Severidade da tag conforme o perfil. */
  severidadePapel(papel: Papel): 'danger' | 'warning' | 'info' {
    if (papel === 'ADMIN') {
      return 'danger';
    }
    return papel === 'GERENTE' ? 'warning' : 'info';
  }

  /** O usuario logado nao pode alterar o proprio perfil. */
  ehUsuarioLogado(usuario: Usuario): boolean {
    return usuario.id === this.authService.usuario()?.id;
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.usuarioService.listar().subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erro.set(mensagemErro(erro, 'Nao foi possivel carregar os usuarios.'));
        this.carregando.set(false);
      },
    });
  }

  alterarPapel(usuario: Usuario, papel: Papel): void {
    if (papel === usuario.papel) {
      return;
    }

    this.usuarioService
      .alterar(usuario.id, { papel })
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: 'Perfil atualizado',
            detail: `${usuario.nome} agora e ${this.rotuloPapel(papel)}.`,
          });
          this.carregar();
        },
        error: (erro) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Nao foi possivel alterar',
            detail: mensagemErro(erro),
          });
        },
      });
  }

  confirmarDesativacao(usuario: Usuario): void {
    this.confirmationService.confirm({
      header: usuario.ativo ? 'Desativar usuario' : 'Reativar usuario',
      message: usuario.ativo
        ? `Desativar <strong>${usuario.nome}</strong>? O acesso sera bloqueado imediatamente.`
        : `Reativar o acesso de <strong>${usuario.nome}</strong>?`,
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: usuario.ativo ? 'Desativar' : 'Reativar',
      rejectLabel: 'Cancelar',
      acceptButtonStyleClass: usuario.ativo ? 'p-button-danger' : '',
      accept: () => this.alterarSituacao(usuario),
    });
  }

  private alterarSituacao(usuario: Usuario): void {
    this.usuarioService
      .alterar(usuario.id, { ativo: !usuario.ativo })
      .subscribe({
        next: () => {
          this.messageService.add({
            severity: 'success',
            summary: usuario.ativo ? 'Usuario desativado' : 'Usuario reativado',
            detail: `${usuario.nome} ${usuario.ativo ? 'perdeu' : 'recuperou'} o acesso ao sistema.`,
          });
          this.carregar();
        },
        error: (erro) => {
          this.messageService.add({
            severity: 'error',
            summary: 'Nao foi possivel alterar',
            detail: mensagemErro(erro),
          });
        },
      });
  }
}
