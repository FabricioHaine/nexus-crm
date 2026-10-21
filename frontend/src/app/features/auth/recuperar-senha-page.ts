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
  selector: 'app-recuperar-senha-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    InputText,
    Password,
    NgIf,
  ],
  templateUrl: './recuperar-senha-page.html',
  styleUrl: './auth.scss',
})
export class RecuperarSenhaPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);

  formulario!: FormGroup;
  readonly enviando = signal(false);
  readonly enviado = signal(false);
  readonly erro = signal<string | null>(null);

  constructor() {
    this.formulario = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
    });
  }

  /** Token recebido pela URL, quando o usuario chega pelo link do e-mail. */
  get token(): string {
    return this.activatedRoute.snapshot.queryParamMap.get('token') ?? '';
  }

  /** O usuario veio pelo link do e-mail: o formulario mostra a nova senha. */
  get redefinindo(): boolean {
    return this.token.length > 0;
  }

  enviar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.authService.recuperarSenha(this.formulario.getRawValue().email).subscribe({
      next: () => {
        this.enviando.set(false);
        this.enviado.set(true);
        this.messageService.add({
          severity: 'success',
          summary: 'Solicitacao enviada',
          detail: 'Se o e-mail estiver cadastrado, voce recebera o link de recuperacao.',
        });
      },
      error: (erro) => {
        this.enviando.set(false);
        this.erro.set(mensagemErro(erro, 'Nao foi possivel solicitar a recuperacao.'));
      },
    });
  }

  redefinir(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    const { novaSenha, confirmacao } = this.formulario.getRawValue();
    if (novaSenha !== confirmacao) {
      this.erro.set('As senhas nao conferem.');
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.authService.redefinirSenha(this.token, novaSenha).subscribe({
      next: () => {
        this.enviando.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Senha redefinida',
          detail: 'Sua nova senha foi salva. Entre com ela a partir de agora.',
        });
        this.router.navigate(['/login']);
      },
      error: (erro) => {
        this.enviando.set(false);
        this.erro.set(mensagemErro(erro, 'Nao foi possivel redefinir a senha.'));
      },
    });
  }

  campoInvalido(nome: string): boolean {
    const controle = this.formulario.get(nome);
    return !!controle && controle.invalid && controle.touched;
  }

  mensagemDoCampo(nome: string): string {
    const controle = this.formulario.get(nome);
    if (!controle) {
      return '';
    }
    if (controle.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (controle.hasError('email')) {
      return 'Informe um e-mail valido.';
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
