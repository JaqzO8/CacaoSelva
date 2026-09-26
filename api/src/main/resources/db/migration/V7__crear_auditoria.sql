CREATE TABLE auditoria_lotes (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lote_id     INTEGER NOT NULL,
    accion      VARCHAR(10) NOT NULL CHECK (accion IN ('CREAR', 'ACTUALIZAR', 'ELIMINAR')),
    usuario     VARCHAR(50),
    datos_json  JSONB,
    fecha       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_auditoria_lote_id ON auditoria_lotes(lote_id);
CREATE INDEX idx_auditoria_fecha ON auditoria_lotes(fecha);
