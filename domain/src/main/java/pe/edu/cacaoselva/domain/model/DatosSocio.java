package pe.edu.cacaoselva.domain.model;

import pe.edu.cacaoselva.domain.exception.DatosSocioInvalidosException;

/** Datos de negocio sin identidad; se usan tanto al registrar como al modificar un socio. */
public record DatosSocio(String dni, String nombre, String zona, String telefono) {
    private static final java.util.regex.Pattern DNI_PATTERN = java.util.regex.Pattern.compile("^\\d{8}$");

    public DatosSocio {
        if (dni == null || !DNI_PATTERN.matcher(dni.strip()).matches()) {
            throw new DatosSocioInvalidosException("El DNI debe tener exactamente 8 dígitos.");
        }
        dni = dni.strip();
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 120) {
            throw new DatosSocioInvalidosException("El nombre es obligatorio y admite hasta 120 caracteres.");
        }
        nombre = nombre.strip();
        if (zona != null) {
            if (zona.isBlank()) {
                zona = null;
            } else if (zona.strip().length() > 60) {
                throw new DatosSocioInvalidosException("La zona admite hasta 60 caracteres.");
            } else {
                zona = zona.strip();
            }
        }
        if (telefono != null) {
            if (telefono.isBlank()) {
                telefono = null;
            } else if (telefono.strip().length() > 15) {
                throw new DatosSocioInvalidosException("El teléfono admite hasta 15 caracteres.");
            } else {
                telefono = telefono.strip();
            }
        }
    }
}
