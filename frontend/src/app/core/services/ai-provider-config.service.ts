import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AiProviderConfig, AiProviderConfigRequest } from '../models/ai-provider-config.model';

@Injectable({ providedIn: 'root' })
export class AiProviderConfigService {
  private readonly http = inject(HttpClient);

  list(): Observable<AiProviderConfig[]> {
    return this.http.get<AiProviderConfig[]>(`${environment.apiBaseUrl}/api/ai-provider-configs`);
  }

  save(request: AiProviderConfigRequest): Observable<AiProviderConfig> {
    return this.http.post<AiProviderConfig>(`${environment.apiBaseUrl}/api/ai-provider-configs`, request);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${environment.apiBaseUrl}/api/ai-provider-configs/${id}`);
  }
}
