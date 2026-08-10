import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import { UsageQuota } from '../models/usage-quota.model';
import { MOCK_USAGE_QUOTA } from '../testing/usage-quota.fixtures';

// Mock temporário: devolve a fixture local em vez de chamar GET /api/usage-quota/me.
@Injectable({ providedIn: 'root' })
export class UsageQuotaService {
  me(): Observable<UsageQuota> {
    return of(MOCK_USAGE_QUOTA).pipe(delay(300));
  }
}
