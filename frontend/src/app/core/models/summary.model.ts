/**
 * Tipo do item extraído. Os rótulos são neutros de propósito: o produto é um
 * facilitador de reunião, e "ponto de atenção" cobre o que antes se chamava
 * "objeção" sem assumir que toda reunião é uma venda.
 */
export type SummaryItemType = 'decisao' | 'proximo_passo' | 'valor_mencionado' | 'ponto_atencao';

/** Só decisão e próximo passo têm prioridade; nos outros tipos vem sempre null. */
export type SummaryItemPriority = 'alta' | 'normal';

export interface SummaryItem {
  id: string;
  type: SummaryItemType;
  content: string;
  /** Ponto da gravação em que o item foi dito. Nulo quando não foi possível ancorar. */
  timestampSeconds: number | null;
  priority: SummaryItemPriority | null;
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

/** Título de cada grupo na aba Resumo, no plural. */
export const SUMMARY_ITEM_GROUP_LABEL: Record<SummaryItemType, string> = {
  decisao: 'Decisões',
  proximo_passo: 'Próximos passos',
  ponto_atencao: 'Pontos de atenção',
  valor_mencionado: 'Valores mencionados',
};

/**
 * Ordem dos grupos na aba Resumo: o que move o trabalho primeiro, o ruído por
 * último. Valores mencionados fecham a lista de propósito — costumam ser muitos
 * e com pouco peso.
 */
export const SUMMARY_GROUP_ORDER: SummaryItemType[] = [
  'decisao',
  'proximo_passo',
  'ponto_atencao',
  'valor_mencionado',
];
