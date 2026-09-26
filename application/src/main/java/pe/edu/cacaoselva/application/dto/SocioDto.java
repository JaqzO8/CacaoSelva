package pe.edu.cacaoselva.application.dto;

import pe.edu.cacaoselva.domain.model.Socio;

/** Salida de los casos de uso de socios. */
public record SocioDto(Integer id, String dni, String nombre, String zona, String telefono) {
    public static SocioDto from(Socio socio) {
        return new SocioDto(socio.id(), socio.dni(), socio.nombre(), socio.zona(), socio.telefono());
    }
}
