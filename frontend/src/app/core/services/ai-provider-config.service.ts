import { Injectable } from '@angular/core';
import { Observable, delay, of } from 'rxjs';

import { AiProviderConfig, AiProviderConfigRequest } from '../models/ai-provider-config.model';
import { MOCK_AI_PROVIDER_CONFIGS } from '../testing/ai-provider-config.fixtures';

// Mock temporário: lê/escreve a fixture local em vez de chamar
// GET/POST/DELETE /api/ai-provider-configs. A chave em si nunca é guardada
// aqui, igual no backend real — só o hasApiKey derivado dela.
@Injectable({ providedIn: 'root' })
export class AiProviderConfigService {
  list(): Observable<AiProviderConfig[]> {
    // Cópia rasa: a fixture é mutada em save()/delete() e um array com a
    // mesma referência não dispara um signal.set() (ver o mesmo problema
    // resolvido em MeetingsService.get()).
    return of([...MOCK_AI_PROVIDER_CONFIGS]).pipe(delay(350));
  }

  save(request: AiProviderConfigRequest): Observable<AiProviderConfig> {
    if (request.isDefault) {
      MOCK_AI_PROVIDER_CONFIGS.forEach((config) => (config.isDefault = false));
    }
    const existingIndex = MOCK_AI_PROVIDER_CONFIGS.findIndex((c) => c.provider === request.provider);
    const config: AiProviderConfig = {
      id: existingIndex >= 0 ? MOCK_AI_PROVIDER_CONFIGS[existingIndex].id : `provider-${request.provider.toLowerCase()}`,
      provider: request.provider,
      hasApiKey: !!request.apiKey?.trim(),
      isDefault: request.isDefault,
      createdAt: existingIndex >= 0 ? MOCK_AI_PROVIDER_CONFIGS[existingIndex].createdAt : new Date().toISOString(),
    };
    if (existingIndex >= 0) {
      MOCK_AI_PROVIDER_CONFIGS[existingIndex] = config;
    } else {
      MOCK_AI_PROVIDER_CONFIGS.push(config);
    }
    return of(config).pipe(delay(400));
  }

  delete(id: string): Observable<void> {
    const index = MOCK_AI_PROVIDER_CONFIGS.findIndex((c) => c.id === id);
    if (index >= 0) {
      MOCK_AI_PROVIDER_CONFIGS.splice(index, 1);
    }
    return of(undefined).pipe(delay(300));
  }
}
