export type CrmProvider = 'PIPEDRIVE';

export interface CrmConnection {
  id: string;
  provider: CrmProvider;
  connectedAt: string;
  tokenExpiresAt: string | null;
}

export interface CrmConnectionRequest {
  provider: CrmProvider;
  authorizationCode: string;
}
