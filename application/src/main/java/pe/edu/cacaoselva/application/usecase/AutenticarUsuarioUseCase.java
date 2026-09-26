package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.exception.CredencialesInvalidasException;
import pe.edu.cacaoselva.application.port.PasswordPort;
import pe.edu.cacaoselva.application.port.TokenPort;
import pe.edu.cacaoselva.application.port.UsuarioRepository;
import pe.edu.cacaoselva.domain.model.Credenciales;
import pe.edu.cacaoselva.domain.model.Usuario;

/** Fase 3: autenticación de usuario y generación de JWT. */
public final class AutenticarUsuarioUseCase {
    private final UsuarioRepository usuarios;
    private final PasswordPort passwords;
    private final TokenPort tokens;

    public AutenticarUsuarioUseCase(UsuarioRepository usuarios, PasswordPort passwords, TokenPort tokens) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.passwords = Objects.requireNonNull(passwords);
        this.tokens = Objects.requireNonNull(tokens);
    }

    /** Devuelve el JWT firmado si las credenciales son válidas. */
    public String execute(Credenciales credenciales) {
        Objects.requireNonNull(credenciales);
        Usuario usuario = usuarios.findByUsuario(credenciales.usuario())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.activo()) {
            throw new CredencialesInvalidasException();
        }
        if (!passwords.verificar(credenciales.contrasena(), usuario.hash())) {
            throw new CredencialesInvalidasException();
        }
        return tokens.generar(usuario.usuario(), usuario.rol());
    }
}
