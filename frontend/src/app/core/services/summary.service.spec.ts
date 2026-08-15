import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Summary, SummaryContent } from '../models/summary.model';
import { SummaryService } from './summary.service';

describe('SummaryService', () => {
  let service: SummaryService;
  let httpMock: HttpTestingController;

  const CONTENT: SummaryContent = {
    summary: 'Resumo',
    decisions: ['Decisão'],
    nextSteps: null,
    mentionedValues: null,
    paymentMethod: null,
    objections: null,
  };

  const SUMMARY: Summary = {
    id: 's1',
    meetingId: 'm1',
    content: CONTENT,
    approved: false,
    approvedAt: null,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SummaryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('get() fetches GET /api/meetings/{id}/summary', () => {
    let result: Summary | undefined;
    service.get('m1').subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings/m1/summary');
    expect(req.request.method).toBe('GET');
    req.flush(SUMMARY);

    expect(result).toEqual(SUMMARY);
  });

  it('update() puts the content wrapped in { content } and returns the approved summary', () => {
    const approved: Summary = { ...SUMMARY, approved: true, approvedAt: new Date().toISOString() };

    let result: Summary | undefined;
    service.update('m1', CONTENT).subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings/m1/summary');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ content: CONTENT });
    req.flush(approved);

    expect(result).toEqual(approved);
  });

  it('sendToCrm() posts to the send-to-crm endpoint', () => {
    let completed = false;
    service.sendToCrm('m1').subscribe(() => (completed = true));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings/m1/summary/send-to-crm');
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(completed).toBe(true);
  });
});
