package pe.edu.cacaoselva.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cacaoselva.application.exception.ApiNoDisponibleException;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContarLotesPendientesUseCaseTest {
    @Mock private LoteQueryPort queryPort;

    @Test
    void cuentaSoloLosDosPendientes() {
        when(queryPort.findAll()).thenReturn(List.of(
                new Lote(1, "Ana", new BigDecimal("120.5"), EstadoLote.PENDIENTE),
                new Lote(2, "Luis", new BigDecimal("80"), EstadoLote.LIQUIDADO),
                new Lote(3, "Rosa", new BigDecimal("95.25"), EstadoLote.PENDIENTE)));

        assertEquals(2, new ContarLotesPendientesUseCase(queryPort).execute());
    }

    @Test
    void devuelveCeroSiNoHayLotes() {
        when(queryPort.findAll()).thenReturn(List.of());

        assertEquals(0, new ContarLotesPendientesUseCase(queryPort).execute());
    }

    @Test
    void devuelveCeroSiTodosEstanLiquidados() {
        when(queryPort.findAll()).thenReturn(List.of(
                new Lote(2, "Luis", new BigDecimal("80"), EstadoLote.LIQUIDADO)));

        assertEquals(0, new ContarLotesPendientesUseCase(queryPort).execute());
    }

    @Test
    void propagaElFalloSinInventarUnConteoCero() {
        when(queryPort.findAll()).thenThrow(new ApiNoDisponibleException("Sin conexión"));

        assertThrows(ApiNoDisponibleException.class,
                () -> new ContarLotesPendientesUseCase(queryPort).execute());
    }
}
