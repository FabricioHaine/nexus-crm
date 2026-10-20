import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { ClienteService } from './cliente.service';

describe('ClienteService', () => {
  let service: ClienteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ClienteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve listar clientes com paginacao e termo de busca', () => {
    service.listar('ana', 0, 10, 'nome,asc').subscribe();

    const requisicao = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/clientes`,
    );
    expect(requisicao.request.method).toBe('GET');
    expect(requisicao.request.params.get('termo')).toBe('ana');
    expect(requisicao.request.params.get('page')).toBe('0');
    expect(requisicao.request.params.get('size')).toBe('10');
    expect(requisicao.request.params.get('sort')).toBe('nome,asc');
    requisicao.flush({});
  });

  it('nao deve enviar o parametro termo quando a busca estiver vazia', () => {
    service.listar('   ', 0, 10).subscribe();

    const requisicao = httpMock.expectOne(
      (r) => r.url === `${environment.apiUrl}/clientes`,
    );
    expect(requisicao.request.params.has('termo')).toBe(false);
    requisicao.flush({});
  });

  it('deve cadastrar um cliente enviando um POST', () => {
    const payload = {
      nome: 'Maria Silva',
      email: 'maria@empresa.com.br',
      telefone: null,
      empresa: null,
      cargo: null,
      observacoes: null,
    };

    service.criar(payload).subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/clientes`);
    expect(requisicao.request.method).toBe('POST');
    expect(requisicao.request.body).toEqual(payload);
    requisicao.flush({ id: 1 });
  });

  it('deve atualizar um cliente enviando um PUT para o id informado', () => {
    service.atualizar(7, { nome: 'Maria', email: 'maria@empresa.com.br' }).subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/clientes/7`);
    expect(requisicao.request.method).toBe('PUT');
    requisicao.flush({ id: 7 });
  });

  it('deve excluir um cliente enviando um DELETE', () => {
    service.excluir(7).subscribe();

    const requisicao = httpMock.expectOne(`${environment.apiUrl}/clientes/7`);
    expect(requisicao.request.method).toBe('DELETE');
    requisicao.flush(null);
  });

  it('deve propagar o erro devolvido pela API', () => {
    let erroRecebido: unknown = null;
    service.buscarPorId(999).subscribe({
      error: (erro) => (erroRecebido = erro),
    });

    httpMock
      .expectOne(`${environment.apiUrl}/clientes/999`)
      .flush({ status: 404, mensagem: 'Cliente nao encontrado com o id 999.' }, { status: 404, statusText: 'Not Found' });

    expect(erroRecebido).not.toBeNull();
  });
});
