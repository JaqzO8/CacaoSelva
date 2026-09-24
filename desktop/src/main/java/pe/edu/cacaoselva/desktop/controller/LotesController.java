package pe.edu.cacaoselva.desktop.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import java.util.List;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.desktop.view.LotesView;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.domain.exception.DatosLoteInvalidosException;
import pe.edu.cacaoselva.application.exception.LoteNoEncontradoException;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;

/** Sus métodos y las notificaciones a la vista se ejecutan en el hilo de UI. */
public final class LotesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LotesController.class);
    private final LoteQueryPort queryPort;
    private final LotesView view;
    private final Executor worker;
    private final Consumer<Runnable> uiDispatcher;
    private final CrearLoteUseCase crear;
    private final ActualizarLoteUseCase actualizar;
    private final EliminarLoteUseCase eliminar;
    private CompletableFuture<?> pending;
    private boolean consultando;
    private boolean cerrado;

    public LotesController(LoteQueryPort queryPort, LotesView view,
                           Executor worker, Consumer<Runnable> uiDispatcher,
                           CrearLoteUseCase crear, ActualizarLoteUseCase actualizar, EliminarLoteUseCase eliminar) {
        this.queryPort = queryPort;
        this.view = view;
        this.worker = worker;
        this.uiDispatcher = uiDispatcher;
        this.crear = crear;
        this.actualizar = actualizar;
        this.eliminar = eliminar;
    }

    public void consultar() {
        ejecutar(queryPort::findAll, null);
    }

    public void guardar(Integer id, GuardarLoteCommand command) {
        ejecutar(() -> {
            if (id == null) {
                crear.execute(command);
            } else {
                actualizar.execute(id, command);
            }
            return queryPort.findAll();
        }, "Guardando lote...");
    }

    public void eliminar(Integer id) {
        ejecutar(() -> {
            eliminar.execute(id);
            return queryPort.findAll();
        }, "Eliminando lote...");
    }

    private void ejecutar(Supplier<List<Lote>> action, String operation) {
        if (consultando || cerrado) {
            return;
        }
        consultando = true;
        if (operation == null) {
            view.mostrarConsultando();
        } else {
            view.mostrarOperacion(operation);
        }
        try {
            pending = CompletableFuture.supplyAsync(action, worker)
                .whenComplete((lotes, error) -> uiDispatcher.accept(() -> {
                    if (cerrado) {
                        return;
                    }
                    consultando = false;
                    if (error == null) {
                        view.mostrarLotes(lotes);
                    } else {
                        informarError(error, operation != null);
                    }
                }));
        } catch (RejectedExecutionException error) {
            consultando = false;
            view.mostrarError("La aplicación está cerrando. Vuelve a abrirla para consultar.");
        }
    }

    private void informarError(Throwable error, boolean write) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        LOGGER.warn("Fallo en la operación de lotes: {}", cause.getMessage());
        if (cause instanceof DatosLoteInvalidosException || cause instanceof LoteNoEncontradoException) {
            view.mostrarError(cause.getMessage());
        } else if (write) {
            view.mostrarError("No se confirmó el resultado. Consulta los lotes antes de repetir la operación.");
        } else {
            view.mostrarError();
        }
    }

    public void cerrar() {
        cerrado = true;
        if (pending != null) {
            pending.cancel(true);
        }
    }
}
