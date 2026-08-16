import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
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
  let queryParams: Record<string, string>;

  const buildFixture = async (skipInitialDetectChanges = false) => {
    await TestBed.configureTestingModule({
      imports: [Settings],
      providers: [
        provideRouter([]),
        { provide: CrmConnectionService, useValue: crmServiceMock },
        { provide: AiProviderConfigService, useValue: providerServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(queryParams) } },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Settings);
    if (!skipInitialDetectChanges) {
      fixture.detectChanges();
    }
  };

  beforeEach(() => {
    queryParams = {};
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

  it('builds the Pipedrive authorize URL from the environment config', async () => {
    await buildFixture();
    const url = fixture.componentInstance['buildPipedriveAuthorizeUrl']();

    expect(url).toContain('https://oauth.pipedrive.com/oauth/authorize');
    expect(url).toContain('redirect_uri=');
  });

  it('completes the OAuth flow when returning with a ?code= query param, then strips it from the URL', async () => {
    queryParams = { code: 'abc123' };
    const connectSpy = vi.fn(crmServiceMock.connect);
    crmServiceMock.connect = connectSpy;
    await buildFixture(true);
    const navigateSpy = vi.spyOn(TestBed.inject(Router), 'navigate');
    fixture.detectChanges();

    expect(connectSpy).toHaveBeenCalledWith({ provider: 'PIPEDRIVE', authorizationCode: 'abc123' });
    expect(fixture.componentInstance['crmConnection']()?.provider).toBe('PIPEDRIVE');
    expect(navigateSpy).toHaveBeenCalled();
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
