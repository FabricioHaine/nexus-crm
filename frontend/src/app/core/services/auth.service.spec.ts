import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Usuario } from '../models/usuario.model';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const usuario: Usuario = {
    id: 1,
    nome: 'Administrador',
    email: 'admin@nexuscrm.local',
    papel: 'ADMIN',
    ativo: true,
    dataCriacao: '2026-10-20T18:00:00',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('deve autenticar e armazenar o token devolvido pela API', () => {
    service.login({ email: usuario.email, senha: 'Nexus@2026' }).subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/auth/login`);
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual({ email: usuario.email, senha: 'Nexus@2026' });

    requisicao.flush({
      token: 'token-jwt',
      tipo: 'Bearer',
      expiraEm: '2026-10-20T22:00:00',
      usuario,
    });

    expect(service.token()).toBe('token-jwt');
    expect(service.usuario()?.papel).toBe('ADMIN');
    expect(service.autenticado()).toBe(true);
    expect(localStorage.getItem('nexus-crm.token')).toBe('token-jwt');
  });

  it('deve cadastrar um novo usuario e iniciar a sessao', () => {
    service.registrar({ nome: 'Maria', email: 'maria@empresa.com.br', senha: 'Senha@123' }).subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/auth/registro`);
    expect(requisicao.request.method).toBe('POST');

    requisicao.flush({ token: 'token-novo', tipo: 'Bearer', expiraEm: '2026-10-20T22:00:00', usuario });

    expect(service.token()).toBe('token-novo');
  });

  it('deve encerrar a sessao e limpar o armazenamento local', () => {
    localStorage.setItem('nexus-crm.token', 'token-jwt');
    localStorage.setItem('nexus-crm.usuario', JSON.stringify(usuario));

    service.logout();

    expect(service.token()).toBeNull();
    expect(service.usuario()).toBeNull();
    expect(service.autenticado()).toBe(false);
    expect(localStorage.getItem('nexus-crm.token')).toBeNull();
  });

  it('deve informar o perfil do usuario autenticado', () => {
    service.login({ email: usuario.email, senha: 'Nexus@2026' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({
      token: 'token-jwt',
      tipo: 'Bearer',
      expiraEm: '2026-10-20T22:00:00',
      usuario,
    });

    expect(service.temPapel('ADMIN')).toBe(true);
    expect(service.temPapel('VENDEDOR')).toBe(false);
    expect(service.papeis()).toEqual(['ADMIN']);
    expect(service.podeExcluir()).toBe(true);
  });

  it('nao deve permitir exclusao para o perfil Vendedor', () => {
    service.login({ email: 'vendedor@empresa.com.br', senha: 'Senha@123' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({
      token: 'token-jwt',
      tipo: 'Bearer',
      expiraEm: '2026-10-20T22:00:00',
      usuario: { ...usuario, papel: 'VENDEDOR' },
    });

    expect(service.podeExcluir()).toBe(false);
  });
});
