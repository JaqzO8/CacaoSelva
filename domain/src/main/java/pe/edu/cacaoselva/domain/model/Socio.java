package pe.edu.cacaoselva.domain.model;

/** Entidad de dominio con identidad propia. Representa un socio productor de cacao. */
public record Socio(Integer id, DatosSocio datos) {
    public Socio {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID del socio debe ser mayor que cero.");
        }
        if (datos == null) {
            throw new IllegalArgumentException("Los datos del socio son obligatorios.");
        }
    }

    public String dni() { return datos.dni(); }
    public String nombre() { return datos.nombre(); }
    public String zona() { return datos.zona(); }
    public String telefono() { return datos.telefono(); }

    @Override
    public String toString() { return nombre() + " (" + dni() + ")"; }
}
