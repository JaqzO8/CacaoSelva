package pe.edu.cacaoselva.infrastructure.http;

import pe.edu.cacaoselva.domain.model.DatosSocio;
import pe.edu.cacaoselva.domain.model.Socio;

record HttpSocioResponse(Integer id, String dni, String nombre, String zona, String telefono) {
    Socio toDomain() {
        return new Socio(id, new DatosSocio(dni, nombre, zona, telefono));
    }
}
