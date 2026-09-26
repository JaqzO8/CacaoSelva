package pe.edu.cacaoselva.desktop.controller;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.cacaoselva.application.exception.ApiNoDisponibleException;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.desktop.view.LotesView;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LotesControllerTest {
    @Mock private LoteQueryPort queryPort;
    @Mock private LotesView view;
    @Mock private LoteWritePort writer;
    private final Queue<Runnable> background = new ArrayDeque<>();
    private final Queue<Runnable> ui = new ArrayDeque<>();
    private LotesController controller;

    @BeforeEach
    void setUp() {
        controller = new LotesController(queryPort, view, background::add, ui::add,
                new CrearLoteUseCase(writer), new ActualizarLoteUseCase(writer), new EliminarLoteUseCase(writer));
    }

    @Test
    void ejecutaLaConsultaFueraDeLaUiYPublicaElResultadoEnElla() {
        List<Lote> lotes = List.of(new Lote(1, 1, new BigDecimal("120.5"), EstadoLote.PENDIENTE));
        when(queryPort.findAll(any(FiltroLotes.class))).thenReturn(page(lotes));

        controller.consultar();
        verify(view).mostrarConsultando();
        verifyNoInteractions(queryPort);
        background.remove().run();
        verify(queryPort).findAll(any(FiltroLotes.class));
        verify(view, never()).mostrarPaginaLotes(any(PaginaLotes.class));
        ui.remove().run();
        verify(view).mostrarPaginaLotes(page(lotes));
    }

    @Test
    void informaElFalloYPermiteVolverAConsultar() {
        when(queryPort.findAll(any(FiltroLotes.class))).thenThrow(new ApiNoDisponibleException("Sin conexión"))
                .thenReturn(page(List.of()));
        controller.consultar();
        background.remove().run();
        ui.remove().run();
        verify(view).mostrarError();

        controller.consultar();
        background.remove().run();
        ui.remove().run();
        verify(queryPort, times(2)).findAll(any(FiltroLotes.class));
        verify(view).mostrarPaginaLotes(page(List.of()));
    }

    @Test
    void impideConsultasSimultaneas() {
        controller.consultar();
        controller.consultar();
        assertEquals(1, background.size());
        verify(view).mostrarConsultando();
    }

    @Test
    void ignoraUnResultadoSiLaVentanaYaSeCerro() {
        controller.consultar();
        controller.cerrar();
        background.remove().run();
        ui.forEach(Runnable::run);
        verify(view, never()).mostrarPaginaLotes(any(PaginaLotes.class));
        verify(view, never()).mostrarError();
    }

    @Test
    void noConsultaDespuesDelCierre() {
        controller.cerrar();
        controller.consultar();
        verifyNoInteractions(view, queryPort);
        assertEquals(0, background.size());
    }

    @Test
    void guardaUnaSolaVezYActualizaLosDatos() {
        var command = new GuardarLoteCommand(1, BigDecimal.ONE, EstadoLote.PENDIENTE);
        Lote created = new Lote(31, 1, BigDecimal.ONE, EstadoLote.PENDIENTE);
        when(writer.create(command.toDomain())).thenReturn(created);
        when(queryPort.findAll(any(FiltroLotes.class))).thenReturn(page(List.of(created)));
        controller.guardar(null, command);
        controller.guardar(null, command);
        verifyNoInteractions(writer);
        background.remove().run();
        ui.remove().run();
        verify(writer, times(1)).create(command.toDomain());
        verify(view).mostrarPaginaLotes(page(List.of(created)));
    }

    @Test
    void siFallaLaLecturaPosteriorNoRepiteLaEscritura() {
        var command = new GuardarLoteCommand(1, BigDecimal.ONE, EstadoLote.PENDIENTE);
        when(writer.create(command.toDomain())).thenReturn(new Lote(31, 1, BigDecimal.ONE, EstadoLote.PENDIENTE));
        when(queryPort.findAll(any(FiltroLotes.class))).thenThrow(new ApiNoDisponibleException("Sin conexión"));
        controller.guardar(null, command);
        background.remove().run();
        ui.remove().run();
        verify(writer, times(1)).create(command.toDomain());
        verify(view).mostrarError(contains("antes de repetir"));
    }

    @Test
    void muestraValidacionSinEnviarDatosInvalidos() {
        controller.guardar(null, new GuardarLoteCommand(1, BigDecimal.ZERO, EstadoLote.PENDIENTE));
        background.remove().run();
        ui.remove().run();
        verifyNoInteractions(writer, queryPort);
        verify(view).mostrarError(contains("peso"));
    }

    private PaginaLotes page(List<Lote> content) {
        return new PaginaLotes(content.stream().map(LoteDto::from).toList(), 0, 20,
                content.size(), content.isEmpty() ? 0 : 1);
    }
}
