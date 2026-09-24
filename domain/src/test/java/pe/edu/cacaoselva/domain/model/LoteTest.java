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
        Lote lote = new Lote(1, "Ana", new BigDecimal("120.50"), EstadoLote.PENDIENTE);
        assertEquals(new BigDecimal("120.50"), lote.pesoKg());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rechazaUnaIdentidadInvalida(Integer id) {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(id, "Ana", BigDecimal.ONE, EstadoLote.PENDIENTE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rechazaSociosVacios(String socio) {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, socio, BigDecimal.ONE, EstadoLote.PENDIENTE));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01"})
    void rechazaPesosInvalidos(String peso) {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, "Ana", peso == null ? null : new BigDecimal(peso), EstadoLote.PENDIENTE));
    }

    @Test
    void exigeUnEstado() {
        assertThrows(IllegalArgumentException.class,
                () -> new Lote(1, "Ana", BigDecimal.ONE, null));
    }
}
