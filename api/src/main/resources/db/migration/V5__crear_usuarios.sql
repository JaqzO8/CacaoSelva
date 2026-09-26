CREATE TABLE usuarios (
    id        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario   VARCHAR(50)  NOT NULL UNIQUE,
    hash      VARCHAR(255) NOT NULL,
    rol       VARCHAR(16)  NOT NULL CHECK (rol IN ('CONSULTOR', 'OPERADOR', 'ADMIN')),
    activo    BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Usuario administrador inicial (contraseña: "admin123", hash BCrypt)
INSERT INTO usuarios (usuario, hash, rol)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN');
