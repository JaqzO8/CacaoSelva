package pe.edu.cacaoselva.application.dto;

import pe.edu.cacaoselva.domain.model.Rol;
import pe.edu.cacaoselva.domain.model.Usuario;

public record UsuarioDto(Integer id, String usuario, Rol rol, boolean activo) {
    public static UsuarioDto from(Usuario usuario) {
        return new UsuarioDto(usuario.id(), usuario.usuario(), usuario.rol(), usuario.activo());
    }
}
