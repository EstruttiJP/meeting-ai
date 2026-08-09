import { Meeting } from '../models/meeting.model';

// Dados fake usados enquanto cada tela ainda não está ligada à API real
// (ver processo de mock-primeiro no AGENTS.md/PLAIN.md, Etapa 2).
export const MOCK_MEETINGS: Meeting[] = [
  {
    id: '3fa1b1b0-1a1a-4e1a-8a1a-000000000001',
    title: 'Kickoff com Cliente Horizonte',
    originalFilename: 'kickoff-horizonte.mp4',
    status: 'READY',
    uploadedAt: '2026-08-08T13:05:00Z',
    expiresAt: '2026-08-15T13:05:00Z',
    sentToCrmAt: null,
  },
  {
    id: '3fa1b1b0-1a1a-4e1a-8a1a-000000000002',
    title: 'Negociação de contrato — Vértice Ltda',
    originalFilename: 'negociacao-vertice.m4a',
    status: 'SENT_TO_CRM',
    uploadedAt: '2026-08-06T10:30:00Z',
    expiresAt: '2026-08-13T10:30:00Z',
    sentToCrmAt: '2026-08-06T15:12:00Z',
  },
  {
    id: '3fa1b1b0-1a1a-4e1a-8a1a-000000000003',
    title: 'Reunião de descoberta — Padaria Bom Trigo',
    originalFilename: 'descoberta-bom-trigo.mp3',
    status: 'TRANSCRIBING',
    uploadedAt: '2026-08-09T18:40:00Z',
    expiresAt: '2026-08-16T18:40:00Z',
    sentToCrmAt: null,
  },
  {
    id: '3fa1b1b0-1a1a-4e1a-8a1a-000000000004',
    title: 'Follow-up trimestral — Grupo Alameda',
    originalFilename: 'followup-alameda.wav',
    status: 'SUMMARIZING',
    uploadedAt: '2026-08-09T19:10:00Z',
    expiresAt: '2026-08-16T19:10:00Z',
    sentToCrmAt: null,
  },
  {
    id: '3fa1b1b0-1a1a-4e1a-8a1a-000000000005',
    title: 'Renovação de plano — Studio Marimar',
    originalFilename: 'renovacao-marimar.mp4',
    status: 'FAILED',
    uploadedAt: '2026-08-05T09:00:00Z',
    expiresAt: '2026-08-12T09:00:00Z',
    sentToCrmAt: null,
  },
  {
    id: '3fa1b1b0-1a1a-4e1a-8a1a-000000000006',
    title: 'Reunião de alinhamento — Comercial Norte',
    originalFilename: 'alinhamento-comercial-norte.mp3',
    status: 'EXPIRED',
    uploadedAt: '2026-07-20T11:00:00Z',
    expiresAt: '2026-07-27T11:00:00Z',
    sentToCrmAt: null,
  },
];
