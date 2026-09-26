package pe.edu.cacaoselva.api.dto;

import pe.edu.cacaoselva.application.dto.GuardarSocioCommand;

/** Petición HTTP para crear un socio. */
public record GuardarSocioRequest(String dni, String nombre, String zona, String telefono) {
    public GuardarSocioCommand toCommand() {
        return new GuardarSocioCommand(dni, nombre, zona, telefono);
    }
}
