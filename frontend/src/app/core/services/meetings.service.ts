import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import { Meeting } from '../models/meeting.model';
import { MOCK_MEETINGS } from '../testing/meeting.fixtures';

// Mock temporário: devolve fixtures locais em vez de chamar a API real
// (GET /api/meetings). O delay simula latência de rede pra loading state
// ser visível na tela.
@Injectable({ providedIn: 'root' })
export class MeetingsService {
  list(): Observable<Meeting[]> {
    return of(MOCK_MEETINGS).pipe(delay(400));
  }
}
