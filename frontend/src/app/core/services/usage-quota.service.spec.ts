import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { UsageQuota } from '../models/usage-quota.model';
import { UsageQuotaService } from './usage-quota.service';

describe('UsageQuotaService', () => {
  let service: UsageQuotaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(UsageQuotaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('me() fetches GET /api/usage-quota/me', () => {
    const quota: UsageQuota = { id: 'q1', monthReference: '2026-08', meetingsUploaded: 3, meetingsLimit: 10 };

    let result: UsageQuota | undefined;
    service.me().subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/usage-quota/me');
    expect(req.request.method).toBe('GET');
    req.flush(quota);

    expect(result).toEqual(quota);
  });
});
