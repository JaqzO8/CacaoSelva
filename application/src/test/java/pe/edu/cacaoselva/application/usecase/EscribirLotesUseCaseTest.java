package pe.edu.cacaoselva.application.usecase;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.exception.IdentificadorLoteInvalidoException;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.domain.exception.DatosLoteInvalidosException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscribirLotesUseCaseTest {
    @Mock private LoteWritePort writer;
    private final GuardarLoteCommand command = new GuardarLoteCommand(" Ana ", new BigDecimal("12.375"), EstadoLote.PENDIENTE);

    @Test
    void creaConIdGeneradoYNormalizaSocio() {
        when(writer.create(command.toDomain())).thenReturn(new Lote(31, "Ana", command.pesoKg(), command.estado()));
        var result = new CrearLoteUseCase(writer).execute(command);
        assertEquals(31, result.id());
        assertEquals("Ana", result.socio());
    }

    @Test
    void actualizaSinCambiarLaIdentidad() {
        when(writer.update(1, command.toDomain())).thenReturn(Optional.of(new Lote(1, "Ana", command.pesoKg(), command.estado())));
        assertEquals(1, new ActualizarLoteUseCase(writer).execute(1, command).id());
    }

    @Test
    void actualizarInexistenteLanzaExcepcionEspecifica() {
        when(writer.update(999, command.toDomain())).thenReturn(Optional.empty());
        assertThrows(LoteNoEncontradoException.class, () -> new ActualizarLoteUseCase(writer).execute(999, command));
    }

    @Test
    void eliminaExistente() {
        when(writer.deleteById(1)).thenReturn(true);
        new EliminarLoteUseCase(writer).execute(1);
        verify(writer).deleteById(1);
    }

    @Test
    void eliminarInexistenteLanzaExcepcionEspecifica() {
        assertThrows(LoteNoEncontradoException.class, () -> new EliminarLoteUseCase(writer).execute(999));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void validaIdAntesDeEscribir(Integer id) {
        assertThrows(IdentificadorLoteInvalidoException.class, () -> new ActualizarLoteUseCase(writer).execute(id, command));
        assertThrows(IdentificadorLoteInvalidoException.class, () -> new EliminarLoteUseCase(writer).execute(id));
        verifyNoInteractions(writer);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "1.0001", "1000000000"})
    void rechazaPesoFueraDelContratoSinEscribir(String peso) {
        var invalid = new GuardarLoteCommand("Ana", new BigDecimal(peso), EstadoLote.PENDIENTE);
        assertThrows(DatosLoteInvalidosException.class, () -> new CrearLoteUseCase(writer).execute(invalid));
        assertThrows(DatosLoteInvalidosException.class, () -> new ActualizarLoteUseCase(writer).execute(1, invalid));
        verifyNoInteractions(writer);
    }
}
