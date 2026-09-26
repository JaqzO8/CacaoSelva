-- 1. Crear socios a partir de los nombres únicos existentes (sin DNI real, se genera uno temporal)
INSERT INTO socios (dni, nombre)
SELECT LPAD(ROW_NUMBER() OVER (ORDER BY socio)::TEXT, 8, '0'), socio
FROM (SELECT DISTINCT socio FROM lotes ORDER BY socio) AS nombres;

-- 2. Agregar columna FK
ALTER TABLE lotes ADD COLUMN socio_id INTEGER;

-- 3. Poblar con los IDs correspondientes
UPDATE lotes SET socio_id = s.id FROM socios s WHERE lotes.socio = s.nombre;

-- 4. Hacer NOT NULL y agregar FK
ALTER TABLE lotes ALTER COLUMN socio_id SET NOT NULL;
ALTER TABLE lotes ADD CONSTRAINT fk_lotes_socio FOREIGN KEY (socio_id) REFERENCES socios(id);
CREATE INDEX idx_lotes_socio_id ON lotes(socio_id);

-- 5. Eliminar columna texto
ALTER TABLE lotes DROP COLUMN socio;
