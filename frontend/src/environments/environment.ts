export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080',
  pipedrive: {
    // Client ID público do app OAuth do Pipedrive (não é segredo — o client_secret
    // fica só no backend, usado na troca do código por token). Preencher em cada
    // ambiente real; sem valor, o botão de conectar CRM fica desabilitado.
    clientId: '',
    authorizeUrl: 'https://oauth.pipedrive.com/oauth/authorize',
    redirectUri: 'http://localhost:4200/settings',
  },
};
