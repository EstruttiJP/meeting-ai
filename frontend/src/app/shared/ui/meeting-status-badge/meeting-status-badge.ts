import { Component, computed, input } from '@angular/core';
import { Tag } from 'primeng/tag';

import { MeetingStatus } from '../../../core/models/meeting.model';

type TagSeverity = 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast';

const STATUS_LABEL: Record<MeetingStatus, string> = {
  UPLOADED: 'Enviada',
  TRANSCRIBING: 'Transcrevendo',
  SUMMARIZING: 'Resumindo',
  READY: 'Pronta para revisão',
  SENT_TO_CRM: 'Enviada ao CRM',
  EXPIRED: 'Expirada',
  FAILED: 'Falhou',
};

const STATUS_SEVERITY: Record<MeetingStatus, TagSeverity> = {
  UPLOADED: 'info',
  TRANSCRIBING: 'info',
  SUMMARIZING: 'info',
  READY: 'success',
  SENT_TO_CRM: 'success',
  EXPIRED: 'secondary',
  FAILED: 'danger',
};

@Component({
  selector: 'app-meeting-status-badge',
  imports: [Tag],
  templateUrl: './meeting-status-badge.html',
  styleUrl: './meeting-status-badge.css',
})
export class MeetingStatusBadge {
  readonly status = input.required<MeetingStatus>();

  protected readonly label = computed(() => STATUS_LABEL[this.status()]);
  protected readonly severity = computed(() => STATUS_SEVERITY[this.status()]);
}
