import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { Meeting, MeetingStatus } from '../core/models/meeting.model';
import { MeetingsService } from '../core/services/meetings.service';
import { Progress } from './progress';

describe('Progress', () => {
  let fixture: ComponentFixture<Progress>;
  let getSpy: (id: string) => Observable<Meeting>;

  const meetingWithStatus = (status: MeetingStatus): Meeting => ({
    id: 'm1',
    title: 'Reunião de teste',
    originalFilename: 'audio.mp3',
    status,
    uploadedAt: new Date().toISOString(),
    expiresAt: null,
    sentToCrmAt: null,
  });

  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const createFixture = async () => {
    await TestBed.configureTestingModule({
      imports: [Progress],
      providers: [provideRouter([]), { provide: MeetingsService, useValue: { get: (id: string) => getSpy(id) } }],
    }).compileComponents();

    fixture = TestBed.createComponent(Progress);
    fixture.componentRef.setInput('id', 'm1');
  };

  it('polls repeatedly while the meeting is still processing', async () => {
    const calls: MeetingStatus[] = ['TRANSCRIBING', 'SUMMARIZING', 'READY'];
    let callIndex = 0;
    getSpy = vi.fn(() => of(meetingWithStatus(calls[callIndex++]))) as unknown as (id: string) => Observable<Meeting>;

    await createFixture();
    fixture.detectChanges();

    expect(getSpy).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(2500);
    expect(getSpy).toHaveBeenCalledTimes(2);

    vi.advanceTimersByTime(2500);
    expect(getSpy).toHaveBeenCalledTimes(3);
  });

  it('stops polling once the meeting reaches a terminal status', async () => {
    getSpy = vi.fn(() => of(meetingWithStatus('READY'))) as unknown as (id: string) => Observable<Meeting>;

    await createFixture();
    fixture.detectChanges();
    expect(getSpy).toHaveBeenCalledTimes(1);

    vi.advanceTimersByTime(10_000);
    expect(getSpy).toHaveBeenCalledTimes(1);
  });

  it('stops polling and shows an error message when the request fails', async () => {
    getSpy = vi.fn(() => throwError(() => new Error('offline'))) as unknown as (id: string) => Observable<Meeting>;

    await createFixture();
    fixture.detectChanges();

    expect(fixture.componentInstance['errorMessage']()).toBeTruthy();

    vi.advanceTimersByTime(10_000);
    expect(getSpy).toHaveBeenCalledTimes(1);
  });
});
