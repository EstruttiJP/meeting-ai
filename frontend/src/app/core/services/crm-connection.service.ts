import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';

import { CrmConnection, CrmConnectionRequest } from '../models/crm-connection.model';
import { MOCK_CRM_STATE } from '../testing/crm-connection.fixtures';

// Mock temporário: lê/escreve o estado local em vez de chamar
// GET/POST/DELETE /api/crm-connections.
@Injectable({ providedIn: 'root' })
export class CrmConnectionService {
  me(): Observable<CrmConnection> {
    if (!MOCK_CRM_STATE.connection) {
      return throwError(() => new HttpErrorResponse({ status: 404 })).pipe(delay(300));
    }
    return of(MOCK_CRM_STATE.connection).pipe(delay(300));
  }

  connect(request: CrmConnectionRequest): Observable<CrmConnection> {
    const connection: CrmConnection = {
      id: 'crm-connection-1',
      provider: request.provider,
      connectedAt: new Date().toISOString(),
      tokenExpiresAt: null,
    };
    MOCK_CRM_STATE.connection = connection;
    return of(connection).pipe(delay(500));
  }

  disconnect(): Observable<void> {
    MOCK_CRM_STATE.connection = null;
    return of(undefined).pipe(delay(300));
  }
}
