import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Button } from 'primeng/button';

import { toAppError } from '../core/http/to-app-error';
import { Meeting } from '../core/models/meeting.model';
import { MeetingsService } from '../core/services/meetings.service';
import { ErrorState } from '../shared/ui/error-state/error-state';
import { LoadingState } from '../shared/ui/loading-state/loading-state';
import { MeetingStatusBadge } from '../shared/ui/meeting-status-badge/meeting-status-badge';

interface MeetingAction {
  label: string;
  link: string[];
}

@Component({
  selector: 'app-dashboard',
  imports: [Button, RouterLink, DatePipe, LoadingState, ErrorState, MeetingStatusBadge],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private readonly meetingsService = inject(MeetingsService);

  protected readonly meetings = signal<Meeting[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    this.load();
  }

  protected load() {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.meetingsService.list().subscribe({
      next: (meetings) => {
        this.meetings.set(meetings);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(toAppError(error).message);
        this.loading.set(false);
      },
    });
  }

  protected actionFor(meeting: Meeting): MeetingAction | null {
    switch (meeting.status) {
      case 'READY':
      case 'SENT_TO_CRM':
        return { label: 'Ver resumo', link: ['/meetings', meeting.id, 'review'] };
      case 'UPLOADED':
      case 'TRANSCRIBING':
      case 'SUMMARIZING':
        return { label: 'Ver progresso', link: ['/meetings', meeting.id, 'progress'] };
      default:
        return null;
    }
  }
}
