export type AiProvider = 'OPENROUTER' | 'OPENAI' | 'GEMINI' | 'CLAUDE';

export interface AiProviderConfig {
  id: string;
  provider: AiProvider;
  hasApiKey: boolean;
  isDefault: boolean;
  createdAt: string;
}

export interface AiProviderConfigRequest {
  provider: AiProvider;
  // Vazio/omitido = mantém sem chave própria (cai no OpenRouter free da aplicação).
  apiKey?: string;
  isDefault: boolean;
}
