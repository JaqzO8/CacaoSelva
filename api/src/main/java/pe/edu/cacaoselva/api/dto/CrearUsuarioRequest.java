package pe.edu.cacaoselva.api.dto;

import pe.edu.cacaoselva.domain.model.Rol;

public record CrearUsuarioRequest(String usuario, String contrasena, Rol rol) { }
