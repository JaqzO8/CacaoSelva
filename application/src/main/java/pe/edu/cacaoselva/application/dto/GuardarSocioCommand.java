package pe.edu.cacaoselva.application.dto;

import pe.edu.cacaoselva.domain.model.DatosSocio;

/** Comando para crear o actualizar un socio. */
public record GuardarSocioCommand(String dni, String nombre, String zona, String telefono) {
    public DatosSocio toDomain() {
        return new DatosSocio(dni, nombre, zona, telefono);
    }
}
