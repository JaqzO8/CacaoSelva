CREATE TABLE socios (
    id       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dni      VARCHAR(8)   NOT NULL UNIQUE CHECK (dni ~ '^\d{8}$'),
    nombre   VARCHAR(120) NOT NULL CHECK (CHAR_LENGTH(TRIM(nombre)) > 0),
    zona     VARCHAR(60),
    telefono VARCHAR(15)
);

CREATE INDEX idx_socios_nombre ON socios(nombre);
