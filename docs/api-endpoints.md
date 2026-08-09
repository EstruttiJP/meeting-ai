# API endpoints — Meeting AI

Referência dos endpoints REST do backend, derivada do modelo de dados e do
fluxo de produto descritos em [`PLAIN.md`](../PLAIN.md). Esta é a fundação
de código (entidades, DTOs, controllers) — a maioria dos métodos abaixo
ainda não tem lógica de negócio implementada (ver coluna "Status").

Todas as rotas sob `/api/**` exigem autenticação (ver
[`SecurityConfig`](../backend/src/main/java/com/meetingai/backend/security/SecurityConfig.java)).
`/actuator/health` e `/oauth2/**` ficam liberados.

## Users

| Método | Rota            | Descrição                          | Status |
|--------|-----------------|-------------------------------------|--------|
| GET    | `/api/users/me` | Perfil do usuário autenticado        | 501 — depende do login OAuth2 (ainda não implementado) |

## Usage quota

| Método | Rota                    | Descrição                              | Status |
|--------|-------------------------|------------------------------------------|--------|
| GET    | `/api/usage-quota/me`   | Cota de uso do mês corrente do usuário    | 501 — depende de resolução do usuário autenticado |

## AI provider config

| Método | Rota                              | Descrição                                          | Status |
|--------|------------------------------------|-------------------------------------------------------|--------|
| GET    | `/api/ai-provider-configs`         | Lista as configurações de provider de IA do usuário    | 501 |
| POST   | `/api/ai-provider-configs`         | Cria/atualiza uma configuração (provider + chave própria opcional) | 501 — requer criptografia da chave |
| DELETE | `/api/ai-provider-configs/{id}`    | Remove uma configuração                                 | 501 |

## CRM connection

| Método | Rota                        | Descrição                                  | Status |
|--------|------------------------------|-----------------------------------------------|--------|
| GET    | `/api/crm-connections/me`    | Status da conexão com o CRM (Pipedrive)         | 501 |
| POST   | `/api/crm-connections`       | Salva a conexão após o OAuth do Pipedrive          | 501 — requer troca de código OAuth |
| DELETE | `/api/crm-connections/me`    | Desconecta o CRM                                    | 501 |

## Meetings

| Método | Rota                    | Descrição                                        | Status |
|--------|--------------------------|-----------------------------------------------------|--------|
| GET    | `/api/meetings`          | Lista as reuniões do usuário (dashboard)              | 501 — depende de resolução do usuário autenticado |
| POST   | `/api/meetings`          | Upload de arquivo de reunião (multipart: `title`, `file`) | 501 — requer storage + pipeline assíncrono |
| GET    | `/api/meetings/{id}`     | Detalhe/status de uma reunião                          | 501 |

## Transcription

| Método | Rota                                  | Descrição                  | Status |
|--------|----------------------------------------|--------------------------------|--------|
| GET    | `/api/meetings/{id}/transcription`     | Conteúdo da transcrição gerada    | 501 |

## Summary

| Método | Rota                                        | Descrição                                          | Status |
|--------|-----------------------------------------------|---------------------------------------------------------|--------|
| GET    | `/api/meetings/{id}/summary`                  | Resumo estruturado gerado pelo LLM                        | 501 |
| PUT    | `/api/meetings/{id}/summary`                  | Edição do resumo pelo usuário antes do envio ao CRM          | 501 |
| POST   | `/api/meetings/{id}/summary/send-to-crm`      | Envia o resumo aprovado para o CRM                            | 501 — requer integração Pipedrive |

## Sobre o status 501

Nesta etapa da fundação de código não existe login OAuth2 funcional (login
Google ainda não foi implementado), então **nenhum endpoint consegue
resolver "usuário autenticado"** de forma real — por isso todos os métodos
retornam `501 Not Implemented` em vez de dado mockado: um mock exigiria
inventar a forma da resposta antes de a lógica real (auth, storage,
criptografia, chamadas a LLM/CRM) existir, o que arriscaria divergir do
formato real depois. A validação de entrada (`@Valid`) roda normalmente
antes do stub (requisição inválida retorna `400`, nunca chega a `501`).
