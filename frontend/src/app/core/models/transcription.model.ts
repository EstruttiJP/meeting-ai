export interface Transcription {
  id: string;
  meetingId: string;
  content: string;
  language: string | null;
  provider: string;
  createdAt: string;
}
