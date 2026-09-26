package pe.edu.cacaoselva.infrastructure.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

public final class InMemoryLoteRepository implements LoteRepository {
    private final List<Lote> lotes = List.of(
            new Lote(1, 1, new BigDecimal("120.5"), EstadoLote.PENDIENTE),
            new Lote(2, 2, new BigDecimal("80"), EstadoLote.LIQUIDADO),
            new Lote(3, 3, new BigDecimal("95.25"), EstadoLote.PENDIENTE));

    @Override
    public List<Lote> findAll() {
        return lotes;
    }

    @Override
    public Optional<Lote> findById(Integer id) {
        return lotes.stream().filter(lote -> lote.id().equals(id)).findFirst();
    }
}
