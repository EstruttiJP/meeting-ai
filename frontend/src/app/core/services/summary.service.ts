import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Summary, SummaryContent, SummaryItemType } from '../models/summary.model';

@Injectable({ providedIn: 'root' })
export class SummaryService {
  private readonly http = inject(HttpClient);

  private summaryUrl(meetingId: string): string {
    return `${environment.apiBaseUrl}/api/meetings/${meetingId}/summary`;
  }

  get(meetingId: string): Observable<Summary> {
    return this.http.get<Summary>(this.summaryUrl(meetingId));
  }

  // PUT edita e aprova numa tacada só — não existe endpoint de aprovar separado.
  update(meetingId: string, content: SummaryContent): Observable<Summary> {
    return this.http.put<Summary>(this.summaryUrl(meetingId), { content });
  }

  // Edição granular: cada chamada mexe num item só, então corrigir uma decisão
  // não reescreve o resto do resumo junto.
  updateText(meetingId: string, summary: string): Observable<Summary> {
    return this.http.patch<Summary>(this.summaryUrl(meetingId), { summary });
  }

  addItem(
    meetingId: string,
    item: { type: SummaryItemType; content: string; timestampSeconds: number | null },
  ): Observable<Summary> {
    return this.http.post<Summary>(`${this.summaryUrl(meetingId)}/items`, item);
  }

  updateItem(meetingId: string, itemId: string, content: string): Observable<Summary> {
    return this.http.patch<Summary>(`${this.summaryUrl(meetingId)}/items/${itemId}`, { content });
  }

  removeItem(meetingId: string, itemId: string): Observable<Summary> {
    return this.http.delete<Summary>(`${this.summaryUrl(meetingId)}/items/${itemId}`);
  }

  sendToCrm(meetingId: string): Observable<void> {
    return this.http.post<void>(`${this.summaryUrl(meetingId)}/send-to-crm`, {});
  }
}
