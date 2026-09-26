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
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.exception.IdentificadorLoteInvalidoException;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarLotePorIdUseCaseTest {
    @Mock private LoteRepository repository;

    @Test
    void devuelveElLoteExistente() {
        Lote ana = new Lote(1, 1, new BigDecimal("120.5"), EstadoLote.PENDIENTE);
        when(repository.findById(1)).thenReturn(Optional.of(ana));

        assertEquals(LoteDto.from(ana), new BuscarLotePorIdUseCase(repository).execute(1));
    }

    @Test
    void informaCuandoElLoteNoExiste() {
        when(repository.findById(999)).thenReturn(Optional.empty());

        var error = assertThrows(LoteNoEncontradoException.class,
                () -> new BuscarLotePorIdUseCase(repository).execute(999));
        assertEquals("No existe el lote con id 999", error.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rechazaIdsInvalidosAntesDeConsultar(Integer id) {
        assertThrows(IdentificadorLoteInvalidoException.class,
                () -> new BuscarLotePorIdUseCase(repository).execute(id));
        verifyNoInteractions(repository);
    }
}
