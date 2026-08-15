export type MeetingStatus =
  | 'UPLOADED'
  | 'TRANSCRIBING'
  | 'SUMMARIZING'
  | 'READY'
  | 'SENT_TO_CRM'
  | 'EXPIRED'
  | 'FAILED';

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
  failureCategory: MeetingFailureCategory | null;
}
