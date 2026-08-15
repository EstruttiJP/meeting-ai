import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Summary, SummaryContent } from '../models/summary.model';

@Injectable({ providedIn: 'root' })
export class SummaryService {
  private readonly http = inject(HttpClient);

  get(meetingId: string): Observable<Summary> {
    return this.http.get<Summary>(`${environment.apiBaseUrl}/api/meetings/${meetingId}/summary`);
  }

  // PUT edita e aprova numa tacada só — não existe endpoint de aprovar separado.
  update(meetingId: string, content: SummaryContent): Observable<Summary> {
    return this.http.put<Summary>(`${environment.apiBaseUrl}/api/meetings/${meetingId}/summary`, { content });
  }

  sendToCrm(meetingId: string): Observable<void> {
    return this.http.post<void>(`${environment.apiBaseUrl}/api/meetings/${meetingId}/summary/send-to-crm`, {});
  }
}
