package pe.edu.cacaoselva.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.cacaoselva.domain.exception.DatosSocioInvalidosException;

import static org.junit.jupiter.api.Assertions.*;

class SocioTest {
    private static DatosSocio datos() {
        return new DatosSocio("12345678", "Ana López", "Zona Norte", "999888777");
    }

    @Test
    void creaConDatosValidos() {
        var socio = new Socio(1, datos());
        assertEquals(1, socio.id());
        assertEquals("12345678", socio.dni());
        assertEquals("Ana López", socio.nombre());
        assertEquals("Zona Norte", socio.zona());
        assertEquals("999888777", socio.telefono());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rechazaIdInvalido(Integer id) {
        assertThrows(IllegalArgumentException.class, () -> new Socio(id, datos()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "1234567", "123456789", "abcdefgh", "1234 678"})
    void rechazaDniInvalido(String dni) {
        assertThrows(DatosSocioInvalidosException.class,
                () -> new DatosSocio(dni, "Ana", null, null));
    }

    @Test
    void validaDni8Digitos() {
        var datos = new DatosSocio("12345678", "Ana", null, null);
        assertEquals("12345678", datos.dni());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rechazaNombreVacio(String nombre) {
        assertThrows(DatosSocioInvalidosException.class,
                () -> new DatosSocio("12345678", nombre, null, null));
    }

    @Test
    void normalizaEspaciosEnNombre() {
        var datos = new DatosSocio(" 12345678 ", "  Ana López  ", null, null);
        assertEquals("12345678", datos.dni());
        assertEquals("Ana López", datos.nombre());
    }

    @Test
    void blancoSeConvierteANullEnZonaYTelefono() {
        var datos = new DatosSocio("12345678", "Ana", "  ", "  ");
        assertNull(datos.zona());
        assertNull(datos.telefono());
    }
}
