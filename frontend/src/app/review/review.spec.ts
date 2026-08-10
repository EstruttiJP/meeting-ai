import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { Meeting } from '../core/models/meeting.model';
import { Summary, SummaryContent } from '../core/models/summary.model';
import { MeetingsService } from '../core/services/meetings.service';
import { SummaryService } from '../core/services/summary.service';
import { Review } from './review';

describe('Review', () => {
  let fixture: ComponentFixture<Review>;
  let meetingsServiceMock: { get: (id: string) => Observable<Meeting> };
  let summaryServiceMock: {
    get: (id: string) => Observable<Summary>;
    update: (id: string, content: SummaryContent) => Observable<Summary>;
    sendToCrm: (id: string) => Observable<void>;
  };

  const MEETING: Meeting = {
    id: 'm1',
    title: 'Reunião de teste',
    originalFilename: 'audio.mp3',
    status: 'READY',
    uploadedAt: new Date().toISOString(),
    expiresAt: null,
    sentToCrmAt: null,
  };

  const SUMMARY: Summary = {
    id: 's1',
    meetingId: 'm1',
    content: {
      summary: 'Resumo original',
      decisions: ['Decisão A'],
      nextSteps: null,
      mentionedValues: null,
      paymentMethod: null,
      objections: null,
    },
    approved: false,
    approvedAt: null,
  };

  beforeEach(async () => {
    meetingsServiceMock = { get: () => of(MEETING) };
    summaryServiceMock = {
      get: () => of(SUMMARY),
      update: (_id, content) => of({ ...SUMMARY, content, approved: true, approvedAt: new Date().toISOString() }),
      sendToCrm: () => of(undefined),
    };

    await TestBed.configureTestingModule({
      imports: [Review],
      providers: [
        { provide: MeetingsService, useValue: meetingsServiceMock },
        { provide: SummaryService, useValue: summaryServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Review);
    fixture.componentRef.setInput('id', 'm1');
    fixture.detectChanges();
  });

  it('loads the meeting and pre-fills the draft from the summary content', () => {
    const instance = fixture.componentInstance;
    expect(instance['meeting']()?.title).toBe('Reunião de teste');
    expect(instance['draft']()?.summary).toBe('Resumo original');
    expect(instance['draft']()?.decisions).toEqual(['Decisão A']);
  });

  it('disables approval while the summary text is blank', () => {
    const instance = fixture.componentInstance;
    instance['updateDraft']({ summary: '   ' });
    expect(instance['canApprove']()).toBe(false);
  });

  it('approves and saves, then reveals the send-to-CRM action', () => {
    const instance = fixture.componentInstance;
    instance['approveAndSave']();

    expect(instance['summary']()?.approved).toBe(true);
    expect(instance['approving']()).toBe(false);
  });

  it('sends the approved summary to the CRM and marks the meeting as sent', () => {
    const instance = fixture.componentInstance;
    instance['approveAndSave']();
    instance['sendToCrm']();

    expect(instance['sendingToCrm']()).toBe(false);
    expect(instance['meeting']()?.status).toBe('SENT_TO_CRM');
    expect(instance['alreadySentToCrm']()).toBe(true);
  });

  it('surfaces an error when sending to the CRM fails', () => {
    summaryServiceMock.sendToCrm = () =>
      throwError(
        () => new HttpErrorResponse({ status: 400, error: { detail: 'Nenhuma conexão de CRM configurada.' } }),
      );
    const instance = fixture.componentInstance;
    instance['approveAndSave']();
    instance['sendToCrm']();

    expect(instance['sendError']()).toBe('Nenhuma conexão de CRM configurada.');
  });
});
