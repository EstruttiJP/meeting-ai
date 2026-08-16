import { Component, ElementRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Button } from 'primeng/button';
import { ProgressBar } from 'primeng/progressbar';

import { toAppError } from '../core/http/to-app-error';
import { MEETING_TYPE_OPTIONS, MeetingType } from '../core/models/meeting.model';
import { UsageQuota } from '../core/models/usage-quota.model';
import { MeetingsService } from '../core/services/meetings.service';
import { UsageQuotaService } from '../core/services/usage-quota.service';
import { ErrorState } from '../shared/ui/error-state/error-state';
import { LoadingState } from '../shared/ui/loading-state/loading-state';

const ACCEPTED_EXTENSIONS = ['.mp3', '.mp4', '.wav', '.m4a'];

@Component({
  selector: 'app-upload',
  imports: [FormsModule, Button, ProgressBar, LoadingState, ErrorState],
  templateUrl: './upload.html',
  styleUrl: './upload.css',
})
export class Upload {
  private readonly meetingsService = inject(MeetingsService);
  private readonly usageQuotaService = inject(UsageQuotaService);
  private readonly router = inject(Router);

  private readonly fileInput = viewChild.required<ElementRef<HTMLInputElement>>('fileInput');

  protected readonly quota = signal<UsageQuota | null>(null);
  protected readonly quotaLoading = signal(true);
  protected readonly quotaError = signal<string | null>(null);

  protected readonly quotaExhausted = computed(() => {
    const q = this.quota();
    return q !== null && q.meetingsUploaded >= q.meetingsLimit;
  });

  protected readonly quotaPercent = computed(() => {
    const q = this.quota();
    if (!q || q.meetingsLimit === 0) {
      return 0;
    }
    return Math.min(100, Math.round((q.meetingsUploaded / q.meetingsLimit) * 100));
  });

  /**
   * Escolhido antes de anexar o arquivo: o tipo muda a ênfase da extração, e
   * pedir depois do upload seria tarde — o pipeline já teria começado.
   */
  protected readonly meetingType = signal<MeetingType | null>(null);
  protected readonly typeOptions = MEETING_TYPE_OPTIONS;

  protected readonly isDraggingOver = signal(false);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly fileError = signal<string | null>(null);
  protected title = '';

  protected readonly submitting = signal(false);
  protected readonly submitError = signal<string | null>(null);

  /** Sem tipo escolhido não dá para anexar: a ordem faz parte do fluxo pedido. */
  protected readonly canPickFile = computed(() => this.meetingType() !== null && !this.quotaExhausted());

  protected readonly canSubmit = computed(
    () =>
      !!this.selectedFile() &&
      this.meetingType() !== null &&
      this.title.trim().length > 0 &&
      !this.quotaExhausted() &&
      !this.submitting(),
  );

  constructor() {
    this.loadQuota();
  }

  protected loadQuota() {
    this.quotaLoading.set(true);
    this.quotaError.set(null);
    this.usageQuotaService.me().subscribe({
      next: (quota) => {
        this.quota.set(quota);
        this.quotaLoading.set(false);
      },
      error: (error) => {
        this.quotaError.set(toAppError(error).message);
        this.quotaLoading.set(false);
      },
    });
  }

  protected onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDraggingOver.set(true);
  }

  protected onDragLeave() {
    this.isDraggingOver.set(false);
  }

  protected onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDraggingOver.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (file && this.canPickFile()) {
      this.handleFile(file);
    }
  }

  protected onFileInputChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) {
      this.handleFile(file);
    }
  }

  protected browseFiles() {
    if (!this.canPickFile()) {
      return;
    }
    this.fileInput().nativeElement.click();
  }

  protected clearFile() {
    this.selectedFile.set(null);
    this.fileError.set(null);
    this.fileInput().nativeElement.value = '';
  }

  private handleFile(file: File) {
    const extension = file.name.slice(file.name.lastIndexOf('.')).toLowerCase();
    if (!ACCEPTED_EXTENSIONS.includes(extension)) {
      this.fileError.set(`Formato não suportado. Use: ${ACCEPTED_EXTENSIONS.join(', ')}.`);
      this.selectedFile.set(null);
      return;
    }
    this.fileError.set(null);
    this.selectedFile.set(file);
    if (!this.title.trim()) {
      this.title = file.name.replace(extension, '');
    }
  }

  protected submit() {
    const file = this.selectedFile();
    if (!file || !this.canSubmit()) {
      return;
    }
    this.submitting.set(true);
    this.submitError.set(null);
    this.meetingsService.upload(this.title.trim(), file, this.meetingType()!).subscribe({
      next: (meeting) => {
        this.submitting.set(false);
        this.router.navigate(['/meetings', meeting.id, 'progress']);
      },
      error: (error) => {
        this.submitting.set(false);
        this.submitError.set(toAppError(error).message);
      },
    });
  }
}
