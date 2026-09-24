package pe.edu.cacaoselva.desktop;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pe.edu.cacaoselva.desktop.controller.LotesController;
import pe.edu.cacaoselva.desktop.view.LotesWindow;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;
import pe.edu.cacaoselva.infrastructure.http.HttpLoteQueryAdapter;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;

public final class CacaoSelvaDesktopApplication extends Application {
    private final ExecutorService worker = Executors.newVirtualThreadPerTaskExecutor();
    private HttpLoteQueryAdapter adapter;
    private LotesController controller;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        adapter = HttpLoteQueryAdapter.create(ApiClientConfig.fromEnvironment());
        LotesWindow view = new LotesWindow();
        controller = new LotesController(adapter, view, worker, Platform::runLater,
                new CrearLoteUseCase(adapter), new ActualizarLoteUseCase(adapter), new EliminarLoteUseCase(adapter));
        view.setOnConsultar(controller::consultar);
        view.setOnGuardar(controller::guardar);
        view.setOnEliminar(controller::eliminar);
        Scene scene = new Scene(view, 1020, 680);
        scene.getStylesheets().add(getClass().getResource("/desktop.css").toExternalForm());
        stage.setTitle("CacaoSelva - Lotes");
        stage.setMinWidth(840);
        stage.setMinHeight(600);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.cerrar();
        }
        worker.shutdownNow();
        if (adapter != null) {
            adapter.close();
        }
    }
}
