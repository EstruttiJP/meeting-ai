package com.meetingai.backend.meeting;

/**
 * Tipo da reunião, escolhido no upload. Não muda a estrutura do resumo — os
 * quatro tipos de item continuam os mesmos — só a ênfase do que o modelo deve
 * priorizar extrair: caçar valores numa daily rende ruído, enquanto num
 * fechamento é o que mais importa.
 *
 * <p>Fica gravado na reunião também para servir de filtro/contexto no futuro.
 */
public enum MeetingType {
	FECHAMENTO,
	DAILY,
	APRESENTACAO,
	GENERICA
}
