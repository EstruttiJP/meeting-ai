import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CrmConnection, CrmConnectionRequest } from '../models/crm-connection.model';

@Injectable({ providedIn: 'root' })
export class CrmConnectionService {
  private readonly http = inject(HttpClient);

  // 404 quando não há conexão — o componente trata isso como "desconectado",
  // não como erro (ver ApiExceptionHandler não intercepta esse 404 específico,
  // é o comportamento normal do endpoint).
  me(): Observable<CrmConnection> {
    return this.http.get<CrmConnection>(`${environment.apiBaseUrl}/api/crm-connections/me`);
  }

  connect(request: CrmConnectionRequest): Observable<CrmConnection> {
    return this.http.post<CrmConnection>(`${environment.apiBaseUrl}/api/crm-connections`, request);
  }

  disconnect(): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/api/crm-connections/me`);
  }
}
