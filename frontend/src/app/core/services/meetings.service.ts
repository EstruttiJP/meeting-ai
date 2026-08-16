import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Meeting, MeetingType } from '../models/meeting.model';

@Injectable({ providedIn: 'root' })
export class MeetingsService {
  private readonly http = inject(HttpClient);

  list(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${environment.apiBaseUrl}/api/meetings`);
  }

  get(id: string): Observable<Meeting> {
    return this.http.get<Meeting>(`${environment.apiBaseUrl}/api/meetings/${id}`);
  }

  /**
   * URL do áudio original para o elemento <audio>. É o próprio endpoint da API
   * (autenticado por cookie de sessão), não a URL de storage — por isso a tag
   * precisa de crossorigin="use-credentials" para mandar o cookie.
   */
  audioUrl(id: string): string {
    return `${environment.apiBaseUrl}/api/meetings/${id}/audio`;
  }

  upload(title: string, file: File, meetingType: MeetingType): Observable<Meeting> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('file', file);
    formData.append('meetingType', meetingType);
    return this.http.post<Meeting>(`${environment.apiBaseUrl}/api/meetings`, formData);
  }
}
