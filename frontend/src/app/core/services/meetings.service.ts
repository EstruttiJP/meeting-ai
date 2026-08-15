import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Meeting } from '../models/meeting.model';

@Injectable({ providedIn: 'root' })
export class MeetingsService {
  private readonly http = inject(HttpClient);

  list(): Observable<Meeting[]> {
    return this.http.get<Meeting[]>(`${environment.apiBaseUrl}/api/meetings`);
  }

  get(id: string): Observable<Meeting> {
    return this.http.get<Meeting>(`${environment.apiBaseUrl}/api/meetings/${id}`);
  }

  upload(title: string, file: File): Observable<Meeting> {
    const formData = new FormData();
    formData.append('title', title);
    formData.append('file', file);
    return this.http.post<Meeting>(`${environment.apiBaseUrl}/api/meetings`, formData);
  }
}
