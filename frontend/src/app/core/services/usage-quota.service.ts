import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UsageQuota } from '../models/usage-quota.model';

@Injectable({ providedIn: 'root' })
export class UsageQuotaService {
  private readonly http = inject(HttpClient);

  me(): Observable<UsageQuota> {
    return this.http.get<UsageQuota>(`${environment.apiBaseUrl}/api/usage-quota/me`);
  }
}
