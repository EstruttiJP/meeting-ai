export type MeetingStatus =
  | 'UPLOADED'
  | 'TRANSCRIBING'
  | 'SUMMARIZING'
  | 'READY'
  | 'SENT_TO_CRM'
  | 'EXPIRED'
  | 'FAILED';

export interface Meeting {
  id: string;
  title: string;
  originalFilename: string;
  status: MeetingStatus;
  uploadedAt: string;
  expiresAt: string | null;
  sentToCrmAt: string | null;
}
