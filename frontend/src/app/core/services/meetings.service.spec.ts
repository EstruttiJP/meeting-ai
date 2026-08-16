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
        meetingType: 'GENERICA',
        failureCategory: null,
      },
    ];

    let result: Meeting[] | undefined;
    service.list().subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.method).toBe('GET');
    req.flush(meetings);

    expect(result).toEqual(meetings);
  });

  it('upload() posts a multipart request with title and file fields', () => {
    const file = new File(['conteudo'], 'audio.mp3', { type: 'audio/mpeg' });
    const created: Meeting = {
      id: 'm2',
      title: 'Nova reunião',
      originalFilename: 'audio.mp3',
      status: 'UPLOADED',
      uploadedAt: new Date().toISOString(),
      expiresAt: null,
      sentToCrmAt: null,
      meetingType: 'GENERICA',
      failureCategory: null,
    };

    let result: Meeting | undefined;
    service.upload('Nova reunião', file, 'FECHAMENTO').subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings');
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    const body = req.request.body as FormData;
    expect(body.get('title')).toBe('Nova reunião');
    expect(body.get('file')).toBe(file);
    expect(body.get('meetingType')).toBe('FECHAMENTO');
    req.flush(created);

    expect(result).toEqual(created);
  });

  it('get() fetches GET /api/meetings/{id}', () => {
    const meeting: Meeting = {
      id: 'm3',
      title: 'Reunião específica',
      originalFilename: 'audio.mp3',
      status: 'TRANSCRIBING',
      uploadedAt: new Date().toISOString(),
      expiresAt: null,
      sentToCrmAt: null,
      meetingType: 'GENERICA',
      failureCategory: null,
    };

    let result: Meeting | undefined;
    service.get('m3').subscribe((response) => (result = response));

    const req = httpMock.expectOne('http://localhost:8080/api/meetings/m3');
    expect(req.request.method).toBe('GET');
    req.flush(meeting);

    expect(result).toEqual(meeting);
  });
});
