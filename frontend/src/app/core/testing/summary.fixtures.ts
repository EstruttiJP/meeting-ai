import { Summary } from '../models/summary.model';

// Chave = meetingId (ver meeting.fixtures.ts). Só existe resumo pra reuniões
// que já passaram por SUMMARIZING no pipeline (READY ou SENT_TO_CRM).
export const MOCK_SUMMARIES: Record<string, Summary> = {
  '3fa1b1b0-1a1a-4e1a-8a1a-000000000001': {
    id: 'summary-1',
    meetingId: '3fa1b1b0-1a1a-4e1a-8a1a-000000000001',
    content: {
      summary:
        'Reunião de kickoff com o Cliente Horizonte para apresentar o escopo do projeto e alinhar expectativas de prazo.',
      decisions: ['Fechar escopo inicial em 2 semanas', 'Cliente vai indicar um ponto focal técnico'],
      nextSteps: ['Enviar proposta comercial revisada', 'Agendar reunião técnica com o time de TI do cliente'],
      mentionedValues: ['R$ 45.000,00 de investimento inicial'],
      paymentMethod: 'Boleto em 3x',
      objections: ['Preocupação com o prazo de integração com o sistema legado'],
    },
    approved: false,
    approvedAt: null,
  },
  '3fa1b1b0-1a1a-4e1a-8a1a-000000000002': {
    id: 'summary-2',
    meetingId: '3fa1b1b0-1a1a-4e1a-8a1a-000000000002',
    content: {
      summary: 'Negociação final do contrato anual com a Vértice Ltda, já com desconto aprovado pela diretoria.',
      decisions: ['Aplicar 10% de desconto na renovação anual'],
      nextSteps: ['Enviar contrato assinado até sexta-feira'],
      mentionedValues: ['R$ 120.000,00 anual'],
      paymentMethod: 'Transferência trimestral',
      objections: [],
    },
    approved: true,
    approvedAt: '2026-08-06T15:10:00Z',
  },
};
