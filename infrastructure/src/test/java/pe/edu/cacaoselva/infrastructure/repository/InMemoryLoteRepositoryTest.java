package pe.edu.cacaoselva.infrastructure.repository;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryLoteRepositoryTest {
    private final InMemoryLoteRepository repository = new InMemoryLoteRepository();

    @Test
    void contieneLosTresLotesInicialesYDosPendientes() {
        assertEquals(3, repository.findAll().size());
        assertEquals(2, new ContarLotesPendientesUseCase(repository).execute());
        assertEquals(1, repository.findById(1).orElseThrow().socioId());
        assertEquals(new BigDecimal("95.25"), repository.findById(3).orElseThrow().pesoKg());
        assertTrue(repository.findById(999).isEmpty());
    }

    @Test
    void impideModificarLosDatosCompartidos() {
        assertThrows(UnsupportedOperationException.class, () -> repository.findAll().clear());
        assertEquals(3, repository.findAll().size());
    }
}
