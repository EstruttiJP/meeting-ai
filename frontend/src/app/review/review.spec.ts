import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { Meeting } from '../core/models/meeting.model';
import { Summary, SummaryContent, SummaryItem, SummaryItemType } from '../core/models/summary.model';
import { MeetingsService } from '../core/services/meetings.service';
import { SummaryService } from '../core/services/summary.service';
import { Review } from './review';

describe('Review', () => {
  let fixture: ComponentFixture<Review>;
  let meetingsServiceMock: { get: (id: string) => Observable<Meeting>; audioUrl: (id: string) => string };
  let summaryServiceMock: {
    get: (id: string) => Observable<Summary>;
    update: (id: string, content: SummaryContent) => Observable<Summary>;
    updateText: (id: string, summary: string) => Observable<Summary>;
    addItem: (id: string, item: unknown) => Observable<Summary>;
    updateItem: (id: string, itemId: string, content: string) => Observable<Summary>;
    removeItem: (id: string, itemId: string) => Observable<Summary>;
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
    failureCategory: null,
  };

  const ITEMS: SummaryItem[] = [
    { id: 'item-1', type: 'decisao', content: 'Fechar o plano', timestampSeconds: 10 },
    { id: 'item-2', type: 'proximo_passo', content: 'Enviar contrato', timestampSeconds: 60 },
    { id: 'item-3', type: 'ponto_atencao', content: 'Sem âncora', timestampSeconds: null },
  ];

  const SUMMARY: Summary = {
    id: 's1',
    meetingId: 'm1',
    content: { summary: 'Resumo original', items: ITEMS },
    approved: false,
    approvedAt: null,
  };

  const withItems = (items: SummaryItem[]): Summary => ({
    ...SUMMARY,
    content: { ...SUMMARY.content, items },
  });

  const createFixture = async (meeting: Meeting = MEETING) => {
    // Reset explícito: os testes que precisam de outro estado de reunião montam
    // o componente de novo, e o TestBed já foi instanciado pelo beforeEach.
    TestBed.resetTestingModule();
    meetingsServiceMock = { get: () => of(meeting), audioUrl: (id) => `http://api.local/api/meetings/${id}/audio` };
    summaryServiceMock = {
      get: () => of(SUMMARY),
      update: (_id, content) => of({ ...SUMMARY, content, approved: true, approvedAt: new Date().toISOString() }),
      updateText: (_id, summary) => of({ ...SUMMARY, content: { ...SUMMARY.content, summary } }),
      addItem: () =>
        of(
          withItems([
            ...ITEMS,
            { id: 'item-4', type: 'decisao', content: 'Adicionado à mão', timestampSeconds: 5 },
          ]),
        ),
      updateItem: (_id, itemId, content) =>
        of(withItems(ITEMS.map((item) => (item.id === itemId ? { ...item, content } : item)))),
      removeItem: (_id, itemId) => of(withItems(ITEMS.filter((item) => item.id !== itemId))),
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
  };

  beforeEach(async () => {
    await createFixture();
  });

  it('lists the extracted items in chronological order, undated ones last', () => {
    const ids = fixture.componentInstance['items']().map((item) => item.id);
    expect(ids).toEqual(['item-1', 'item-2', 'item-3']);
  });

  it('highlights the item being mentioned at the current playback position', () => {
    const instance = fixture.componentInstance;

    instance['currentTime'].set(11);
    expect(instance['highlightedItemId']()).toBe('item-1');

    instance['currentTime'].set(61);
    expect(instance['highlightedItemId']()).toBe('item-2');
  });

  it('drops the highlight once playback moves past the item', () => {
    const instance = fixture.componentInstance;

    instance['currentTime'].set(11);
    expect(instance['highlightedItemId']()).toBe('item-1');

    // Passou da janela do item-1 e ainda não chegou no item-2.
    instance['currentTime'].set(40);
    expect(instance['highlightedItemId']()).toBeNull();
  });

  it('never highlights an item that has no timestamp', () => {
    const instance = fixture.componentInstance;
    for (const time of [0, 5, 15, 30, 61, 120]) {
      instance['currentTime'].set(time);
      expect(instance['highlightedItemId']()).not.toBe('item-3');
    }
  });

  it('edits a single item without touching the others', () => {
    const instance = fixture.componentInstance;
    instance['startEditingItem'](ITEMS[1]);
    instance['itemDraft'].set('Enviar contrato revisado');
    instance['saveItem']('item-2');

    const items = instance['items']();
    expect(items.find((i) => i.id === 'item-2')?.content).toBe('Enviar contrato revisado');
    expect(items.find((i) => i.id === 'item-1')?.content).toBe('Fechar o plano');
    expect(instance['editingItemId']()).toBeNull();
  });

  it('removes only the targeted item', () => {
    const instance = fixture.componentInstance;
    instance['removeItem']('item-1');

    expect(instance['items']().map((i) => i.id)).toEqual(['item-2', 'item-3']);
  });

  it('adds a manual item anchored at the current playback position', () => {
    const instance = fixture.componentInstance;
    const addSpy = vi.spyOn(summaryServiceMock, 'addItem');
    instance['currentTime'].set(42.7);
    instance['newItemType'].set('decisao' as SummaryItemType);
    instance['newItemContent'].set('Adicionado à mão');
    instance['addItem']();

    expect(addSpy).toHaveBeenCalledWith('m1', {
      type: 'decisao',
      content: 'Adicionado à mão',
      timestampSeconds: 42,
    });
    expect(instance['newItemContent']()).toBe('');
  });

  it('does not send an empty item to the backend', () => {
    const instance = fixture.componentInstance;
    const addSpy = vi.spyOn(summaryServiceMock, 'addItem');
    instance['newItemContent'].set('   ');
    instance['addItem']();

    expect(addSpy).not.toHaveBeenCalled();
  });

  it('edits the summary text on its own, leaving items untouched', () => {
    const instance = fixture.componentInstance;
    instance['startEditingSummary']();
    instance['summaryDraft'].set('Resumo reescrito');
    instance['saveSummaryText']();

    expect(instance['content']()?.summary).toBe('Resumo reescrito');
    expect(instance['items']()).toHaveLength(3);
    expect(instance['summaryDraft']()).toBeNull();
  });

  it('shows the retention message instead of a player for an expired meeting', async () => {
    await createFixture({ ...MEETING, status: 'EXPIRED' });

    expect(fixture.componentInstance['audioExpired']()).toBe(true);
    expect(fixture.nativeElement.querySelector('audio')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('removido após o período de retenção');
  });

  it('falls back to the retention message when the audio request fails', () => {
    const instance = fixture.componentInstance;
    expect(instance['audioExpired']()).toBe(false);

    instance['onAudioError']();
    fixture.detectChanges();

    expect(instance['audioExpired']()).toBe(true);
    expect(fixture.nativeElement.querySelector('audio')).toBeNull();
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
