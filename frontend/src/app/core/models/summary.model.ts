export interface SummaryContent {
  summary: string;
  decisions: string[] | null;
  nextSteps: string[] | null;
  mentionedValues: string[] | null;
  paymentMethod: string | null;
  objections: string[] | null;
}

export interface Summary {
  id: string;
  meetingId: string;
  content: SummaryContent;
  approved: boolean;
  approvedAt: string | null;
}

export interface SummaryUpdateRequest {
  content: SummaryContent;
}
