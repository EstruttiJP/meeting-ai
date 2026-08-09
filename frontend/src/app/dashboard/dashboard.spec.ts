import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Observable, Subject, throwError } from 'rxjs';

import { toAppError } from '../core/http/to-app-error';
import { Meeting } from '../core/models/meeting.model';
import { MeetingsService } from '../core/services/meetings.service';
import { Dashboard } from './dashboard';

describe('Dashboard', () => {
  let fixture: ComponentFixture<Dashboard>;
  let meetingsSubject: Subject<Meeting[]>;
  let meetingsServiceMock: { list: () => Observable<Meeting[]> };

  const MEETING: Meeting = {
    id: 'm1',
    title: 'Reunião de teste',
    originalFilename: 'audio.mp3',
    status: 'READY',
    uploadedAt: new Date().toISOString(),
    expiresAt: null,
    sentToCrmAt: null,
  };

  beforeEach(async () => {
    meetingsSubject = new Subject<Meeting[]>();
    meetingsServiceMock = { list: () => meetingsSubject.asObservable() };

    await TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [provideRouter([]), { provide: MeetingsService, useValue: meetingsServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(Dashboard);
  });

  it('shows the loading state while meetings are being fetched', () => {
    fixture.detectChanges();
    expect(fixture.componentInstance['loading']()).toBe(true);
  });

  it('renders the meeting list once loaded', () => {
    fixture.detectChanges();
    meetingsSubject.next([MEETING]);

    expect(fixture.componentInstance['loading']()).toBe(false);
    expect(fixture.componentInstance['meetings']()).toEqual([MEETING]);
  });

  it('shows an error message when the meetings request fails', () => {
    meetingsServiceMock.list = () => throwError(() => new Error('network down'));
    fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    expect(fixture.componentInstance['loading']()).toBe(false);
    expect(fixture.componentInstance['errorMessage']()).toBe(toAppError(new Error('network down')).message);
  });

  it('points READY meetings to the review screen and TRANSCRIBING ones to progress', () => {
    const instance = fixture.componentInstance;
    expect(instance['actionFor']({ ...MEETING, status: 'READY' })?.link).toEqual([
      '/meetings',
      'm1',
      'review',
    ]);
    expect(instance['actionFor']({ ...MEETING, status: 'TRANSCRIBING' })?.link).toEqual([
      '/meetings',
      'm1',
      'progress',
    ]);
    expect(instance['actionFor']({ ...MEETING, status: 'EXPIRED' })).toBeNull();
  });
});
