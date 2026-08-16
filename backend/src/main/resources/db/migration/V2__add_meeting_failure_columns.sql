-- Motivo técnico da falha do pipeline (transcrição/resumo). Sem essas colunas
-- uma reunião em FAILED não guardava nenhum rastro da causa, e o diagnóstico
-- dependia de achar o stacktrace no log do backend antes dele rotacionar.
ALTER TABLE meeting ADD COLUMN failure_category VARCHAR(30);
ALTER TABLE meeting ADD COLUMN failure_reason TEXT;
