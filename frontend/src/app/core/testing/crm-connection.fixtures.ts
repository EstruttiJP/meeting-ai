import { CrmConnection } from '../models/crm-connection.model';

// Wrapper mutável (não um `let` exportado) porque outros módulos precisam
// atualizar esse estado — bindings de import são somente leitura, propriedades
// de um objeto não. Começa desconectado pra demonstrar o fluxo de conexão.
export const MOCK_CRM_STATE: { connection: CrmConnection | null } = {
  connection: null,
};
