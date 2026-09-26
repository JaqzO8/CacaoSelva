-- Columna de versión para concurrencia optimista (Fase 4)
ALTER TABLE lotes ADD COLUMN version INTEGER NOT NULL DEFAULT 1;
