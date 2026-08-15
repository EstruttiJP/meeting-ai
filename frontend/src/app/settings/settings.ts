import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Password } from 'primeng/password';
import { Select } from 'primeng/select';

import { environment } from '../../environments/environment';
import { toAppError } from '../core/http/to-app-error';
import { AiProvider, AiProviderConfig } from '../core/models/ai-provider-config.model';
import { CrmConnection } from '../core/models/crm-connection.model';
import { AiProviderConfigService } from '../core/services/ai-provider-config.service';
import { CrmConnectionService } from '../core/services/crm-connection.service';
import { ErrorState } from '../shared/ui/error-state/error-state';
import { LoadingState } from '../shared/ui/loading-state/loading-state';

const PROVIDER_OPTIONS: { label: string; value: AiProvider }[] = [
  { label: 'OpenRouter (plano free da aplicação)', value: 'OPENROUTER' },
  { label: 'OpenAI (GPT)', value: 'OPENAI' },
  { label: 'Google Gemini', value: 'GEMINI' },
  { label: 'Anthropic Claude', value: 'CLAUDE' },
];

const PROVIDER_LABELS: Record<AiProvider, string> = {
  OPENROUTER: 'OpenRouter',
  OPENAI: 'OpenAI (GPT)',
  GEMINI: 'Google Gemini',
  CLAUDE: 'Anthropic Claude',
};

@Component({
  selector: 'app-settings',
  imports: [FormsModule, Button, Select, Password, Checkbox, DatePipe, LoadingState, ErrorState],
  templateUrl: './settings.html',
  styleUrl: './settings.css',
})
export class Settings implements OnInit {
  private readonly crmConnectionService = inject(CrmConnectionService);
  private readonly aiProviderConfigService = inject(AiProviderConfigService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly providerOptions = PROVIDER_OPTIONS;
  protected readonly providerLabels = PROVIDER_LABELS;

  // Client id público do app Pipedrive — não configurado em dev por padrão
  // (ver .env.example). Sem ele o redirect cai numa página de erro do
  // Pipedrive, então escondemos a ação em vez de deixar o usuário tentar.
  protected readonly pipedriveConfigured = !!environment.pipedrive.clientId;

  // --- CRM (Pipedrive) ---
  protected readonly crmLoading = signal(true);
  protected readonly crmError = signal<string | null>(null);
  protected readonly crmConnection = signal<CrmConnection | null>(null);
  protected readonly crmConnecting = signal(false);
  protected readonly crmDisconnecting = signal(false);

  // --- Provider de IA ---
  protected readonly configs = signal<AiProviderConfig[]>([]);
  protected readonly configsLoading = signal(true);
  protected readonly configsError = signal<string | null>(null);

  protected selectedProvider: AiProvider = 'OPENAI';
  protected apiKeyDraft = '';
  protected isDefaultDraft = false;
  protected readonly saving = signal(false);
  protected readonly saveError = signal<string | null>(null);
  protected readonly deletingId = signal<string | null>(null);

  protected readonly canSave = computed(() => !this.saving());

  ngOnInit() {
    const authorizationCode = this.route.snapshot.queryParamMap.get('code');
    if (authorizationCode) {
      this.completePipedriveOAuth(authorizationCode);
    } else {
      this.loadCrmConnection();
    }
    this.loadProviderConfigs();
  }

  protected loadCrmConnection() {
    this.crmLoading.set(true);
    this.crmError.set(null);
    this.crmConnectionService.me().subscribe({
      next: (connection) => {
        this.crmConnection.set(connection);
        this.crmLoading.set(false);
      },
      error: (error) => {
        this.crmLoading.set(false);
        if (error instanceof HttpErrorResponse && error.status === 404) {
          this.crmConnection.set(null);
          return;
        }
        this.crmError.set(toAppError(error).message);
      },
    });
  }

  // Sai da SPA de propósito — o fluxo OAuth do Pipedrive precisa acontecer no
  // domínio deles. A volta (com ?code= na URL) é tratada no ngOnInit.
  protected connectCrm() {
    window.location.href = this.buildPipedriveAuthorizeUrl();
  }

  protected buildPipedriveAuthorizeUrl(): string {
    const params = new URLSearchParams({
      client_id: environment.pipedrive.clientId,
      redirect_uri: environment.pipedrive.redirectUri,
    });
    return `${environment.pipedrive.authorizeUrl}?${params.toString()}`;
  }

  private completePipedriveOAuth(authorizationCode: string) {
    this.crmConnecting.set(true);
    this.crmLoading.set(false);
    this.crmError.set(null);
    this.crmConnectionService.connect({ provider: 'PIPEDRIVE', authorizationCode }).subscribe({
      next: (connection) => {
        this.crmConnection.set(connection);
        this.crmConnecting.set(false);
        // Tira o ?code= da URL pra um refresh não tentar trocar o mesmo código de novo.
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      },
      error: (error) => {
        this.crmError.set(toAppError(error).message);
        this.crmConnecting.set(false);
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      },
    });
  }

  protected disconnectCrm() {
    this.crmDisconnecting.set(true);
    this.crmConnectionService.disconnect().subscribe({
      next: () => {
        this.crmConnection.set(null);
        this.crmDisconnecting.set(false);
      },
      error: (error) => {
        this.crmError.set(toAppError(error).message);
        this.crmDisconnecting.set(false);
      },
    });
  }

  protected loadProviderConfigs() {
    this.configsLoading.set(true);
    this.configsError.set(null);
    this.aiProviderConfigService.list().subscribe({
      next: (configs) => {
        this.configs.set(configs);
        this.configsLoading.set(false);
      },
      error: (error) => {
        this.configsError.set(toAppError(error).message);
        this.configsLoading.set(false);
      },
    });
  }

  protected saveProviderConfig() {
    this.saving.set(true);
    this.saveError.set(null);
    this.aiProviderConfigService
      .save({ provider: this.selectedProvider, apiKey: this.apiKeyDraft || undefined, isDefault: this.isDefaultDraft })
      .subscribe({
        next: () => {
          // A chave nunca fica em memória depois de salva — só o hasApiKey volta da API.
          this.apiKeyDraft = '';
          this.isDefaultDraft = false;
          this.saving.set(false);
          this.loadProviderConfigs();
        },
        error: (error) => {
          this.saveError.set(toAppError(error).message);
          this.saving.set(false);
        },
      });
  }

  protected deleteProviderConfig(id: string) {
    this.deletingId.set(id);
    this.aiProviderConfigService.delete(id).subscribe({
      next: () => {
        this.deletingId.set(null);
        this.loadProviderConfigs();
      },
      error: (error) => {
        this.configsError.set(toAppError(error).message);
        this.deletingId.set(null);
      },
    });
  }
}
