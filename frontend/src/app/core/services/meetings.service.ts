import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Meeting, MeetingStatus } from '../models/meeting.model';
import { MOCK_MEETINGS } from '../testing/meeting.fixtures';

const PIPELINE_ORDER: MeetingStatus[] = ['UPLOADED', 'TRANSCRIBING', 'SUMMARIZING', 'READY'];

// get()/upload() ainda são mock temporário: geram fixtures locais em vez de
// chamar a API real. O delay simula latência de rede pra loading state ser
// visível na tela. list() já foi trocado por GET /api/meetings de verdade.
@Injectable({ providedIn: 'root' })
export class MeetingsService {
  private readonly http = inject(HttpClient);

  list(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${environment.apiBaseUrl}/api/meetings`);
  }

  // A cada chamada avança a reunião um passo no pipeline (se ainda não terminou),
  // simulando o processamento em segundo plano pra tela de progresso ter algo
  // para mostrar entre uma sondagem (poll) e outra.
  get(id: string): Observable<Meeting> {
    const index = MOCK_MEETINGS.findIndex((m) => m.id === id);
    if (index === -1) {
      return throwError(() => ({ status: 404 }));
    }
    const current = MOCK_MEETINGS[index];
    const stepIndex = PIPELINE_ORDER.indexOf(current.status);
    const nextStatus =
      stepIndex >= 0 && stepIndex < PIPELINE_ORDER.length - 1 ? PIPELINE_ORDER[stepIndex + 1] : current.status;
    // Sempre um objeto novo (nunca muta o existente): um signal.set() com a mesma
    // referência não notifica os consumidores, mesmo com o valor interno alterado.
    const updated: Meeting = { ...current, status: nextStatus };
    MOCK_MEETINGS[index] = updated;
    return of(updated).pipe(delay(300));
  }

  upload(title: string, file: File): Observable<Meeting> {
    const meeting: Meeting = {
      id: crypto.randomUUID(),
      title,
      originalFilename: file.name,
      status: 'UPLOADED',
      uploadedAt: new Date().toISOString(),
      expiresAt: null,
      sentToCrmAt: null,
    };
    MOCK_MEETINGS.unshift(meeting);
    return of(meeting).pipe(delay(600));
  }
}
