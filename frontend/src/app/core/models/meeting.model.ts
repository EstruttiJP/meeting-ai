export type MeetingStatus =
  | 'UPLOADED'
  | 'TRANSCRIBING'
  | 'SUMMARIZING'
  | 'READY'
  | 'SENT_TO_CRM'
  | 'EXPIRED'
  | 'FAILED';

/**
 * Tipo escolhido antes do upload. Muda a ênfase do prompt de extração, não a
 * estrutura dos itens do resumo.
 */
export type MeetingType = 'FECHAMENTO' | 'DAILY' | 'APRESENTACAO' | 'GENERICA';

export const MEETING_TYPE_OPTIONS: { value: MeetingType; label: string; hint: string }[] = [
  {
    value: 'FECHAMENTO',
    label: 'Fechamento de negócio',
    hint: 'Prioriza valores, condições e o que ficou acordado',
  },
  {
    value: 'DAILY',
    label: 'Daily / alinhamento',
    hint: 'Prioriza o que cada um vai fazer e o que está travando',
  },
  {
    value: 'APRESENTACAO',
    label: 'Apresentação / pitch',
    hint: 'Prioriza decisões e dúvidas levantadas pela audiência',
  },
  {
    value: 'GENERICA',
    label: 'Genérica',
    hint: 'Sem ênfase — extrai o que aparecer na conversa',
  },
];

/** Etapa do pipeline que falhou. Só vem preenchida quando status é FAILED. */
export type MeetingFailureCategory =
  | 'TRANSCRIPTION'
  | 'SUMMARY'
  | 'INVALID_SUMMARY_FORMAT'
  | 'UNKNOWN';

export interface Meeting {
  id: string;
  title: string;
  originalFilename: string;
  status: MeetingStatus;
  uploadedAt: string;
  expiresAt: string | null;
  sentToCrmAt: string | null;
  meetingType: MeetingType;
  failureCategory: MeetingFailureCategory | null;
}
