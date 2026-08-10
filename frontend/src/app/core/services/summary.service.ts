import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';

import { Summary, SummaryContent } from '../models/summary.model';
import { MOCK_MEETINGS } from '../testing/meeting.fixtures';
import { MOCK_SUMMARIES } from '../testing/summary.fixtures';

// Mock temporário: lê/escreve nas fixtures locais em vez de chamar
// GET/PUT /api/meetings/{id}/summary e POST .../send-to-crm.
@Injectable({ providedIn: 'root' })
export class SummaryService {
  get(meetingId: string): Observable<Summary> {
    const summary = MOCK_SUMMARIES[meetingId];
    if (!summary) {
      return throwError(() => ({ status: 404 }));
    }
    return of(summary).pipe(delay(350));
  }

  // PUT edita e aprova numa tacada só — não existe endpoint de aprovar separado.
  update(meetingId: string, content: SummaryContent): Observable<Summary> {
    const current = MOCK_SUMMARIES[meetingId];
    if (!current) {
      return throwError(() => ({ status: 404 }));
    }
    const updated: Summary = { ...current, content, approved: true, approvedAt: new Date().toISOString() };
    MOCK_SUMMARIES[meetingId] = updated;
    return of(updated).pipe(delay(400));
  }

  sendToCrm(meetingId: string): Observable<void> {
    const summary = MOCK_SUMMARIES[meetingId];
    if (!summary) {
      return throwError(() => ({ status: 404 }));
    }
    if (!summary.approved) {
      return throwError(() => ({ status: 409, error: { detail: 'O resumo precisa ser aprovado antes do envio.' } }));
    }
    const meetingIndex = MOCK_MEETINGS.findIndex((m) => m.id === meetingId);
    if (meetingIndex >= 0) {
      MOCK_MEETINGS[meetingIndex] = {
        ...MOCK_MEETINGS[meetingIndex],
        status: 'SENT_TO_CRM',
        sentToCrmAt: new Date().toISOString(),
      };
    }
    return of(undefined).pipe(delay(500));
  }
}
