# API endpoints — Meeting AI

Referência dos endpoints REST do backend, derivada do modelo de dados e do
fluxo de produto descritos em [`PLAIN.md`](../PLAIN.md). Todos os métodos
abaixo têm lógica de negócio implementada — a coluna "Observações" registra
comportamento não óbvio a partir da assinatura (regra de autorização,
efeito colateral, pré-condição).

Todas as rotas sob `/api/**` exigem sessão autenticada via Google OAuth2
(ver [`SecurityConfig`](../backend/src/main/java/com/meetingai/backend/security/SecurityConfig.java)) —
sem sessão, devolvem `401`. `/actuator/health`, `/oauth2/**` e `/login/**`
ficam liberados. `@Valid` roda antes de qualquer lógica: requisição inválida
sempre retorna `400`, nunca chega ao controller.

## Users

| Método | Rota            | Descrição                          |
|--------|-----------------|-------------------------------------|
| GET    | `/api/users/me` | Perfil do usuário autenticado (upsert automático no primeiro login Google) |

## Usage quota

| Método | Rota                    | Descrição                              |
|--------|-------------------------|------------------------------------------|
| GET    | `/api/usage-quota/me`   | Cota de uso do mês corrente — cria a cota com o limite padrão se ainda não existir |

## AI provider config

| Método | Rota                              | Descrição                                          | Observações |
|--------|------------------------------------|-------------------------------------------------------|-------------|
| GET    | `/api/ai-provider-configs`         | Lista as configurações de provider de IA do usuário    | Chave nunca aparece na resposta — só `hasApiKey: boolean` |
| POST   | `/api/ai-provider-configs`         | Cria/atualiza a configuração de um provider (chave própria opcional) | Criptografa a chave antes de salvar; marcar `isDefault: true` desmarca o default anterior; upsert por (usuário, provider) |
| DELETE | `/api/ai-provider-configs/{id}`    | Remove uma configuração                                 | `404` (não `403`) se a config não for do usuário autenticado |

## CRM connection

| Método | Rota                        | Descrição                                  | Observações |
|--------|------------------------------|-----------------------------------------------|-------------|
| GET    | `/api/crm-connections/me`    | Status da conexão com o CRM (Pipedrive)         | `404` se não houver conexão |
| POST   | `/api/crm-connections`       | Troca o código OAuth do Pipedrive por tokens e salva a conexão | Tokens criptografados; substitui conexão anterior do usuário |
| DELETE | `/api/crm-connections/me`    | Desconecta o CRM                                    | |

## Meetings

| Método | Rota                    | Descrição                                        | Observações |
|--------|--------------------------|-----------------------------------------------------|-------------|
| GET    | `/api/meetings`          | Lista as reuniões do usuário (dashboard)              | Sem paginação ainda |
| POST   | `/api/meetings`          | Upload de arquivo de reunião (multipart: `title`, `file`) | Valida formato (mp3/mp4/wav/m4a) e tamanho antes de checar `UsageQuota`; dispara o pipeline assíncrono (transcrição → resumo) e responde `201` sem esperar terminar |
| GET    | `/api/meetings/{id}`     | Detalhe/status de uma reunião                          | `404` se não for do usuário autenticado |
| GET    | `/api/meetings/{id}/audio` | Áudio original, para o player da tela de revisão      | Servido pela API (nunca por URL direta de storage), então posse e retenção são conferidas a cada request; `410` quando o arquivo já foi apagado pela expiração, para a tela diferenciar "expirou" de "deu erro". Range request ainda não suportado |

## Transcription

| Método | Rota                                  | Descrição                  | Observações |
|--------|----------------------------------------|--------------------------------|-------------|
| GET    | `/api/meetings/{id}/transcription`     | Conteúdo da transcrição gerada    | `404` se o pipeline ainda não chegou lá |

## Summary

| Método | Rota                                        | Descrição                                          | Observações |
|--------|-----------------------------------------------|---------------------------------------------------------|-------------|
| GET    | `/api/meetings/{id}/summary`                  | Resumo estruturado gerado pelo LLM                        | `404` se o resumo ainda não existe |
| PUT    | `/api/meetings/{id}/summary`                  | Edição do resumo pelo usuário — é a própria revisão humana, aprova ao salvar | Não existe endpoint de "aprovar" separado; PUT edita e aprova numa tacada só |
| PATCH  | `/api/meetings/{id}/summary`                  | Edita só o texto corrido do resumo, sem tocar nos itens | Não aprova |
| POST   | `/api/meetings/{id}/summary/items`            | Adiciona um item à mão (`type`, `content`, `timestampSeconds`) | O `id` é gerado no servidor, para não colidir com os ids vindos do modelo |
| PATCH  | `/api/meetings/{id}/summary/items/{itemId}`   | Corrige o texto de um item | Tipo e timestamp não mudam: o timestamp está ancorado num trecho real do áudio |
| DELETE | `/api/meetings/{id}/summary/items/{itemId}`   | Remove um item | `404` se o item não existir (tela desatualizada) |
| POST   | `/api/meetings/{id}/summary/send-to-crm`      | Envia o resumo aprovado para o CRM (cria negócio + nota no Pipedrive) | `409` se o resumo não estiver aprovado; `400` se não houver CRM conectado; nunca dispara sozinho, só a partir desta chamada explícita |

O conteúdo do resumo é um texto livre (`summary`) mais uma lista plana de itens
tipados (`items`), cada um com `id`, `type` (`decisao`, `proximo_passo`,
`valor_mencionado`, `ponto_atencao`), `content` e `timestampSeconds` — o ponto da
gravação em que aquilo foi dito, sempre ancorado num segmento real da
transcrição, ou `null` quando não foi possível ancorar. Os endpoints por item
existem para que corrigir uma decisão não reescreva o resumo inteiro junto.

## Pipeline assíncrono

Upload dispara `MeetingPipelineService` em background
(`UPLOADED → TRANSCRIBING → SUMMARIZING → READY`). Qualquer falha em
qualquer etapa (Whisper fora do ar, resposta do LLM fora do schema
esperado, etc.) marca a reunião como `FAILED` em vez de deixar status
inconsistente ou dado malformado salvo. O provider de resumo usado depende
da configuração padrão do usuário em `/api/ai-provider-configs` — sem
configuração própria, cai no OpenRouter (plano free da aplicação).

Um job agendado (`MeetingExpirationJob`, de hora em hora) apaga o arquivo
de áudio e marca `EXPIRED` para reuniões com `expires_at` vencido
(7 dias por padrão, configurável via `MEETING_RETENTION_DAYS`) — o resumo
em texto permanece no banco.
