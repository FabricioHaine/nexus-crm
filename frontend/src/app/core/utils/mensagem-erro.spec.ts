import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';

import { mensagemErro } from './mensagem-erro';

describe('mensagemErro', () => {
  it('deve extrair a mensagem geral devolvida pela API', () => {
    const erro = new HttpErrorResponse({
      status: 409,
      error: { mensagem: 'Ja existe um cliente com este e-mail.' },
    });

    expect(mensagemErro(erro)).toBe('Ja existe um cliente com este e-mail.');
  });

  it('deve somar os erros de campo devolvidos pela API', () => {
    const erro = new HttpErrorResponse({
      status: 400,
      error: {
        mensagem: 'Verifique os campos destacados.',
        campos: { email: 'O e-mail informado e invalido' },
      },
    });

    expect(mensagemErro(erro)).toContain('O e-mail informado e invalido');
  });

  it('deve avisar quando a API esta fora do ar', () => {
    const erro = new HttpErrorResponse({ status: 0 });

    expect(mensagemErro(erro)).toContain('Nao foi possivel conectar ao servidor');
  });

  it('deve avisar quando a sessao expirou', () => {
    const erro = new HttpErrorResponse({ status: 401 });

    expect(mensagemErro(erro)).toContain('Sessao expirada');
  });

  it('deve avisar quando o perfil nao permite a operacao', () => {
    const erro = new HttpErrorResponse({ status: 403 });

    expect(mensagemErro(erro)).toContain('perfil de acesso');
  });

  it('deve usar a mensagem padrao quando o erro nao tem informacao util', () => {
    expect(mensagemErro(new Error('boom'), 'Mensagem padrao')).toBe('Mensagem padrao');
  });
});
