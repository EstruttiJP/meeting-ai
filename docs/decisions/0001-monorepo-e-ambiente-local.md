# 0001 — Monorepo com Docker Compose e LocalStack para ambiente local

- Status: aceito
- Data: 2026-08-09

## Contexto

O Meeting AI é um projeto solo, sem time revisando PRs manualmente. Antes de
qualquer lógica de negócio, era preciso decidir como o repositório seria
organizado e como o ambiente de desenvolvimento seria reproduzido — tanto
pela desenvolvedora quanto por agentes de IA que vierem a trabalhar no
código — sem depender de configuração manual em cada máquina.

## Decisão

- **Monorepo único** (`backend/`, `frontend/`, `docs/decisions/`) em vez de
  repositórios separados. Para um projeto solo em estágio inicial, o custo de
  coordenar dois repositórios (versionamento cruzado, PRs sincronizados) supera
  o benefício de isolamento — o monorepo mantém backend e frontend sempre em
  commits compatíveis.
- **Docker Compose como fonte única de verdade do ambiente local**: um único
  `docker compose up` sobe Postgres, backend, frontend e LocalStack. Nenhum
  passo manual (criar bucket, rodar migração, instalar dependência) deve ser
  necessário depois do `up`.
- **LocalStack em vez de conta AWS real para desenvolvimento**: simula S3 e
  SQS localmente, sem custo e sem depender de internet/credenciais AWS reais
  para trabalhar no projeto. A troca para AWS real acontece só na etapa de
  deploy, alterando `AWS_ENDPOINT_URL` (ver Etapa 4 do `PLAIN.md`).
- **Hot-reload nos dois serviços de aplicação** (Spring Boot DevTools +
  bind mount; `ng serve --poll` + bind mount) para que o ciclo de
  desenvolvimento dentro do container seja igual ao ciclo fora dele — sem
  rebuild de imagem a cada alteração.
- **CI com jobs separados por pasta** (`paths:` filtrando `backend/**` e
  `frontend/**`) para não rodar testes de um módulo quando só o outro mudou,
  e para servir de único "revisor" automático das mudanças, já que não há
  revisão humana de PR nesse projeto.

## Consequências

- Qualquer pessoa (ou agente) que clone o repositório roda o projeto
  completo com um comando, descrito no `README.md`.
- Branch protection no GitHub (configuração manual, fora do código) passa a
  ser a única barreira contra merge de código quebrado — documentada no
  `README.md`.
- Trocar Postgres/S3/SQS locais pelos serviços AWS reais no deploy exige só
  variáveis de ambiente diferentes, não mudança de código (ver `docker-compose.yml`
  e `application.properties`).
