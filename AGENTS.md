# AGENTS.md — padrão de desenvolvimento para agentes de IA

Este arquivo vale para qualquer agente de IA (Claude Code incluído)
trabalhando neste repositório. O projeto é mantido por uma única
desenvolvedora, sem revisão humana de PR — a esteira de CI e as regras
abaixo são a única linha de defesa contra código quebrado ou inseguro
chegando na `main`. Trate-as como obrigatórias, não como sugestão.

## Contexto do produto

Meeting AI transcreve reuniões e atualiza CRM automaticamente, sempre com
revisão humana do usuário antes de qualquer envio. O plano completo de
produto está em [`PLAIN.md`](PLAIN.md); decisões de arquitetura já tomadas
estão em [`docs/decisions/`](docs/decisions/) — leia antes de propor uma
mudança estrutural para não repetir uma discussão já resolvida.

## Convenção de nomes

- **Classes Java**: `PascalCase`, sufixo pelo papel (`MeetingController`,
  `MeetingService`, `MeetingRepository`, `TranscriptionProvider`). Pacote
  raiz: `com.meetingai.backend`.
- **Componentes Angular**: seletor com prefixo `app-` (`app-meeting-list`);
  arquivo e classe em `kebab-case`/`PascalCase` conforme o gerador padrão do
  Angular CLI (`ng generate component`) — não crie arquivos à mão quando o
  CLI resolve.
- **Branches**: `tipo/descricao-curta` (ex.: `feat/upload-de-audio`,
  `fix/expiracao-de-reuniao`, `chore/atualizar-dependencias`).
- **Commits**: [Conventional Commits](https://www.conventionalcommits.org/)
  (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`), em português
  ou inglês, mas consistente com o restante do histórico do arquivo que
  está sendo alterado.

## Estrutura de pastas esperada por módulo

**Backend** (`backend/src/main/java/com/meetingai/backend/`) — um pacote
por módulo de domínio, não por camada técnica:

```
backend/src/main/java/com/meetingai/backend/
├── meeting/           # entidade, repositório, service, controller de Meeting
├── transcription/       # TranscriptionProvider e implementações
├── summary/               # SummaryProvider e implementações
├── crm/                     # integração com Pipedrive
└── health/                    # health-check (já existe)
```

Cada módulo de domínio deve ter seu espelho em
`backend/src/test/java/com/meetingai/backend/<modulo>/`.

**Frontend** (`frontend/src/app/`) — uma pasta por feature/tela, não um
`components/` genérico compartilhando tudo:

```
frontend/src/app/
├── dashboard/
├── upload/
├── review/
└── settings/
```

## Testes

- **Toda mudança de lógica de negócio precisa vir acompanhada de teste** no
  mesmo PR — sem exceção, já que não há revisor humano para pedir depois.
  Documentação, configuração e ajuste puramente visual/CSS não exigem teste
  novo, mas não devem quebrar os existentes.
- Backend: teste unitário para regra de negócio (`@ExtendWith(MockitoExtension.class)`
  ou similar); `@WebMvcTest`/`@SpringBootTest` só quando o comportamento
  depender de verdade do contexto Spring.
- Frontend: `ng test` (Vitest) para lógica de componente/serviço; não é
  necessário e2e nesta fase do projeto.
- Rode `mvn test` (backend) e `ng lint && ng test` (frontend) localmente
  antes de abrir PR — é exatamente o que os workflows de CI vão rodar, e
  falhar lá bloqueia o merge (ver branch protection no `README.md`).

## Segredos

- **Nenhuma chave de API, senha ou token pode ser commitado**, em nenhuma
  forma — nem em código, nem em `application.properties`, nem em exemplo
  "temporário". Valores sensíveis entram só via variável de ambiente,
  documentada (sem o valor real) em [`.env.example`](.env.example).
- Se notar um segredo já commitado em qualquer momento (mesmo em um commit
  antigo), pare e avise a desenvolvedora antes de qualquer outra ação — não
  tente "corrigir" reescrevendo histórico sozinho.
- Chaves de API de terceiros que o usuário final colar no produto (ex.:
  chave própria de LLM) devem ficar criptografadas no banco, nunca em texto
  puro — essa regra é do produto, não só do repositório (ver `PLAIN.md`,
  Etapa 1).

## Diante de ambiguidade: perguntar vs assumir

- **Assuma e prossiga** quando a decisão é reversível, local ao código, e
  há um padrão já estabelecido no repositório para seguir (nome de
  variável, formato de resposta de um endpoint semelhante, estrutura de
  pasta de um módulo análogo). Documente a suposição no PR/commit se não for
  óbvia.
- **Pergunte antes de agir** quando a decisão for difícil de reverter
  (mudança de schema de banco já com dados, escolha de provider externo,
  remoção de uma feature, qualquer coisa que toque billing/pagamento) ou
  quando a ambiguidade for sobre **o que o produto deve fazer**, não sobre
  **como implementar** — isso é decisão de produto da desenvolvedora, não do
  agente.
- Nunca contorne a esteira de CI (`--no-verify`, pular teste que falhou,
  forçar merge) para "destravar" uma tarefa. Se o CI está bloqueando,
  o bloqueio é o sistema funcionando como projetado.

## Fluxo de commits

- Nunca acumule uma tarefa inteira em um único commit. Divida em unidades lógicas
  (ex.: "criar entidade", "adicionar endpoint", "adicionar teste do endpoint") e
  commit a cada uma.
- Sempre rode os testes relevantes antes de cada commit. Nunca commitar código
  que quebra o teste, mesmo que seja um commit intermediário.
- Formato de mensagem: Conventional Commits
  (`feat: `, `fix: `, `test: `, `docs: `, `chore: `, `refactor: `)
  Exemplo: `feat(backend): adiciona endpoint de upload de reunião`
- Trabalhe em branch própria por tarefa (`feat/upload-endpoint`), nunca direto na
  `main`. Ao final da tarefa, abra o PR — não faça merge sozinho.
- Se uma tarefa for grande o suficiente para gerar dúvida sobre onde cortar um
  commit, prefira cortar mais cedo, não mais tarde.