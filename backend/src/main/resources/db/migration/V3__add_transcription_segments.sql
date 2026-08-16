-- O Whisper já devolve a transcrição fatiada em segmentos com tempo de início
-- e fim, mas até aqui só o texto concatenado era guardado. Sem os segmentos não
-- há como ancorar cada item do resumo num ponto da gravação — que é o que
-- permite destacar o item enquanto o áudio toca.
--
-- Fica NULL nas transcrições antigas de propósito: são anteriores a este
-- recurso e não têm como ser reconstruídas sem reprocessar o áudio.
ALTER TABLE transcription ADD COLUMN segments JSONB;
