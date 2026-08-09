# Plano de desenvolvimento — Anotador de reunião com IA + atualização automática de CRM

Stack: Spring Boot + Angular + AWS
Modelo: usuário sobe áudio/vídeo de reunião → transcrição → resumo estruturado via LLM → revisão humana → atualização no CRM

---

## Etapa 0 — Planejamento e arquitetura

* Definir nome/marca do produto (mesmo que provisório)
* Desenhar modelo de dados inicial: `User`, `Meeting`, `Transcription`, `Summary`, `CrmConnection`, `UsageQuota`
* Desenhar os endpoints principais da API (upload, status, resumo, conexão CRM, configuração de provider de IA)
* Decidir provider de CRM pro MVP: **Pipedrive** (API simples, bem documentada, forte entre PMEs — RD Station/HubSpot ficam pra v2)
* Criar repositório com dois módulos (`backend/`, `frontend/`) ou monorepo simples
* Criar conta AWS separada (ou usar Free Tier) só pra esse projeto, com billing alert configurado desde o dia 1 (evita susto de custo)

---

## Etapa 1 — Backend local (Spring Boot)

* Gerar projeto via Spring Initializr: `web`, `security`, `oauth2-client`, `data-jpa`, `postgresql`, `validation`
* Modelar entidades JPA: `User`, `Meeting` (status: `UPLOADED`, `TRANSCRIBING`, `SUMMARIZING`, `READY`, `SENT_TO_CRM`, `EXPIRED`), `Transcription`, `Summary`, `CrmConnection`, `UsageQuota`
* Configurar Spring Security com **OAuth2 Login apenas via Google** (bloquear qualquer outro fluxo de cadastro — resolve o problema de e-mail temporário sem precisar de verificação manual)
* Endpoint de upload de arquivo (multipart), com validação de:
  * formato aceito (mp3, mp4, wav, m4a)
  * tamanho máximo por arquivo
  * quantidade de arquivos ativos por usuário (`UsageQuota`) — usuário free tem limite X por mês
* Armazenamento: local disk em dev (pasta temporária), abstraído atrás de uma interface `StorageService` pra trocar por S3 sem reescrever nada depois
* Pipeline assíncrono com `@Async` + fila interna (fila real via SQS só entra na Etapa 4, não precisa AWS pra desenvolver)
* Camada de transcrição abstraída (`TranscriptionProvider`): implementação local com Whisper (via container, ex. `faster-whisper`) pra não gastar em AWS durante o desenvolvimento — trocar por Amazon Transcribe na etapa de deploy
* Camada de LLM abstraída (`SummaryProvider` / strategy pattern) com implementações para:
  * OpenRouter (plano free, modelo padrão)
  * Chave própria do usuário (Gemini, GPT ou Claude) — usuário escolhe nas configurações e cola a própria API key, que fica **criptografada no banco**, nunca em texto puro
* Prompt de extração estruturada: pedir ao modelo que devolva JSON (resumo, decisões, próximos passos, valores mencionados, forma de pagamento citada, objeções) — validar o schema da resposta antes de salvar
* Endpoint de revisão: usuário edita o resumo extraído antes de qualquer envio pro CRM (nunca automatizar 100% sem revisão — isso é ponto de venda do produto, não só detalhe técnico)
* Integração com Pipedrive: OAuth da Pipedrive + criação/atualização de negócio via API a partir do resumo aprovado
* Job agendado (`@Scheduled`) que verifica `expires_at` de cada `Meeting` e apaga o arquivo + marca como `EXPIRED` (ex: 7 dias de vida útil pro áudio bruto; o resumo em texto pode durar mais, já que pesa muito menos)
* Testes unitários das partes de negócio (extração de resumo, regra de quota, regra de expiração)

---

## Etapa 2 — Frontend local (Angular + Tailwind + biblioteca de componentes)

* Criar projeto Angular, configurar Tailwind
* Escolher biblioteca de componentes prontos (PrimeNG ou Angular Material — PrimeNG tende a dar mais liberdade visual com Tailwind)
* Telas a construir, nessa ordem:
  1. Login (botão único "Entrar com Google")
  2. Dashboard — lista de reuniões processadas, com status visual (processando, pronta, enviada, expirada)
  3. Upload — arrastar/soltar arquivo, mostrar limite de uso restante do usuário
  4. Tela de progresso — acompanhar status do pipeline em tempo real (polling simples é suficiente, não precisa WebSocket no MVP)
  5. Tela de revisão do resumo — campos editáveis antes de enviar pro CRM
  6. Configurações — conectar CRM (OAuth Pipedrive), escolher provider de IA (OpenRouter free ou colar chave própria)
* Mockar as telas com dados fake antes de plugar no backend (evita ficar bloqueado esperando API pronta)
* Conectar ao backend real, tratar estados de loading/erro em todas as telas
* Gerenciamento de estado com Angular Signals (mais simples que NgRx pra esse escopo)

---

## Etapa 3 — Integrações externas

* Configurar app OAuth no Google Cloud Console (tela de consentimento, client ID/secret)
* Criar conta OpenRouter, gerar chave de API do plano free
* Criar conta sandbox no Pipedrive, registrar app OAuth
* (Opcional, v2) Webhook de gravação do Zoom/Google Meet — deixar de fora do MVP, é a parte mais cara em tempo de desenvolvimento

---

## Etapa 4 — Deploy em AWS

* RDS Postgres (substitui o banco local)
* S3 para os arquivos de áudio — **configurar Lifecycle Rule no próprio bucket** pra expirar objetos automaticamente (isso resolve a "vida útil" na infraestrutura, não só no código da aplicação — mostra que você conhece o recurso nativo certo pra isso)
* Trocar `TranscriptionProvider` local pelo Amazon Transcribe (streaming ou batch)
* SQS entre as etapas do pipeline (upload → transcrição → resumo), com Lambda ou consumer no próprio Spring Boot escutando a fila
* Backend rodando em ECS Fargate (ou Elastic Beanstalk, se quiser algo mais simples de operar sozinho)
* Frontend Angular buildado e servido via S3 + CloudFront
* Secrets (chaves OpenRouter, credenciais OAuth) no Secrets Manager, nunca em variável de ambiente exposta no repositório
* Pipeline CI/CD simples com GitHub Actions (build + deploy automático a cada push na main)

---

## Etapa 5 — Camada de produto (polimento)

* Landing page explicando a proposta (pode ser estática, fora do Angular da aplicação)
* Tela de planos/billing com **Stripe ou Mercado Pago em modo sandbox** — mostra a integração de pagamento sem cobrar ninguém de verdade
* Analytics básico (Plausible ou PostHog, plano free) só pra mostrar que pensa em métricas de produto
* Revisão de segurança:
  * checar CORS
  * sanitizar o conteúdo da transcrição antes de mandar pro LLM (evitar que um trecho da própria reunião vire uma injeção de prompt sem querer)
  * teto de gasto mensal com a API de IA (circuit breaker simples: se passar de X chamadas/mês, avisa e pausa novos processamentos)
* Revisar todos os textos de erro e estados vazios da interface (isso é o que mais entrega "profissional" numa demo)

---

## Etapa 6 — Lançamento do case

* Deploy final com domínio próprio
* Gravar vídeo curto mostrando o fluxo completo (upload → resumo → CRM atualizado)
* Publicar o código (ao menos parte dele) no GitHub com README bem escrito (arquitetura, decisões técnicas, prints)
* Post no LinkedIn contando a motivação do projeto e o problema real que ele resolve