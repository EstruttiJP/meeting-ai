-- Tipo escolhido pela usuária antes do upload. Muda a ênfase do prompt de
-- extração (não a estrutura dos itens) e fica gravado para servir de filtro
-- e contexto depois.
--
-- NOT NULL sem backfill: as reuniões de teste anteriores foram apagadas, então
-- não existe linha antiga para adivinhar tipo. O default cobre só a janela
-- entre a migration rodar e o backend novo subir; toda reunião nova chega com
-- tipo escolhido de verdade.
ALTER TABLE meeting ADD COLUMN meeting_type VARCHAR(30) NOT NULL DEFAULT 'GENERICA';
