/**
 * Tipo do item extraído. Os rótulos são neutros de propósito: o produto é um
 * facilitador de reunião, e "ponto de atenção" cobre o que antes se chamava
 * "objeção" sem assumir que toda reunião é uma venda.
 */
export type SummaryItemType = 'decisao' | 'proximo_passo' | 'valor_mencionado' | 'ponto_atencao';

export interface SummaryItem {
  id: string;
  type: SummaryItemType;
  content: string;
  /** Ponto da gravação em que o item foi dito. Nulo quando não foi possível ancorar. */
  timestampSeconds: number | null;
}

export interface SummaryContent {
  summary: string;
  items: SummaryItem[];
}

export interface Summary {
  id: string;
  meetingId: string;
  content: SummaryContent;
  approved: boolean;
  approvedAt: string | null;
}

export const SUMMARY_ITEM_TYPE_LABEL: Record<SummaryItemType, string> = {
  decisao: 'Decisão',
  proximo_passo: 'Próximo passo',
  valor_mencionado: 'Valor mencionado',
  ponto_atencao: 'Ponto de atenção',
};
