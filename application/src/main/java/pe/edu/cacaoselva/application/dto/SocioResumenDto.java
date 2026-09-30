package pe.edu.cacaoselva.application.dto;

/** Catálogo de selección sin DNI, zona ni teléfono. */
public record SocioResumenDto(Integer id, String nombre) {
    @Override public String toString() { return nombre; }
}
