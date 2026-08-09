package com.meetingai.backend.transcription;

import java.io.InputStream;

/**
 * Abstrai qual serviço transcreve o áudio da reunião. Local (Whisper via
 * container) em dev, Amazon Transcribe na etapa de deploy — troca só por
 * configuração, sem mudar quem chama.
 */
public interface TranscriptionProvider {

	TranscriptionResult transcribe(InputStream audioContent, String filename, String contentType);

}
