import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Component, ElementRef, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Button } from 'primeng/button';

import { toAppError } from '../core/http/to-app-error';
import { Meeting } from '../core/models/meeting.model';
import {
  SUMMARY_GROUP_ORDER,
  SUMMARY_ITEM_GROUP_LABEL,
  SUMMARY_ITEM_TYPE_LABEL,
  Summary,
  SummaryItem,
  SummaryItemPriority,
  SummaryItemType,
} from '../core/models/summary.model';
import { MeetingsService } from '../core/services/meetings.service';
import { SummaryService } from '../core/services/summary.service';
import { ErrorState } from '../shared/ui/error-state/error-state';
import { LoadingState } from '../shared/ui/loading-state/loading-state';

/**
 * Janela, em segundos, em que um item conta como "sendo falado agora". Curta o
 * bastante para não destacar dois itens seguidos ao mesmo tempo, e longa o
 * bastante para o destaque não piscar entre um segmento e outro.
 */
const HIGHLIGHT_WINDOW_SECONDS = 6;

export type ReviewTab = 'resumo' | 'linha-do-tempo';

/** Um grupo da aba Resumo: um tipo de item com seus itens e o estado de colapso. */
export interface ItemGroup {
  type: SummaryItemType;
  label: string;
  items: SummaryItem[];
}

@Component({
  selector: 'app-review',
  imports: [FormsModule, Button, DatePipe, NgTemplateOutlet, LoadingState, ErrorState],
  templateUrl: './review.html',
  styleUrl: './review.css',
})
export class Review implements OnInit {
  private readonly meetingsService = inject(MeetingsService);
  private readonly summaryService = inject(SummaryService);

  readonly id = input.required<string>();

  private readonly player = viewChild<ElementRef<HTMLAudioElement>>('player');

  protected readonly meeting = signal<Meeting | null>(null);
  protected readonly summary = signal<Summary | null>(null);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly approving = signal(false);
  protected readonly approveError = signal<string | null>(null);

  protected readonly sendingToCrm = signal(false);
  protected readonly sendError = signal<string | null>(null);

  /** Texto do resumo enquanto está sendo editado; null quando não está em edição. */
  protected readonly summaryDraft = signal<string | null>(null);
  /** Id do item em edição e o texto corrente do campo. */
  protected readonly editingItemId = signal<string | null>(null);
  protected readonly itemDraft = signal('');
  protected readonly itemError = signal<string | null>(null);

  protected readonly newItemType = signal<SummaryItemType>('decisao');
  protected readonly newItemContent = signal('');

  /** Posição atual do player, usada para destacar o item correspondente. */
  protected readonly currentTime = signal(0);
  protected readonly audioUnavailable = signal(false);

  protected readonly typeLabels = SUMMARY_ITEM_TYPE_LABEL;
  protected readonly typeOptions: SummaryItemType[] = [
    'decisao',
    'proximo_passo',
    'valor_mencionado',
    'ponto_atencao',
  ];

  protected readonly content = computed(() => this.summary()?.content ?? null);

  /** Itens em ordem cronológica: é assim que a conversa aconteceu. */
  protected readonly items = computed<SummaryItem[]>(() =>
    [...(this.content()?.items ?? [])].sort(
      (a, b) => (a.timestampSeconds ?? Infinity) - (b.timestampSeconds ?? Infinity),
    ),
  );

  /** A aba Resumo abre primeiro: quem chega quer ver o que importa, não a ordem de menção. */
  protected readonly activeTab = signal<ReviewTab>('resumo');

  /** Grupos começam fechados; abrir é escolha de quem quer o detalhe. */
  private readonly expandedGroups = signal<ReadonlySet<SummaryItemType>>(new Set());

  protected readonly groups = computed<ItemGroup[]>(() =>
    SUMMARY_GROUP_ORDER.map((type) => ({
      type,
      label: SUMMARY_ITEM_GROUP_LABEL[type],
      items: this.items().filter((item) => item.type === type),
    })).filter((group) => group.items.length > 0),
  );

  /**
   * Item de destaque: o mais crítico da reunião. Só existe quando algo foi
   * marcado como prioridade alta — por decisão do modelo ou da própria usuária.
   * Sem isso o card some, em vez de eleger um item qualquer.
   */
  protected readonly highlightItem = computed<SummaryItem | null>(
    () => this.items().find((item) => item.priority === 'alta') ?? null,
  );

  protected readonly alreadySentToCrm = computed(() => this.meeting()?.status === 'SENT_TO_CRM');
  protected readonly canApprove = computed(
    () => !!this.content()?.summary.trim() && !this.approving(),
  );

  protected readonly audioSrc = computed(() => this.meetingsService.audioUrl(this.id()));

  /** O áudio original só existe enquanto a reunião não expirou. */
  protected readonly audioExpired = computed(
    () => this.meeting()?.status === 'EXPIRED' || this.audioUnavailable(),
  );

  /**
   * Item que está sendo mencionado no ponto atual da gravação. Só um por vez:
   * dois destaques simultâneos tirariam o sentido do recurso.
   */
  protected readonly highlightedItemId = computed(() => {
    const time = this.currentTime();
    const candidates = this.items().filter(
      (item) =>
        item.timestampSeconds !== null &&
        time >= item.timestampSeconds &&
        time < item.timestampSeconds + HIGHLIGHT_WINDOW_SECONDS,
    );
    if (candidates.length === 0) {
      return null;
    }
    // O mais recente vence, para o destaque acompanhar a conversa.
    return candidates[candidates.length - 1].id;
  });

  ngOnInit() {
    this.load();
  }

