import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, throwError } from 'rxjs';

import { AiProviderConfig, AiProviderConfigRequest } from '../core/models/ai-provider-config.model';
import { CrmConnection, CrmConnectionRequest } from '../core/models/crm-connection.model';
import { AiProviderConfigService } from '../core/services/ai-provider-config.service';
import { CrmConnectionService } from '../core/services/crm-connection.service';
import { Settings } from './settings';

describe('Settings', () => {
  let fixture: ComponentFixture<Settings>;
  let crmServiceMock: {
    me: () => Observable<CrmConnection>;
    connect: (request: CrmConnectionRequest) => Observable<CrmConnection>;
    disconnect: () => Observable<void>;
  };
  let providerServiceMock: {
    list: () => Observable<AiProviderConfig[]>;
    save: (request: AiProviderConfigRequest) => Observable<AiProviderConfig>;
    delete: (id: string) => Observable<void>;
  };

  const buildFixture = async () => {
    await TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        { provide: CrmConnectionService, useValue: crmServiceMock },
        { provide: AiProviderConfigService, useValue: providerServiceMock },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Settings);
    fixture.detectChanges();
  };

  beforeEach(() => {
    crmServiceMock = {
      me: () => throwError(() => new HttpErrorResponse({ status: 404 })),
      connect: (request) => of({ id: 'crm-1', provider: request.provider, connectedAt: new Date().toISOString(), tokenExpiresAt: null }),
      disconnect: () => of(undefined),
    };
    providerServiceMock = {
      list: () =>
        of([{ id: 'p1', provider: 'OPENROUTER', hasApiKey: false, isDefault: true, createdAt: new Date().toISOString() }]),
      save: (request) =>
        of({
          id: 'p2',
          provider: request.provider,
          hasApiKey: !!request.apiKey,
          isDefault: request.isDefault,
          createdAt: new Date().toISOString(),
        }),
      delete: () => of(undefined),
    };
  });

  it('treats a 404 from the CRM connection check as "not connected", not an error', async () => {
    await buildFixture();
    const instance = fixture.componentInstance;

    expect(instance['crmError']()).toBeNull();
    expect(instance['crmConnection']()).toBeNull();
  });

  it('shows a real error banner when the CRM check fails for another reason', async () => {
    crmServiceMock.me = () => throwError(() => new HttpErrorResponse({ status: 500 }));
    await buildFixture();
    const instance = fixture.componentInstance;

    expect(instance['crmError']()).toBeTruthy();
  });

  it('connects the CRM and reflects the new connection', async () => {
    await buildFixture();
    const instance = fixture.componentInstance;

    instance['connectCrm']();

    expect(instance['crmConnection']()?.provider).toBe('PIPEDRIVE');
    expect(instance['crmConnecting']()).toBe(false);
  });

  it('clears the typed API key after a successful save, never re-displaying it', async () => {
    await buildFixture();
    const instance = fixture.componentInstance;
    instance['apiKeyDraft'] = 'sk-super-secret';
    instance['selectedProvider'] = 'OPENAI';

    instance['saveProviderConfig']();

    expect(instance['apiKeyDraft']).toBe('');
    expect(instance['saving']()).toBe(false);
  });

  it('surfaces a save error and keeps the draft so the user can retry', async () => {
    providerServiceMock.save = () => throwError(() => new HttpErrorResponse({ status: 400, error: { detail: 'Chave inválida' } }));
    await buildFixture();
    const instance = fixture.componentInstance;
    instance['apiKeyDraft'] = 'sk-invalid';

    instance['saveProviderConfig']();

    expect(instance['saveError']()).toBe('Chave inválida');
    expect(instance['apiKeyDraft']).toBe('sk-invalid');
  });
});
