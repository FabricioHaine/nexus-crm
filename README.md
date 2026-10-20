# Nexus CRM

Sistema de gestão de clientes e oportunidades de vendas para pequenas e médias empresas,
desenvolvido como aplicação **full stack** com **Java 21 + Spring Boot** no back-end,
**Angular + PrimeNG** no front-end e banco de dados relacional.

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-20-DD0031?logo=angular&logoColor=white)](https://angular.dev/)
[![PrimeNG](https://img.shields.io/badge/PrimeNG-20-5367FE?logo=primeng&logoColor=white)](https://primeng.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)

---

## 1. Descrição

O Nexus CRM centraliza o relacionamento com clientes e o acompanhamento do funil comercial.
A aplicação cobre o ciclo completo da negociação: cadastro do cliente, registro da oportunidade,
movimentação entre etapas do funil e leitura de indicadores comerciais calculados sobre os dados
realmente persistidos no banco.

> Todo o dado exibido na interface vem da API Java. Não há números fixos, listas simuladas nem
> persistência em `localStorage`: o front-end é apenas uma camada de apresentação sobre a API REST.

## 2. Problema que o sistema resolve

Pequenas e médias empresas costumam controlar vendas em planilhas. Isso gera três problemas
recorrentes:

| Problema | Como o Nexus CRM resolve |
| --- | --- |
| Dados de clientes espalhados em planilhas, com e-mails duplicados | Cadastro centralizado com e-mail único validado no back-end |
| Nenhuma visão do funil: não se sabe quanto está em negociação | Funil por etapa, avanço de etapa validado e histórico de fechamento |
| Decisões sem base: "quantas vendas fechamos no mês?" | Dashboard com indicadores calculados por agregações no banco |

## 3. Funcionalidades

**Módulo de clientes**
- Cadastro, edição, consulta e exclusão de clientes
- Paginação, ordenação e busca por nome, e-mail ou empresa
- Validação de campos obrigatórios e formato de e-mail
- Bloqueio de e-mail duplicado (HTTP 409) com mensagem clara
- Exclusão bloqueada quando o cliente possui oportunidades vinculadas
- Página de detalhes com histórico de oportunidades do cliente

**Módulo de oportunidades**
- Cadastro vinculado a um cliente existente
- Valor estimado positivo, probabilidade de conversão (0–100 %) e previsão de fechamento
- Avanço de etapa pelo próprio funil, respeitando o fluxo comercial
- Motivo da perda obrigatório ao marcar uma oportunidade como `PERDIDO`
- Filtros por cliente, etapa e período de criação
- Exclusão com confirmação

**Dashboard e funil**
- Total de clientes, oportunidades abertas, vendas ganhas e perdidas
- Valor total de pipeline aberto, valor ganho e valor perdido
- Taxa de conversão com regra documentada
- Gráfico de distribuição por etapa e evolução mensal das vendas
- Página de funil comercial com conversão entre etapas

**Transversal**
- Tratamento centralizado de erros com respostas estruturadas
- Mensagens de sucesso/erro, estados de carregamento e estados de dados vazios
- Interface responsiva (desktop, tablet e celular) e navegação por rotas do Angular

## 4. Tecnologias utilizadas

### Back-end
| Tecnologia | Versão | Papel |
| --- | --- | --- |
| Java | 21 | linguagem principal |
| Spring Boot | 3.5 | framework da aplicação |
| Spring Web | 3.5 | API REST |
| Spring Data JPA | 3.5 | persistência e repositórios |
| Spring Validation | 3.5 | validação de entrada |
| Springdoc OpenAPI | 2.8 | documentação Swagger |
| Flyway | 11 | versionamento do esquema |
| H2 | 2.x | banco padrão de desenvolvimento |
| PostgreSQL | 15+ | banco recomendado para uso contínuo |
| Maven | 3.9 | build (via *maven wrapper*) |
| JUnit 5 + Mockito | 5.x / 5.x | testes unitários |
| Spring Boot Test | 3.5 | testes de integração |

### Front-end
| Tecnologia | Versão | Papel |
| --- | --- | --- |
| Angular | 20 | framework (standalone components, signals) |
| TypeScript | 5.9 | tipagem |
| PrimeNG | 20 | componentes de UI (tabelas, formulários, diálogo, toast) |
| PrimeIcons | 7 | ícones |
| Chart.js | 4.5 | gráficos do dashboard |
| SCSS | — | estilização e design tokens |
| Vitest | 3.2 | testes unitários |

## 5. Arquitetura da aplicação

```
Navegador
   │  HTTP/JSON
   ▼
┌──────────────────────────────────────────────┐
│  Front-end (Angular)                         │
│  Pages (standalone) → Services → HttpClient  │
└──────────────────────────────────────────────┘
   │  REST  /api/clientes · /api/oportunidades · /api/dashboard
   ▼
┌──────────────────────────────────────────────┐
│  Back-end (Spring Boot)                      │
│  Controller → Service → Repository → JPA     │
│        DTO ⇄ Entity (Mapper)                 │
└──────────────────────────────────────────────┘
   │  JDBC
   ▼
   Banco (H2 em desenvolvimento / PostgreSQL)
```

### O caminho de uma requisição

1. **Controller** (`controller/`) — recebe a requisição HTTP, valida o corpo com `@Valid` e
   devolve DTOs. Não contém regra de negócio.
2. **Service** (`service/`) — contém as regras de negócio: e-mail duplicado, exclusão de cliente
   com oportunidades, transições válidas de etapa, cálculo dos indicadores.
3. **Repository** (`repository/`) — interfaces `JpaRepository` com consultas JPQL nomeadas.
4. **Banco** — tabelas `clientes` e `oportunidades` criadas por migrations do Flyway.
5. **Mapper** (`mapper/`) — converte DTO ↔ entidade, evitando expor entidades JPA na API.

### Organização do back-end

```
backend/src/main/java/com/nexus/crm/
├── config/        CORS, OpenAPI e carga opcional de demonstração
├── controller/    Endpoints REST (clientes, oportunidades, dashboard)
├── service/       Regras de negócio e cálculo de indicadores
├── repository/    Consultas ao banco (Spring Data JPA)
├── model/         Entidades JPA e enumeração de etapas
├── dto/           Records de entrada e saída da API
├── mapper/        Conversão entre DTO e entidade
├── exception/     Exceções de domínio e tratamento global de erros
└── NexusCrmApplication.java
```

### Organização do front-end

```
frontend/src/app/
├── core/
│   ├── models/       Interfaces TypeScript dos dados da API
│   ├── services/     ClienteService, OportunidadeService, DashboardService
│   ├── theme/        Preset de tema do PrimeNG
│   └── utils/        Extração de mensagens de erro da API
├── layout/           Menu lateral + cabeçalho (shell da aplicação)
├── features/
│   ├── dashboard/
│   ├── clientes/
│   ├── oportunidades/
│   ├── funil/
│   └── erro/
├── environments/    URL da API por ambiente
└── app.routes.ts    Rotas da aplicação
```

## 6. Estrutura de pastas

```
nexus-crm/
├── backend/                  API REST (Spring Boot)
│   ├── mvnw / mvnw.cmd       Maven wrapper (nao exige Maven instalado)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/nexus/crm/...
│       ├── main/resources/
│       │   ├── application*.properties
│       │   └── db/migration/{h2,postgresql}/   scripts do Flyway
│       └── test/java/com/nexus/crm/...
├── frontend/                 Aplicação web (Angular)
│   ├── package.json
│   ├── angular.json
│   └── src/app/...
├── docs/
│   └── screenshots/          Capturas de tela reais da aplicação
├── .env.example              Modelo de variaveis de ambiente
├── .gitignore
└── README.md
```

## 7. Pré-requisitos

| Item | Versão | Observação |
| --- | --- | --- |
| JDK | 21 ou superior | o projeto compila com `--release 21` |
| Node.js | 20.19+ ou 22.12+ | recomendado 24 LTS |
| npm | 10+ | acompanha o Node.js |
| Maven | 3.9+ | **opcional**: o *maven wrapper* (`mvnw`) é usado no lugar |
| PostgreSQL | 15+ | **opcional**: apenas se for usar o perfil `postgres` |

Não é necessário Docker.

## 8. Como executar o back-end

```bash
cd backend

# Modo desenvolvimento (H2 em arquivo, sem nenhuma configuração)
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

A API sobe em <http://localhost:8080> e o H2 cria o esquema automaticamente na primeira execução.

**Com dados de demonstração** (perfil `demo` — cria 6 clientes e 10 oportunidades de exemplo):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo
```

**Com PostgreSQL** (perfil `postgres`):

```bash
export DB_URL=jdbc:postgresql://localhost:5432/nexus_crm
export DB_USERNAME=nexus
export DB_PASSWORD=nexus
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

> O banco H2 de desenvolvimento fica em `backend/data/nexusdb.mv.db` (pasta ignorada pelo Git).
> Para começar do zero, apague a pasta `backend/data`.

## 9. Como executar o front-end

```bash
cd frontend
npm install
npm start
```

A aplicação fica disponível em <http://localhost:4200>.

| Script | Descrição |
| --- | --- |
| `npm start` | servidor de desenvolvimento em `http://localhost:4200` |
| `npm run build` | build de produção em `frontend/dist/` |
| `npm test` | testes unitários (Vitest) |
| `npm run watch` | build contínuo em modo desenvolvimento |

A URL da API é configurada em `frontend/src/environments/environment.ts` (produção) e
`environment.development.ts` (desenvolvimento). Se o back-end rodar em outra porta, altere
`apiUrl` no arquivo de desenvolvimento **e** a propriedade `app.cors.origins` no back-end.

## 10. Configuração do banco de dados

### H2 (padrão, sem configuração)

O perfil `dev` usa um banco H2 em arquivo:

```
jdbc:h2:file:./data/nexusdb;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1
```

O console do H2 fica disponível em <http://localhost:8080/h2-console> (usuário `sa`, senha vazia).

### PostgreSQL (recomendado para uso contínuo)

```bash
# 1. Criar o banco e o usuário
sudo -u postgres psql -c "CREATE DATABASE nexus_crm;"
sudo -u postgres psql -c "CREATE USER nexus WITH PASSWORD 'nexus';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE nexus_crm TO nexus;"

# 2. Exportar as credenciais (fora do repositório)
export DB_URL=jdbc:postgresql://localhost:5432/nexus_crm
export DB_USERNAME=nexus
export DB_PASSWORD=nexus

# 3. Executar com o perfil postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
```

As migrations são aplicadas automaticamente pelo Flyway. O esquema **nunca** é criado por
`ddl-auto=update`: nos dois bancos o `ddl-auto` é `validate`, e as scripts versionadas em
`backend/src/main/resources/db/migration/{h2,postgresql}` são a única fonte do esquema.

| Perfil | Banco | `ddl-auto` | Quando usar |
| --- | --- | --- | --- |
| `dev` (padrão) | H2 em arquivo | `validate` | primeira execução, demonstrações |
| `postgres` | PostgreSQL | `validate` | desenvolvimento contínuo / produção |
| `demo` | o do perfil acima | — | adiciona a carga de dados de exemplo |

## 11. Endereços locais

| Recurso | Endereço |
| --- | --- |
| Front-end (Angular) | <http://localhost:4200> |
| API REST | <http://localhost:8080/api> |
| Documentação Swagger UI | <http://localhost:8080/swagger-ui.html> |
| OpenAPI (JSON) | <http://localhost:8080/v3/api-docs> |
| Console H2 (perfil `dev`) | <http://localhost:8080/h2-console> |

## 12. Exemplos de requisições

### Clientes

```bash
# Listar (paginado, com busca)
curl "http://localhost:8080/api/clientes?termo=ana&page=0&size=10&sort=nome,asc"

# Consultar
curl http://localhost:8080/api/clientes/1

# Cadastrar
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{
        "nome": "Ana Ribeiro",
        "email": "ana.ribeiro@empresa.com.br",
        "telefone": "(11) 98877-1020",
        "empresa": "Orbitatelecom",
        "cargo": "Gerente de Compras",
        "observacoes": "Prefere contato por e-mail."
      }'

# Atualizar
curl -X PUT http://localhost:8080/api/clientes/1 \
  -H "Content-Type: application/json" \
  -d '{"nome":"Ana Ribeiro","email":"ana.ribeiro@empresa.com.br","empresa":"Orbitatelecom"}'

# Excluir (204; 409 se houver oportunidades vinculadas)
curl -i -X DELETE http://localhost:8080/api/clientes/1
```

### Oportunidades

```bash
# Listar com filtros
curl "http://localhost:8080/api/oportunidades?clienteId=1&etapa=PROPOSTA&dataInicial=2026-01-01&dataFinal=2026-12-31"

# Cadastrar
curl -X POST http://localhost:8080/api/oportunidades \
  -H "Content-Type: application/json" \
  -d '{
        "titulo": "Renovacao do contrato de licencas",
        "descricao": "Contrato anual para 120 usuarios.",
        "clienteId": 1,
        "valorEstimado": 48000.00,
        "etapa": "NOVO_CONTATO",
        "probabilidade": 10,
        "dataPrevistaFechamento": "2026-12-20"
      }'

# Avançar a etapa (409 em transições inválidas)
curl -X PATCH http://localhost:8080/api/oportunidades/1/etapa \
  -H "Content-Type: application/json" \
  -d '{"etapa":"QUALIFICACAO"}'

# Marcar como perdida (motivo obrigatório)
curl -X PATCH http://localhost:8080/api/oportunidades/1/etapa \
  -H "Content-Type: application/json" \
  -d '{"etapa":"PERDIDO","motivoPerda":"Escolheu a concorrencia"}'
```

### Dashboard

```bash
curl http://localhost:8080/api/dashboard/resumo
curl http://localhost:8080/api/dashboard/oportunidades-por-etapa
curl "http://localhost:8080/api/dashboard/vendas-por-periodo?meses=6"
```

### Formato das respostas de erro

```json
{
  "timestamp": "2026-10-20T17:05:58.530",
  "status": 409,
  "erro": "Conflito de regra de negocio",
  "mensagem": "Transicao invalida: nao e possivel mover a oportunidade de Novo contato para Ganho.",
  "caminho": "/api/oportunidades/11/etapa",
  "campos": {}
}
```

| Código | Situação |
| --- | --- |
| 200 / 201 / 204 | sucesso |
| 400 | payload inválido (campos detalhados em `campos`) |
| 404 | recurso inexistente |
| 409 | e-mail duplicado, cliente com oportunidades, transição inválida |

## 13. Como executar os testes

**Back-end** — testes unitários (JUnit 5 + Mockito) e de integração (MockMvc + H2 em memória):

```bash
cd backend
./mvnw test        # apenas testes unitários
./mvnw verify      # unitários + integração (Failsafe)
```

**Front-end** — testes unitários com Vitest:

```bash
cd frontend
npm test
```

### Resultados da última execução

Executados localmente com JDK 24 (compilando com `--release 21`), Maven 3.9.9, Node 24 e
Angular CLI 20:

| Projeto | Comando | Resultado |
| --- | --- | --- |
| Back-end (unitários) | `./mvnw test` | **16 testes**, 0 falhas — `BUILD SUCCESS` |
| Back-end (integração) | `./mvnw verify` | **18 testes**, 0 falhas — `BUILD SUCCESS` |
| Front-end | `npm test` | **25 testes**, 0 falhas — 6 arquivos de teste |
| Front-end (build) | `npm run build` | bundle de 1,48 MB (311 kB gzip), sem erros |

Cobertura dos testes back-end: cadastro válido, validação de campos inválidos, e-mail duplicado,
cliente inexistente, exclusão bloqueada com oportunidades, cadastro de oportunidade, valor
inválido, cliente inexistente na oportunidade, transição para ganho, transição para perda com
motivo obrigatório, transição inválida, reabertura de oportunidade ganha e os cálculos do
dashboard (inclusive o caso sem dados, sem divisão por zero).

## 14. Capturas de tela

As imagens abaixo são capturas reais da aplicação em execução (perfil `demo`), geradas na pasta
`docs/screenshots/`. Para regerá-las com os seus próprios dados:

```bash
# 1. subir a API com dados de exemplo
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo

# 2. subir o front-end
cd frontend && npm start

# 3. capturar a tela (exemplo com Chrome headless)
chrome --headless=new --disable-gpu --hide-scrollbars \
  --window-size=1440,900 --virtual-time-budget=8000 \
  --screenshot=docs/screenshots/dashboard.png http://localhost:4200/dashboard
```

| Dashboard | Clientes |
| --- | --- |
| ![Dashboard](docs/screenshots/dashboard.png) | ![Clientes](docs/screenshots/clientes.png) |

| Oportunidades | Funil comercial |
| --- | --- |
| ![Oportunidades](docs/screenshots/oportunidades.png) | ![Funil](docs/screenshots/funil.png) |

| Cadastro de cliente | Detalhes do cliente |
| --- | --- |
| ![Cadastro de cliente](docs/screenshots/cliente-form.png) | ![Detalhes do cliente](docs/screenshots/cliente-detalhes.png) |

| Cadastro de oportunidade | |
| --- | --- |
| ![Cadastro de oportunidade](docs/screenshots/oportunidade-form.png) | |

## 15. Decisões técnicas

**Java 21 com registros (`record`) para DTOs.**
DTOs imutáveis, com validação declarativa (`@NotBlank`, `@Email`, `@DecimalMin`). Menos código
boilerplate e contrato da API explícito.

**Entidades JPA nunca são expostas nos endpoints.**
O contrato público é a coleção de DTOs em `dto/`. A conversão fica em `mapper/`, o que evita
expor detalhes de persistência (lazy loading, nomes de colunas) e facilita a evolução do modelo.

**Exclusão de cliente com oportunidades é bloqueada (409).**
Apagar em cascata destruiria o histórico comercial. A API recusa a operação com uma mensagem
explícita, e o front-end apenas exibe o erro devolvido.

**Fluxo do funil validado no back-end.**
`EtapaOportunidade.permiteTransicaoPara()` define as transições permitidas
(`NOVO_CONTATO → QUALIFICACAO → PROPOSTA → GANHO | PERDIDO`). O front-end espelha a mesma regra em
`proximasEtapas()` para não oferecer opções inválidas, mas a decisão final é sempre do back-end.

**Flyway com migrations por banco.**
`spring.flyway.locations=classpath:db/migration/{vendor}` seleciona automaticamente os scripts de
H2 ou PostgreSQL. O esquema é versionado e auditável; `ddl-auto=validate` impede que o Hibernate
altere o esquema por conta própria.

**Erros tratados de forma centralizada.**
`GlobalExceptionHandler` converte exceções de domínio, validação do Bean Validation e erros de
integridade em uma resposta JSON única (`timestamp`, `status`, `erro`, `mensagem`, `caminho`,
`campos`), que o front-end consome em `mensagemErro()`.

**Indicadores calculados no banco, não no front-end.**
O dashboard usa agregações (`COUNT`, `SUM`, `GROUP BY`) via JPQL. Números fixos ou aleatórios
seriam desatualizados e falseariam a análise.

**Sinais e `inject()` no Angular.**
`signal` para estado local das telas e `computed` para valores derivados (KPIs e séries dos
gráficos). Evita `subscribe` manual e mudanças de detecção desnecessárias.

**Componentes standalone e rotaslazy por módulo.**
Cada tela é um componente standalone com seu próprio HTML/SCSS, importando apenas o PrimeNG que
utiliza. O bundle inicial fica em ~311 kB (gzipped) apesar da biblioteca de UI completa.

**Testes de integração reais no back-end.**
`ClienteControllerIT`, `OportunidadeControllerIT` e `DashboardControllerIT` sobem o contexto
Spring completo com H2 em memória e validam status HTTP e corpo da resposta — não apenas a lógica
de negócio isolada.

## 16. Limitações conhecidas

- **Sem autenticação.** A primeira versão é local e não implementa login (nada de autenticação
  fictícia). Qualquer pessoa com acesso à porta 8080 usa a API. As rotas não estão protegidas.
- **Usuário único.** Não há multiusuário, nem histórico de *quem* alterou um registro.
- **Busca textual simples.** A busca de clientes usa `LIKE` sobre três campos; não há busca
  full-text nem tolerância a erros de digitação.
- **Relatórios são apenas os do dashboard.** Não há exportação para CSV/PDF nem relatórios
  parametrizáveis.
- **Validação de data prevista.** A API recusa datas de fechamento no passado apenas no
  `POST`/`PUT`; o front-end também aplica esse limite no seletor de data.
- **Sem deploy containerizado.** O projeto foi pensado para execução local, sem Docker.
- **Dependências de desenvolvimento com alertas de segurança.** O `npm audit` aponta avisos em
  `vitest`/`tinypool` (somente no ambiente de testes). O código da aplicação em produção não é
  afetado; a correção exige a migração para o Vitest 5, ainda não suportado pelo builder do
  Angular 20.

## 17. Melhorias futuras

- Autenticação real (Spring Security + JWT) e proteção das rotas por perfil de usuário
- Perfis de acesso (vendedor, gerente, administrador) e registro de auditoria
- Relatórios exportáveis (CSV) e metas por mês/seller
- E-mail de acompanhamento automático ao cliente e ao vendedor
- Importação de contatos via CSV
- Testes end-to-end com Cypress/Playwright sobre a aplicação real
- Pipeline de CI (GitHub Actions) com build, testes e `npm audit`

## 18. Referência rápida

```bash
# Back-end
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo   # API + dados de exemplo
./mvnw verify                                                # testes

# Front-end
cd frontend
npm install
npm start                                                    # http://localhost:4200
npm test                                                     # testes
```

## 19. Licença

Projeto sob a licença **MIT**. Sinta-se à vontade para estudar, reutilizar e contribuir.

```text
MIT License

Copyright (c) 2026 Nexus CRM

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
