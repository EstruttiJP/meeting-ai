import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { apiInterceptor } from './api.interceptor';

describe('apiInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/';
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([apiInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('always sets withCredentials, even for GET requests', () => {
    http.get('http://localhost:8080/api/meetings').subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.withCredentials).toBe(true);
    req.flush([]);
  });

  it('does not attach X-XSRF-TOKEN to GET requests', () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/';
    http.get('http://localhost:8080/api/meetings').subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush([]);
  });

  it('attaches X-XSRF-TOKEN read from the cookie on mutating requests', () => {
    document.cookie = 'XSRF-TOKEN=abc123; path=/';
    http.post('http://localhost:8080/api/meetings', {}).subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('abc123');
    req.flush({});
  });

  it('sends no X-XSRF-TOKEN header when the cookie has not been set yet', () => {
    http.post('http://localhost:8080/api/meetings', {}).subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    req.flush({});
  });
});
