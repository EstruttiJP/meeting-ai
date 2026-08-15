import { AiProviderConfig } from '../models/ai-provider-config.model';

// Estado inicial: nenhuma chave própria configurada, o app usa o OpenRouter
// (plano free) como default — mesmo comportamento do backend sem configuração.
export const MOCK_AI_PROVIDER_CONFIGS: AiProviderConfig[] = [
  {
    id: 'provider-openrouter',
    provider: 'OPENROUTER',
    hasApiKey: false,
    isDefault: true,
    createdAt: '2026-08-01T10:00:00Z',
  },
];
