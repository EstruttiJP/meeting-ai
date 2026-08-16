# Meeting AI

SaaS que transcreve reuniões (áudio/vídeo) e atualiza o CRM automaticamente
a partir do resumo gerado, sempre com revisão humana antes do envio. O
pipeline principal (login com Google, upload, transcrição via Whisper,
resumo estruturado via LLM, revisão, envio ao Pipedrive, expiração
automática do áudio) já roda de ponta a ponta contra o ambiente local —
ver [`docs/api-endpoints.md`](docs/api-endpoints.md) para o contrato de
API completo. O frontend Angular cobre as seis telas desse fluxo (login,
dashboard, upload, progresso, revisão do resumo, configurações), todas
ligadas à API real.

O plano de produto completo (etapas futuras: transcrição, resumo via LLM,
integração com CRM, deploy em AWS) está em [`PLAIN.md`](PLAIN.md). As
decisões de arquitetura tomadas ao longo do caminho ficam registradas em
[`docs/decisions/`](docs/decisions/).

## Arquitetura local

```mermaid
flowchart LR
    subgraph Docker Compose
        FE["frontend<br/>Angular + Tailwind + PrimeNG<br/>:4200"]
        BE["backend<br/>Spring Boot<br/>:8080"]
        PG[("postgres<br/>:5432")]
        LS["localstack<br/>S3 + SQS<br/>:4566"]
        WH["whisper<br/>faster-whisper<br/>:9000"]
    end

    OR(["OpenRouter / OpenAI /<br/>Gemini / Claude"])
    PD(["Pipedrive"])

    Browser(["navegador"]) --> FE
    FE -- "REST /api/** (sessão via cookie, login Google OAuth2)" --> BE
    BE --> PG
    BE -- "transcrição" --> WH
    BE -. "resumo estruturado" .-> OR
    BE -. "OAuth + criação de negócio" .-> PD
    BE -. "reservado p/ Etapa 4 (S3/SQS reais)" .-> LS
```

O áudio de upload fica em disco local (`StorageService`/`LocalDiskStorageService`),
não no LocalStack — o bucket S3 já existe no ambiente local pra Etapa 4
(deploy), mas a aplicação só passa a usá-lo trocando a implementação por
configuração, sem mudar código (ver
[`docs/decisions/`](docs/decisions/)). Em produção, `postgres` vira RDS,
`localstack` vira S3 + SQS reais, `whisper` vira Amazon Transcribe,
`backend` roda em ECS/Fargate e `frontend` é buildado e servido via S3 +
CloudFront — ver Etapa 4 em [`PLAIN.md`](PLAIN.md).

## Como rodar localmente

Pré-requisitos: Docker e Docker Compose. Nada mais precisa estar instalado
na máquina — Java, Maven, Node e Angular CLI já vêm dentro dos containers.

```bash
cp .env.example .env   # valores padrão já sobem o ambiente
docker compose up
```

Isso sobe 5 serviços, sem nenhum passo manual adicional:

| Serviço    | URL local               | O que é                                   |
|------------|--------------------------|--------------------------------------------|
| `frontend` | http://localhost:4200    | Angular, com live reload                   |
| `backend`  | http://localhost:8080    | Spring Boot, com restart automático (DevTools) |
| `postgres` | localhost:5432            | banco de dados                             |
| `localstack` | http://localhost:4566   | S3 + SQS locais, já com bucket e fila criados (reservado pra Etapa 4) |
| `whisper`  | http://localhost:9000    | transcrição local (faster-whisper), sem custo de AWS |

Abra http://localhost:4200 — sem sessão ativa, o guard de rota redireciona
direto pra tela de login (`/login`); o botão "Entrar com Google" navega pra
`/oauth2/authorization/google` no próprio backend, que é quem conduz o
fluxo OAuth2.

Os defaults do `.env.example` sobem o ambiente e deixam todo o pipeline
compilando/testável, mas algumas features só funcionam de verdade com
credenciais reais (documentadas, sem valor, no próprio `.env.example`):
login exige `GOOGLE_CLIENT_ID`/`GOOGLE_CLIENT_SECRET` de um app OAuth do
Google Cloud Console; resumo via LLM (plano free) exige
`OPENROUTER_API_KEY`; envio ao CRM exige `PIPEDRIVE_CLIENT_ID`/`PIPEDRIVE_CLIENT_SECRET`
de um app Pipedrive. Sem isso, o backend sobe normalmente — só a chamada
externa específica falha com uma mensagem clara.

O client id do Pipedrive é o único segredo que o **frontend** também precisa
conhecer (é público por natureza — só o client secret fica só no backend,
usado na troca do código OAuth por token): a tela de Configurações usa
`frontend/src/environments/environment.ts` (`pipedrive.clientId`) pra montar
a URL de autorização e redirecionar o navegador pra lá. Diferente das
variáveis do backend, esse valor não vem do `.env` — é preciso editar o
arquivo diretamente e restartar o `ng serve`. Sem ele, o botão "Conectar
Pipedrive" fica escondido em vez de levar pra um redirect quebrado.

### Variáveis de ambiente

A referência completa — o que cada variável faz, onde obter o valor e o que
acontece sem ela — vive no [`.env.example`](.env.example), comentada linha a
linha. O resumo:

