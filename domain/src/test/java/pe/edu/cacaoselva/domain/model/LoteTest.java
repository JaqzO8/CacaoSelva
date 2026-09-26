package pe.edu.cacaoselva.domain.model;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoteTest {
    @Test
    void conservaElPesoDecimalExacto() {
        Lote lote = new Lote(1, 1, new BigDecimal("120.50"), EstadoLote.PENDIENTE);
        assertEquals(new BigDecimal("120.50"), lote.pesoKg());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rechazaUnaIdentidadInvalida(Integer id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(id, 1, BigDecimal.ONE, EstadoLote.PENDIENTE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rechazaSocioIdInvalido(Integer socioId) {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, socioId, BigDecimal.ONE, EstadoLote.PENDIENTE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01"})
    void rechazaPesosInvalidos(String peso) {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, 1, peso == null ? null : new BigDecimal(peso), EstadoLote.PENDIENTE));
    }

    @Test
    void exigeUnEstado() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, 1, BigDecimal.ONE, null));
    }

    @Test
    void versionPorDefectoEsUno() {
        Lote lote = new Lote(1, 1, BigDecimal.ONE, EstadoLote.PENDIENTE);
        assertEquals(1, lote.version());
    }

    @Test
    void aceptaVersionExplicita() {
        Lote lote = new Lote(1, 1, BigDecimal.ONE, EstadoLote.PENDIENTE, 5);
        assertEquals(5, lote.version());
    }

    @Test
    void rechazaVersionCero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, 1, BigDecimal.ONE, EstadoLote.PENDIENTE, 0));
    }
}
