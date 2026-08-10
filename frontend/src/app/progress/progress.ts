import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import { toAppError } from '../core/http/to-app-error';
import { Meeting, MeetingStatus } from '../core/models/meeting.model';
import { MeetingsService } from '../core/services/meetings.service';
import { ErrorState } from '../shared/ui/error-state/error-state';
import { LoadingState } from '../shared/ui/loading-state/loading-state';

const POLL_INTERVAL_MS = 2500;

const PIPELINE_STEPS: { status: MeetingStatus; label: string }[] = [
  { status: 'UPLOADED', label: 'Enviada' },
  { status: 'TRANSCRIBING', label: 'Transcrevendo' },
  { status: 'SUMMARIZING', label: 'Gerando resumo' },
  { status: 'READY', label: 'Pronta' },
];

const TERMINAL_STATUSES: MeetingStatus[] = ['READY', 'SENT_TO_CRM', 'FAILED', 'EXPIRED'];

@Component({
  selector: 'app-progress',
  imports: [Button, RouterLink, LoadingState, ErrorState],
  templateUrl: './progress.html',
  styleUrl: './progress.css',
})
export class Progress implements OnInit {
  private readonly meetingsService = inject(MeetingsService);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();

  protected readonly meeting = signal<Meeting | null>(null);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  protected readonly steps = PIPELINE_STEPS;

  protected readonly currentStepIndex = computed(() => {
    const meeting = this.meeting();
    if (!meeting) {
      return -1;
    }
    return PIPELINE_STEPS.findIndex((step) => step.status === meeting.status);
  });

  private pollHandle: ReturnType<typeof setInterval> | undefined;

  ngOnInit() {
    // Polling simples via setInterval — o pipeline roda em background no backend
    // (ver MeetingPipelineService) e essa tela só pergunta o status de tempos em
    // tempos, sem WebSocket. O handle é criado antes da primeira chamada pra
    // que um retorno síncrono (ou muito rápido) já encontre algo pra cancelar.
    this.pollHandle = setInterval(() => this.poll(), POLL_INTERVAL_MS);
    this.destroyRef.onDestroy(() => clearInterval(this.pollHandle));
    this.poll();
  }

  protected retry() {
    this.loading.set(true);
    this.poll();
  }

  private poll() {
    this.meetingsService.get(this.id()).subscribe({
      next: (meeting) => {
        this.meeting.set(meeting);
        this.loading.set(false);
        this.errorMessage.set(null);
        if (TERMINAL_STATUSES.includes(meeting.status)) {
          clearInterval(this.pollHandle);
        }
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(toAppError(error).message);
        clearInterval(this.pollHandle);
      },
    });
  }
}
