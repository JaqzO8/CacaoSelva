package pe.edu.cacaoselva.desktop.view;

import java.util.List;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.application.dto.SocioResumenDto;

public interface LotesView {
    void mostrarConsultando();
    void mostrarLotes(List<Lote> lotes);
    void mostrarError();
    void mostrarError(String message);
    void mostrarOperacion(String message);
    default void mostrarPaginaLotes(PaginaLotes page) {
        mostrarLotes(page.content().stream().map(lote -> lote.toDomain()).toList());
    }
    default void mostrarSocios(List<SocioResumenDto> socios) { }
    default void mostrarSaludApi(boolean disponible) { }
}
