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
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve autenticar e guardar o usuario da sessao', () => {
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

    expect(service.usuario()?.papel).toBe('ADMIN');
    expect(service.autenticado()).toBe(true);
  });

  it('deve cadastrar um novo usuario e iniciar a sessao', () => {
    service.registrar({ nome: 'Maria', email: 'maria@empresa.com.br', senha: 'Senha@123' }).subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/auth/registro`);
    expect(requisicao.request.method).toBe('POST');

    requisicao.flush({ token: 'token-novo', tipo: 'Bearer', expiraEm: '2026-10-20T22:00:00', usuario });

    expect(service.usuario()?.email).toBe(usuario.email);
  });

  it('deve encerrar a sessao e limpar o estado local', () => {
    service.login({ email: usuario.email, senha: 'Nexus@2026' }).subscribe();
    httpMock.expectOne(`${environment.apiUrl}/auth/login`).flush({
      token: 'token-jwt',
      tipo: 'Bearer',
      expiraEm: '2026-10-20T22:00:00',
      usuario,
    });

    service.logout().subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/auth/logout`);
    expect(requisicao.request.method).toBe('POST');
    requisicao.flush(null);

    expect(service.usuario()).toBeNull();
    expect(service.autenticado()).toBe(false);
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

  it('deve consultar a sessao no servidor uma unica vez', async () => {
    const promessa = service.garantirSessao();
    const segunda = service.garantirSessao();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/auth/me`);
    expect(requisicao.request.method).toBe('GET');
    requisicao.flush(usuario);

    await promessa;
    await segunda;

    expect(service.usuario()?.papel).toBe('ADMIN');
    expect(service.sessaoCarregada()).toBe(true);

    // Uma segunda chamada nao deve gerar nova requisicao.
    await service.garantirSessao();
    httpMock.expectNone(`${environment.apiUrl}/auth/me`);
  });

  it('deve considerar sem sessao quando o servidor recusa a consulta', async () => {
    const promessa = service.garantirSessao();
    httpMock
      .expectOne(`${environment.apiUrl}/auth/me`)
      .flush({ status: 401, mensagem: 'Nao autenticado' }, { status: 401, statusText: 'Unauthorized' });

    await promessa;

    expect(service.usuario()).toBeNull();
    expect(service.autenticado()).toBe(false);
  });

  it('deve renovar a sessao e manter o usuario', () => {
    service.renovar().subscribe((renovado) => {
      expect(renovado?.papel).toBe('ADMIN');
    });

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/auth/refresh`);
    expect(requisicao.request.method).toBe('POST');
    requisicao.flush({ token: 'novo', tipo: 'Bearer', expiraEm: '2026-10-20T22:00:00', usuario });

    expect(service.usuario()?.email).toBe(usuario.email);
  });
});
