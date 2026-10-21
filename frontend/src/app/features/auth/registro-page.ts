import { NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';

import { AuthService } from '../../core/services/auth.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-registro-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    InputText,
    Password,
    NgIf,
  ],
  templateUrl: './registro-page.html',
  styleUrls: ['./auth.scss'],
})
export class RegistroPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);

  formulario!: FormGroup;
  readonly enviando = signal(false);
  readonly erro = signal<string | null>(null);

  constructor() {
    this.formulario = this.formBuilder.group({
      nome: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      email: ['', [Validators.required, Validators.email]],
      senha: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
        ],
      ],
      confirmacao: ['', [Validators.required]],
    }, { validators: RegistroPage.senhasConferem });

    this.formulario.get('confirmacao')?.valueChanges.subscribe(() => {
      this.formulario.get('confirmacao')?.updateValueAndValidity();
    });
  }

  /** Valida que a confirmacao da senha e identica a senha informada. */
  private static senhasConferem(group: FormGroup): Record<string, boolean> | null {
    const senha = group.get('senha')?.value;
    const confirmacao = group.get('confirmacao')?.value;
    return senha && confirmacao && senha !== confirmacao ? { senhasDivergentes: true } : null;
  }

  registrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.messageService.add({
        severity: 'warn',
        summary: 'Dados invalidos',
        detail: 'Revise os campos destacados antes de criar a conta.',
      });
      return;
    }

    const { nome, email, senha } = this.formulario.getRawValue();

    this.enviando.set(true);
    this.erro.set(null);

    this.authService.registrar({ nome, email, senha }).subscribe({
      next: (sessao) => {
        this.enviando.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Conta criada',
          detail: `Bem-vindo, ${sessao.usuario.nome}. Sua conta ja esta ativa.`,
        });
        this.router.navigateByUrl(this.destino());
      },
      error: (erro) => {
        this.enviando.set(false);
        this.erro.set(mensagemErro(erro, 'Nao foi possivel criar a conta.'));
      },
    });
  }

  private destino(): string {
    const pedido = this.activatedRoute.snapshot.queryParamMap.get('redirecionar');
    return pedido && pedido.startsWith('/') ? pedido : '/dashboard';
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
    if (controle.hasError('email')) {
      return 'Informe um e-mail valido.';
    }
    if (controle.hasError('minlength')) {
      return controle.getError('minlength')?.requiredLength === 8
        ? 'A senha deve ter ao menos 8 caracteres.'
        : 'Informe ao menos 3 caracteres.';
    }
    if (controle.hasError('pattern')) {
      return 'A senha deve conter ao menos uma letra e um numero.';
    }
    return '';
  }
}
