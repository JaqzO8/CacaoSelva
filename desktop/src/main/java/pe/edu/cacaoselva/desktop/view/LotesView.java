package pe.edu.cacaoselva.desktop.view;

import java.util.List;
import pe.edu.cacaoselva.domain.model.Lote;

public interface LotesView {
    void mostrarConsultando();
    void mostrarLotes(List<Lote> lotes);
    void mostrarError();
    void mostrarError(String message);
    void mostrarOperacion(String message);
}
