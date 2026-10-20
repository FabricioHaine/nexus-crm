import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { environment } from '../../../environments/environment';
import { ClienteForm } from './cliente-form';

/** Componente vazio usado apenas para satisfazer o roteador nos testes. */
@Component({ standalone: true, template: '' })
class RotaStubComponent {}

describe('ClienteForm', () => {
  let fixture: ComponentFixture<ClienteForm>;
  let httpMock: HttpTestingController;
  let messageService: MessageService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ClienteForm],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([{ path: 'clientes/:id', component: RotaStubComponent }]),
        provideNoopAnimations(),
        providePrimeNG(),
        MessageService,
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    messageService = TestBed.inject(MessageService);
    vi.spyOn(messageService, 'add');

    fixture = TestBed.createComponent(ClienteForm);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve criar o formulario com os campos obrigatorios', () => {
    const formulario = fixture.componentInstance.formulario;

    expect(formulario).toBeTruthy();
    expect(formulario.get('nome')?.hasError('required')).toBe(true);
    expect(formulario.get('email')?.hasError('required')).toBe(true);
  });

  it('nao deve enviar requisicao quando o formulario for invalido', () => {
    fixture.componentInstance.salvar();

    expect(fixture.componentInstance.formulario.invalid).toBe(true);
    expect(messageService.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'warn' }),
    );
    httpMock.expectNone(`${environment.apiUrl}/clientes`);
  });

  it('deve recusar e-mail invalido', () => {
    const formulario = fixture.componentInstance.formulario;
    formulario.patchValue({ nome: 'Maria Silva', email: 'email-sem-arroba' });

    expect(formulario.get('email')?.hasError('email')).toBe(true);
  });

  it('deve cadastrar o cliente e navegar para os detalhes', () => {
    const formulario = fixture.componentInstance.formulario;
    formulario.patchValue({ nome: 'Maria Silva', email: 'maria@empresa.com.br' });

    fixture.componentInstance.salvar();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/clientes`);
    expect(requisicao.request.method).toBe('POST');
    requisicao.flush({ id: 12, nome: 'Maria Silva' });

    expect(messageService.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success' }),
    );
  });

  it('deve exibir a mensagem de erro devolvida pela API', () => {
    const formulario = fixture.componentInstance.formulario;
    formulario.patchValue({ nome: 'Maria Silva', email: 'maria@empresa.com.br' });

    fixture.componentInstance.salvar();

    httpMock
      .expectOne(`${environment.apiUrl}/clientes`)
      .flush(
        { mensagem: 'Ja existe um cliente cadastrado com o e-mail maria@empresa.com.br.' },
        { status: 409, statusText: 'Conflict' },
      );

    expect(messageService.add).toHaveBeenCalledWith(
      expect.objectContaining({
        severity: 'error',
        detail: 'Ja existe um cliente cadastrado com o e-mail maria@empresa.com.br.',
      }),
    );
  });
});
