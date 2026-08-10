import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import { Meeting } from '../models/meeting.model';
import { MOCK_MEETINGS } from '../testing/meeting.fixtures';

// Mock temporário: devolve/gera fixtures locais em vez de chamar a API real
// (GET/POST /api/meetings). O delay simula latência de rede pra loading state
// ser visível na tela.
@Injectable({ providedIn: 'root' })
export class MeetingsService {
  list(): Observable<Meeting[]> {
    return of(MOCK_MEETINGS).pipe(delay(400));
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
