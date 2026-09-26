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
        if (usuario == null || usuario.isBlank() || usuario.strip().length() > 50) {
            throw new IllegalArgumentException("El usuario es obligatorio y admite hasta 50 caracteres.");
        }
        if (contrasena == null || contrasena.length() < 12) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 12 caracteres.");
        }
        if (rol == null) throw new IllegalArgumentException("El rol es obligatorio.");
        return UsuarioDto.from(usuarios.create(usuario.strip(), passwords.hash(contrasena), rol));
    }
}
