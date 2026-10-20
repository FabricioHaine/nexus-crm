/** Cliente exibido nas telas e devolvido pela API. */
export interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string | null;
  empresa: string | null;
  cargo: string | null;
  observacoes: string | null;
  dataCadastro: string;
  dataAtualizacao: string;
  totalOportunidades: number;
}

/** Dados enviados para criar ou atualizar um cliente. */
export interface ClientePayload {
  nome: string;
  email: string;
  telefone?: string | null;
  empresa?: string | null;
  cargo?: string | null;
  observacoes?: string | null;
}

/** Envelope de paginacao devolvido pela API. */
export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}
