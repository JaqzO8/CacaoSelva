package pe.edu.cacaoselva.application.usecase;

import java.util.Objects;
import pe.edu.cacaoselva.application.dto.UsuarioDto;
import pe.edu.cacaoselva.application.port.PasswordPort;
import pe.edu.cacaoselva.application.port.UsuarioWritePort;
import pe.edu.cacaoselva.domain.model.Rol;

public final class RegistrarUsuarioUseCase {
    private final UsuarioWritePort usuarios;
    private final PasswordPort passwords;

    public RegistrarUsuarioUseCase(UsuarioWritePort usuarios, PasswordPort passwords) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.passwords = Objects.requireNonNull(passwords);
    }

    public UsuarioDto execute(String usuario, String contrasena, Rol rol) {
        if (usuario == null || !usuario.strip().matches("[A-Za-z0-9._-]{3,50}")) {
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 50 letras, números, puntos, guiones o guiones bajos.");
        }
        if (contrasena == null || contrasena.length() < 12) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres.");
        }
        if (contrasena.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("La contraseña admite como máximo 72 bytes UTF-8.");
        }
        if (rol == null) throw new IllegalArgumentException("El rol es obligatorio.");
        return UsuarioDto.from(usuarios.create(usuario.strip(), passwords.hash(contrasena), rol));
    }
}
