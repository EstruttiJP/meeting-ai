import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Meeting } from '../models/meeting.model';
import { MeetingsService } from './meetings.service';

describe('MeetingsService', () => {
  let service: MeetingsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MeetingsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('list() fetches GET /api/meetings and returns the parsed response', () => {
    const meetings: Meeting[] = [
      {
        id: 'm1',
        title: 'Reunião real',
        originalFilename: 'audio.mp3',
        status: 'READY',
        uploadedAt: new Date().toISOString(),
        expiresAt: null,
        sentToCrmAt: null,
      },
    ];

    let result: Meeting[] | undefined;
    service.list().subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.method).toBe('GET');
    req.flush(meetings);

    expect(result).toEqual(meetings);
  });
});
