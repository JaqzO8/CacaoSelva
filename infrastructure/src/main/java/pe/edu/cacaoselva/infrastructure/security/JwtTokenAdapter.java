package pe.edu.cacaoselva.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import pe.edu.cacaoselva.application.port.TokenPort;
import pe.edu.cacaoselva.domain.model.Rol;

/** Implementación de TokenPort usando JJWT (Fase 3). */
public final class JwtTokenAdapter implements TokenPort {
    private static final long EXPIRACION_MS = 3600_000; // 1 hora
    private final Key key;

    public JwtTokenAdapter(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("El secreto JWT debe tener al menos 32 caracteres.");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SignatureAlgorithm.HS256.getJcaName());
    }

    @Override
    public String generar(String usuario, Rol rol) {
        return Jwts.builder()
                .setSubject(usuario)
                .claim("rol", rol.name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRACION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String validarYObtenerUsuario(String token) {
        return parseClaims(token).getSubject();
    }

    @Override
    public Rol validarYObtenerRol(String token) {
        String rol = parseClaims(token).get("rol", String.class);
        return Rol.valueOf(rol);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (JwtException | IllegalArgumentException error) {
            throw new SecurityException("Token JWT inválido o expirado.", error);
        }
    }
}