| Variável | Precisa? | Sem ela |
|----------|----------|---------|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | opcional | usa `postgres`/`postgres`/`meetingai` |
| `LOCALSTACK_DEBUG`, `S3_BUCKET_NAME`, `SQS_QUEUE_NAME` | opcional | defaults do Compose |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION` | opcional | `test`/`test`/`us-east-1` — o LocalStack não valida credencial |
| `WHISPER_MODEL` | opcional | `base`; `small`+ transcreve melhor em português e mais devagar |
| `FRONTEND_URL` | opcional | `http://localhost:4200` (origem do CORS e redirect pós-login) |
| **`GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET`** | **sim** | não existe login — e toda rota `/api/**` exige sessão |
| **`APP_ENCRYPTION_KEY` / `APP_ENCRYPTION_SALT`** | **sim** | cai num placeholder commitado (público) para criptografar chave de IA e token de CRM |
| **`OPENROUTER_API_KEY`** | **sim** | transcrição funciona, resumo falha e a reunião termina em `FAILED` |
| `OPENROUTER_MODEL` / `OPENROUTER_BASE_URL` | opcional | `openai/gpt-oss-20b:free` |
| `GEMINI_MODEL` / `OPENAI_MODEL` / `CLAUDE_MODEL` | opcional | só afeta quem configurou chave própria |
| `PIPEDRIVE_CLIENT_ID` / `PIPEDRIVE_CLIENT_SECRET` / `PIPEDRIVE_REDIRECT_URI` | opcional | tudo funciona, menos o envio ao CRM |
| `MEETING_RETENTION_DAYS` / `MEETING_MAX_FILE_SIZE_MB` / `MEETING_EXPIRATION_CRON` | opcional | `7` dias / `200` MB / de hora em hora |
| `USAGE_QUOTA_DEFAULT_MONTHLY_LIMIT` | opcional | `10` uploads por usuário/mês |

Dois detalhes que custam tempo quando passam despercebidos:

- **Deixar em branco não é o mesmo que omitir.** `MEETING_RETENTION_DAYS=`
  entrega string vazia ao container, e o default do Spring não entra — no caso
  de variável numérica, o backend nem sobe. Para usar o default, comente a linha
  (é assim que as opcionais vêm no `.env.example`).
- **Variável nova precisa ser repassada no `docker-compose.yml`**, no bloco
  `environment:` do serviço `backend`. O `application.properties` até lê, mas o
  container só enxerga o que o Compose entrega.

Sobre `APP_ENCRYPTION_KEY`/`APP_ENCRYPTION_SALT`: trocar esses valores torna
ilegível tudo que já foi criptografado com os anteriores — os usuários precisam
colar a chave de IA de novo e reconectar o CRM. Gere um par por ambiente
(`openssl rand -base64 32` e `openssl rand -hex 16`) e não rotacione sem
planejar a migração.

### Hot-reload

- **Backend**: o código-fonte de `backend/` é montado como volume dentro do
  container. `mvn spring-boot:run` sozinho **não** recompila `.java` ao
  salvar — por isso [`backend/dev-entrypoint.sh`](backend/dev-entrypoint.sh)
  roda, em paralelo, um watcher com `entr` que recompila
  (`mvn compile`) a cada alteração; o Spring Boot DevTools detecta as
  classes recompiladas em `target/classes` e reinicia a aplicação sozinho —
  sem `docker compose build`.
- **Frontend**: o código-fonte de `frontend/` também é montado como volume.
  `ng serve` roda com `--poll` para detectar mudanças mesmo em sistemas de
  arquivos de bind mount do Docker que não propagam eventos `inotify`
  (comum em Docker Desktop no Mac/Windows; no Linux nativo funciona também
  sem polling, mas a flag foi deixada ligada para garantir consistência em
  qualquer máquina).
- `node_modules` (frontend) e o cache do Maven (`~/.m2`, backend) ficam em
  volumes nomeados separados, para não serem sobrescritos pelo bind mount
  do código-fonte e não precisarem ser reinstalados a cada `up`.

### LocalStack

O bucket S3 e a fila SQS usados pelo projeto são criados automaticamente
quando o container do LocalStack sobe, via
[`localstack/init-aws.sh`](localstack/init-aws.sh) — não é preciso nenhum
passo manual depois do `docker compose up`. Para conferir:

```bash
docker compose exec localstack awslocal s3 ls
docker compose exec localstack awslocal sqs list-queues
```

## Estrutura de pastas

```
.
├── backend/            # Spring Boot (Java 25) — API
├── frontend/            # Angular + Tailwind — interface web
├── docs/decisions/      # ADRs — decisões de arquitetura, uma por arquivo
├── localstack/           # script de inicialização do S3/SQS local
├── docker-compose.yml    # sobe o ambiente local completo
├── .env.example          # variáveis de ambiente documentadas (sem segredos)
├── AGENTS.md              # padrão de desenvolvimento para agentes de IA
└── PLAIN.md                # plano de produto completo (todas as etapas)
```

## CI

Dois workflows no GitHub Actions, cada um disparado só quando a pasta
correspondente muda (`paths:` no workflow):

- [`.github/workflows/ci-backend.yml`](.github/workflows/ci-backend.yml) — `mvn test` contra um Postgres de serviço.
- [`.github/workflows/ci-frontend.yml`](.github/workflows/ci-frontend.yml) — `ng lint` + `ng test`.

Os dois já estão estruturados para que um job de `deploy` futuro entre com
`needs: test` — ou seja, deploy nunca dispara sem os testes passarem antes
(ver comentários nos próprios arquivos de workflow).
