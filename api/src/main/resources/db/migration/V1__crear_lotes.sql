CREATE TABLE lotes (
    id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    socio VARCHAR(120) NOT NULL CHECK (CHAR_LENGTH(TRIM(socio)) > 0),
    peso_kg DECIMAL(12, 3) NOT NULL CHECK (peso_kg > 0),
    estado VARCHAR(16) NOT NULL CHECK (estado IN ('PENDIENTE', 'LIQUIDADO'))
);

CREATE INDEX idx_lotes_estado ON lotes(estado);
