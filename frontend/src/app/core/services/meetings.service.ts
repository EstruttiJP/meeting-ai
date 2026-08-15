import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Meeting, MeetingStatus } from '../models/meeting.model';
import { MOCK_MEETINGS } from '../testing/meeting.fixtures';

const PIPELINE_ORDER: MeetingStatus[] = ['UPLOADED', 'TRANSCRIBING', 'SUMMARIZING', 'READY'];

// get() ainda é mock temporário: avança a fixture local em vez de chamar a
// API real, pra tela de progresso ter algo pra mostrar entre uma sondagem e
// outra. list() e upload() já chamam a API de verdade.
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
    const formData = new FormData();
    formData.append('title', title);
    formData.append('file', file);
    return this.http.post<Meeting>(`${environment.apiBaseUrl}/api/meetings`, formData);
  }
}
