package pe.edu.cacaoselva.application.usecase;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarLotesUseCaseTest {
    @Mock private LoteRepository repository;

    @Test
    void devuelveLosRegistrosDelRepositorio() {
        Lote lote = new Lote(7, 1, new BigDecimal("120.5"), EstadoLote.PENDIENTE);
        when(repository.findAll()).thenReturn(List.of(lote));

        assertEquals(List.of(LoteDto.from(lote)), new ListarLotesUseCase(repository).execute());
        verify(repository).findAll();
    }

    @Test
    void admiteUnRepositorioVacio() {
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), new ListarLotesUseCase(repository).execute());
    }
}
