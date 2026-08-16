package com.meetingai.backend.transcription;

/**
 * Um trecho da transcrição com o ponto da gravação em que foi dito. É o que
 * permite ancorar cada item do resumo num instante do áudio — sem isso o
 * player não teria como destacar o item no momento certo.
 */
public record TranscriptionSegment(double startSeconds, double endSeconds, String text) {
}
