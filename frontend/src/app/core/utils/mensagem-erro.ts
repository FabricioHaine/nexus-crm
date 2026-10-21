import { HttpErrorResponse } from '@angular/common/http';

/**
 * Converte a resposta de erro da API em uma mensagem exibivel ao usuario.
 * O backend devolve erros estruturados no formato { mensagem, campos }.
 */
export function mensagemErro(erro: unknown, padrao = 'Nao foi possivel completar a operacao.'): string {
  if (erro instanceof HttpErrorResponse) {
    const corpo = erro.error as { mensagem?: string; campos?: Record<string, string> } | null;

    if (corpo?.campos && Object.keys(corpo.campos).length > 0) {
      const detalhe = Object.values(corpo.campos).join(' ');
      return corpo.mensagem ? `${corpo.mensagem} ${detalhe}` : detalhe;
    }
    if (corpo?.mensagem) {
      return corpo.mensagem;
    }
    if (erro.status === 0) {
      return 'Nao foi possivel conectar ao servidor. Verifique se a API esta em execucao.';
    }
    if (erro.status === 401) {
      return 'Sessao expirada ou credenciais invalidas. Entre novamente.';
    }
    if (erro.status === 403) {
      return 'Seu perfil de acesso nao permite executar esta operacao.';
    }
  }
  return padrao;
}