  protected load() {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      meeting: this.meetingsService.get(this.id()),
      summary: this.summaryService.get(this.id()),
    }).subscribe({
      next: ({ meeting, summary }) => {
        this.meeting.set(meeting);
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(toAppError(error).message);
        this.loading.set(false);
      },
    });
  }

  protected onTimeUpdate(event: Event) {
    this.currentTime.set((event.target as HTMLAudioElement).currentTime);
  }

  protected onAudioError() {
    // O endpoint responde 410 quando o arquivo já foi apagado pela retenção.
    this.audioUnavailable.set(true);
  }

  /**
   * Leva o player até o ponto em que o item foi dito. O elemento vem por
   * viewChild porque a tag <audio> vive dentro de um bloco @if — uma variável
   * de template não seria visível na lista de itens, que está fora dele.
   */
  protected seekTo(item: SummaryItem) {
    const player = this.player()?.nativeElement;
    if (item.timestampSeconds === null || !player) {
      return;
    }
    player.currentTime = item.timestampSeconds;
    this.currentTime.set(item.timestampSeconds);
  }

  protected formatTimestamp(seconds: number | null): string {
    if (seconds === null) {
      return '--:--';
    }
    const total = Math.round(seconds);
    return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`;
  }

  protected startEditingSummary() {
    this.summaryDraft.set(this.content()?.summary ?? '');
  }

  protected cancelEditingSummary() {
    this.summaryDraft.set(null);
  }

  protected saveSummaryText() {
    const text = this.summaryDraft();
    if (text === null || !text.trim()) {
      return;
    }
    this.summaryService.updateText(this.id(), text).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.summaryDraft.set(null);
      },
      error: (error) => this.itemError.set(toAppError(error).message),
    });
  }

  protected startEditingItem(item: SummaryItem) {
    this.editingItemId.set(item.id);
    this.itemDraft.set(item.content);
    this.itemError.set(null);
  }

  protected cancelEditingItem() {
    this.editingItemId.set(null);
    this.itemDraft.set('');
  }

  protected saveItem(itemId: string) {
    const text = this.itemDraft();
    if (!text.trim()) {
      return;
    }
    this.summaryService.updateItem(this.id(), itemId, { content: text }).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.cancelEditingItem();
      },
      error: (error) => this.itemError.set(toAppError(error).message),
    });
  }

  protected isGroupExpanded(type: SummaryItemType): boolean {
    return this.expandedGroups().has(type);
  }

  protected toggleGroup(type: SummaryItemType) {
    const next = new Set(this.expandedGroups());
    if (!next.delete(type)) {
      next.add(type);
    }
    this.expandedGroups.set(next);
  }

  /**
   * Promove ou rebaixa um item no card de destaque. Só decisão e próximo passo
   * têm prioridade — o botão nem aparece nos outros tipos.
   */
  protected togglePriority(item: SummaryItem) {
    const priority: SummaryItemPriority = item.priority === 'alta' ? 'normal' : 'alta';
    this.summaryService.updateItem(this.id(), item.id, { priority }).subscribe({
      next: (summary) => this.summary.set(summary),
      error: (error) => this.itemError.set(toAppError(error).message),
    });
  }

  /** O contexto do ng-template chega como any, então o acesso ao rótulo passa por aqui. */
  protected typeLabel(item: SummaryItem): string {
    return SUMMARY_ITEM_TYPE_LABEL[item.type];
  }

  protected canHavePriority(item: SummaryItem): boolean {
    return item.type === 'decisao' || item.type === 'proximo_passo';
  }

  /** Leva para a linha do tempo já no ponto do item, a partir do card de destaque. */
  protected openInTimeline(item: SummaryItem) {
    this.activeTab.set('linha-do-tempo');
    this.seekTo(item);
  }

  protected removeItem(itemId: string) {
    this.summaryService.removeItem(this.id(), itemId).subscribe({
      next: (summary) => this.summary.set(summary),
      error: (error) => this.itemError.set(toAppError(error).message),
    });
  }

  protected addItem() {
    const text = this.newItemContent();
    if (!text.trim()) {
      return;
    }
    // Item criado à mão nasce ancorado no ponto em que o player está: quem
    // anotou algo que faltou normalmente acabou de ouvir esse trecho.
    const timestamp = this.audioExpired() ? null : Math.floor(this.currentTime());
    this.summaryService
      .addItem(this.id(), { type: this.newItemType(), content: text, timestampSeconds: timestamp })
      .subscribe({
        next: (summary) => {
          this.summary.set(summary);
          this.newItemContent.set('');
        },
        error: (error) => this.itemError.set(toAppError(error).message),
      });
  }

  protected approveAndSave() {
    const content = this.content();
    if (!content || !this.canApprove()) {
      return;
    }
    this.approving.set(true);
    this.approveError.set(null);
    this.summaryService.update(this.id(), content).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.approving.set(false);
      },
      error: (error) => {
        this.approveError.set(toAppError(error).message);
        this.approving.set(false);
      },
    });
  }

  protected sendToCrm() {
    this.sendingToCrm.set(true);
    this.sendError.set(null);
    this.summaryService.sendToCrm(this.id()).subscribe({
      next: () => {
        this.sendingToCrm.set(false);
        const meeting = this.meeting();
        if (meeting) {
          this.meeting.set({ ...meeting, status: 'SENT_TO_CRM', sentToCrmAt: new Date().toISOString() });
        }
      },
      error: (error) => {
        this.sendingToCrm.set(false);
        this.sendError.set(toAppError(error).message);
      },
    });
  }
}
