export const environment = {
  production: true,
  // Vazio = mesma origem do frontend (cenário de deploy da Etapa 4, atrás do
  // mesmo domínio/CloudFront). Ajustar aqui se o backend ficar em domínio próprio.
  apiBaseUrl: '',
  pipedrive: {
    clientId: '',
    authorizeUrl: 'https://oauth.pipedrive.com/oauth/authorize',
    redirectUri: '/settings',
  },
};
