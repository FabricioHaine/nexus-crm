import { NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Password } from 'primeng/password';

import { ROTULO_PAPEL } from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-conta-page',
  imports: [ReactiveFormsModule, Button, Password, NgIf],
  templateUrl: './conta-page.html',
  styleUrl: './conta-page.scss',
})
export class ContaPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);

  formulario!: FormGroup;
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);

  /** Usuario autenticado no momento. */
  readonly usuario = this.authService.usuario;

  /** Rotulo do perfil de acesso. */
  rotuloPapel(): string {
    const papel = this.usuario()?.papel;
    return papel ? (ROTULO_PAPEL[papel] ?? papel) : '';
  }

  constructor() {
    this.formulario = this.formBuilder.group({
      senhaAtual: ['', [Validators.required]],
      novaSenha: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
        ],
      ],
      confirmacao: ['', [Validators.required]],
    });

    this.formulario.get('confirmacao')?.valueChanges.subscribe(() => {
      this.formulario.get('confirmacao')?.updateValueAndValidity();
    });
  }

  trocarSenha(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.messageService.add({
        severity: 'warn',
        summary: 'Dados invalidos',
        detail: 'Revise os campos destacados antes de continuar.',
      });
      return;
    }

    const { senhaAtual, novaSenha, confirmacao } = this.formulario.getRawValue();
    if (senhaAtual === novaSenha) {
      this.erro.set('A nova senha deve ser diferente da atual.');
      return;
    }
    if (novaSenha !== confirmacao) {
      this.erro.set('As senhas nao conferem.');
      return;
    }

    this.salvando.set(true);
    this.erro.set(null);

    this.authService.trocarSenha(senhaAtual, novaSenha).subscribe({
      next: () => {
        this.salvando.set(false);
        this.formulario.reset();
        this.messageService.add({
          severity: 'success',
          summary: 'Senha alterada',
          detail: 'Sua senha foi atualizada e as outras sessoes foram encerradas.',
        });
      },
      error: (erro) => {
        this.salvando.set(false);
        this.erro.set(mensagemErro(erro, 'Nao foi possivel alterar a senha.'));
        this.messageService.add({
          severity: 'error',
          summary: 'Nao foi possivel alterar',
          detail: mensagemErro(erro),
        });
      },
    });
  }

  campoInvalido(nome: string): boolean {
    const controle = this.formulario.get(nome);
    return !!controle && controle.invalid && (controle.touched || !!controle.errors?.['api']);
  }

  mensagemDoCampo(nome: string): string {
    const controle = this.formulario.get(nome);
    if (!controle) {
      return '';
    }
    if (nome === 'confirmacao' && this.formulario.hasError('senhasDivergentes')) {
      return 'As senhas nao conferem.';
    }
    if (controle.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (controle.hasError('minlength')) {
      return 'A senha deve ter ao menos 8 caracteres.';
    }
    if (controle.hasError('pattern')) {
      return 'A senha deve conter ao menos uma letra e um numero.';
    }
    return '';
  }
}
