import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Button } from 'primeng/button';

import { toAppError } from '../core/http/to-app-error';
import { Meeting } from '../core/models/meeting.model';
import { Summary, SummaryContent } from '../core/models/summary.model';
import { MeetingsService } from '../core/services/meetings.service';
import { SummaryService } from '../core/services/summary.service';
import { ErrorState } from '../shared/ui/error-state/error-state';
import { LoadingState } from '../shared/ui/loading-state/loading-state';
import { EditableList } from './editable-list/editable-list';

function cloneContent(content: SummaryContent): SummaryContent {
  return {
    summary: content.summary,
    decisions: [...(content.decisions ?? [])],
    nextSteps: [...(content.nextSteps ?? [])],
    mentionedValues: [...(content.mentionedValues ?? [])],
    paymentMethod: content.paymentMethod ?? '',
    objections: [...(content.objections ?? [])],
  };
}

@Component({
  selector: 'app-review',
  imports: [FormsModule, Button, DatePipe, LoadingState, ErrorState, EditableList],
  templateUrl: './review.html',
  styleUrl: './review.css',
})
export class Review implements OnInit {
  private readonly meetingsService = inject(MeetingsService);
  private readonly summaryService = inject(SummaryService);

  readonly id = input.required<string>();

  protected readonly meeting = signal<Meeting | null>(null);
  protected readonly summary = signal<Summary | null>(null);
  protected readonly draft = signal<SummaryContent | null>(null);

  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly approving = signal(false);
  protected readonly approveError = signal<string | null>(null);

  protected readonly sendingToCrm = signal(false);
  protected readonly sendError = signal<string | null>(null);

  protected readonly alreadySentToCrm = computed(() => this.meeting()?.status === 'SENT_TO_CRM');
  protected readonly canApprove = computed(() => !!this.draft()?.summary.trim() && !this.approving());

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
        this.draft.set(cloneContent(summary.content));
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(toAppError(error).message);
        this.loading.set(false);
      },
    });
  }

  protected updateDraft(patch: Partial<SummaryContent>) {
    const current = this.draft();
    if (current) {
      this.draft.set({ ...current, ...patch });
    }
  }

  protected approveAndSave() {
    const content = this.draft();
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
