import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Observable, of } from 'rxjs';

import { Meeting, MeetingType } from '../core/models/meeting.model';
import { UsageQuota } from '../core/models/usage-quota.model';
import { MeetingsService } from '../core/services/meetings.service';
import { UsageQuotaService } from '../core/services/usage-quota.service';
import { Upload } from './upload';

describe('Upload', () => {
  let fixture: ComponentFixture<Upload>;
  let router: Router;
  let meetingsServiceMock: {
    upload: (title: string, file: File, meetingType: MeetingType) => Observable<Meeting>;
  };
  let usageQuotaServiceMock: { me: () => Observable<UsageQuota> };

  const buildFile = (name: string) => new File(['conteudo'], name, { type: 'audio/mpeg' });

  // Muta os métodos em vez de trocar o objeto — o TestBed já capturou a
  // referência via useValue, reatribuir a variável não alcançaria a DI.
  const setup = (quota: UsageQuota) => {
    usageQuotaServiceMock.me = () => of(quota);
  };

  beforeEach(async () => {
    usageQuotaServiceMock = {
      me: () => of({ id: 'q1', monthReference: '2026-08', meetingsUploaded: 2, meetingsLimit: 10 }),
    };
    meetingsServiceMock = {
      upload: (title, file, meetingType) =>
        of({
          id: 'new-meeting',
          title,
          originalFilename: file.name,
          status: 'UPLOADED',
          uploadedAt: new Date().toISOString(),
          expiresAt: null,
          sentToCrmAt: null,
          meetingType,
          failureCategory: null,
        }),
    };

    await TestBed.configureTestingModule({
      imports: [Upload],
      providers: [
        provideRouter([]),
        { provide: UsageQuotaService, useValue: usageQuotaServiceMock },
        { provide: MeetingsService, useValue: meetingsServiceMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Upload);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('rejects files with an unsupported extension', () => {
    const instance = fixture.componentInstance;
    instance['meetingType'].set('GENERICA');
    instance['onFileInputChange']({ target: { files: [buildFile('reuniao.pdf')] } } as unknown as Event);

    expect(instance['fileError']()).toContain('Formato não suportado');
    expect(instance['selectedFile']()).toBeNull();
  });

  it('accepts a supported file and fills the title from the filename', () => {
    const instance = fixture.componentInstance;
    instance['meetingType'].set('GENERICA');
    instance['onFileInputChange']({ target: { files: [buildFile('kickoff-cliente.mp3')] } } as unknown as Event);

    expect(instance['fileError']()).toBeNull();
    expect(instance['selectedFile']()?.name).toBe('kickoff-cliente.mp3');
    expect(instance['title']).toBe('kickoff-cliente');
  });

  it('disables submit while the monthly quota is exhausted', () => {
    setup({ id: 'q1', monthReference: '2026-08', meetingsUploaded: 10, meetingsLimit: 10 });
    fixture = TestBed.createComponent(Upload);
    fixture.detectChanges();
    const instance = fixture.componentInstance;

    instance['meetingType'].set('GENERICA');
    instance['onFileInputChange']({ target: { files: [buildFile('audio.mp3')] } } as unknown as Event);
    instance['title'] = 'Reunião de teste';

    expect(instance['quotaExhausted']()).toBe(true);
    expect(instance['canSubmit']()).toBe(false);
  });

  it('uploads the meeting and navigates to its progress screen', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');
    const instance = fixture.componentInstance;

    instance['meetingType'].set('DAILY');
    instance['onFileInputChange']({ target: { files: [buildFile('audio.mp3')] } } as unknown as Event);
    instance['title'] = 'Reunião de teste';
    instance['submit']();

    expect(navigateSpy).toHaveBeenCalledWith(['/meetings', 'new-meeting', 'progress']);
  });

  it('keeps the file picker closed until a meeting type is chosen', () => {
    const instance = fixture.componentInstance;
    expect(instance['canPickFile']()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain('Escolha o tipo da reunião');

    instance['meetingType'].set('FECHAMENTO');
    fixture.detectChanges();

    expect(instance['canPickFile']()).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('Arraste o arquivo aqui');
  });

  it('ignores a dropped file while no type is chosen', () => {
    const instance = fixture.componentInstance;
    instance['onDrop']({
      preventDefault: () => undefined,
      dataTransfer: { files: [buildFile('audio.mp3')] },
    } as unknown as DragEvent);

    expect(instance['selectedFile']()).toBeNull();
  });

  it('sends the chosen meeting type along with the file', () => {
    const instance = fixture.componentInstance;
    const uploadSpy = vi.spyOn(meetingsServiceMock, 'upload');

    instance['meetingType'].set('FECHAMENTO');
    instance['onFileInputChange']({ target: { files: [buildFile('audio.mp3')] } } as unknown as Event);
    instance['title'] = 'Fechamento com o cliente';
    instance['submit']();

    expect(uploadSpy).toHaveBeenCalledWith('Fechamento com o cliente', expect.any(File), 'FECHAMENTO');
  });

  it('does not allow submitting without a meeting type', () => {
    const instance = fixture.componentInstance;
    instance['meetingType'].set('GENERICA');
    instance['onFileInputChange']({ target: { files: [buildFile('audio.mp3')] } } as unknown as Event);
    instance['title'] = 'Reunião de teste';
    expect(instance['canSubmit']()).toBe(true);

    instance['meetingType'].set(null);
    expect(instance['canSubmit']()).toBe(false);
  });
});
