package pe.edu.cacaoselva.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import pe.edu.cacaoselva.application.port.PasswordPort;

/** Implementación de PasswordPort usando BCrypt de Spring Security (Fase 3). */
public final class BCryptPasswordAdapter implements PasswordPort {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String contrasena) {
        return encoder.encode(contrasena);
    }

    @Override
    public boolean verificar(String contrasena, String hash) {
        return encoder.matches(contrasena, hash);
    }
}
