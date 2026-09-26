package pe.edu.cacaoselva.desktop.view;

import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.domain.model.DatosLote;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.DatosSocio;
import pe.edu.cacaoselva.domain.model.Socio;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import java.awt.image.BufferedImage;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import pe.edu.cacaoselva.desktop.controller.LotesController;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;
import pe.edu.cacaoselva.infrastructure.http.HttpLoteQueryAdapter;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;

import static org.junit.jupiter.api.Assertions.*;

/** Prueba opcional con controles reales; requiere una sesión gráfica. */
@EnabledIfSystemProperty(named = "cacaoselva.test.javafx", matches = "true")
class LotesWindowFxTest {
    private static final String LOTES = """
            [{"id":1,"socioId":1,"pesoKg":120.5,"estado":"PENDIENTE","version":1},
             {"id":2,"socioId":2,"pesoKg":80,"estado":"LIQUIDADO","version":1},
             {"id":3,"socioId":3,"pesoKg":95.25,"estado":"PENDIENTE","version":1}]
            """;
    private LotesWindow view;
    private Stage stage;
    private LotesController controller;

    @BeforeAll
    static void iniciarJavaFx() throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            ready.countDown();
        });
        assertTrue(ready.await(10, TimeUnit.SECONDS));
    }

    @AfterAll
    static void detenerJavaFx() {
        Platform.exit();
    }

    @Test
    void consultaFallaLimpiaYRecuperaEnControlesReales() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger status = new AtomicInteger(200);
        ObjectMapper json = new ObjectMapper();
        var lotes = new CopyOnWriteArrayList<>(List.of(json.readValue(LOTES, Lote[].class)));
        server.createContext("/auth/login", exchange -> {
            try (exchange) {
                byte[] bytes = "{\"token\":\"fx-test-token\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.createContext("/actuator/health", exchange -> {
            try (exchange) {
                byte[] bytes = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.createContext("/socios", exchange -> {
            try (exchange) {
                var socios = List.of(
                        java.util.Map.of("id", 1, "dni", "11111111", "nombre", "Ana", "zona", "Zona", "telefono", ""),
                        java.util.Map.of("id", 2, "dni", "22222222", "nombre", "Luis", "zona", "Zona", "telefono", ""),
                        java.util.Map.of("id", 3, "dni", "33333333", "nombre", "Rosa", "zona", "Zona", "telefono", ""),
                        java.util.Map.of("id", 4, "dni", "44444444", "nombre", "Paco", "zona", "Zona", "telefono", ""),
                        java.util.Map.of("id", 5, "dni", "55555555", "nombre", "Luz", "zona", "Zona", "telefono", ""));
                byte[] bytes = json.writeValueAsBytes(socios);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.createContext("/lotes", exchange -> {
            try (exchange) {
                int code = status.get();
                Object response = lotes;
                if (code == 200 && !exchange.getRequestMethod().equals("GET")) {
                    String method = exchange.getRequestMethod();
                    int id = method.equals("POST") ? 4 : Integer.parseInt(exchange.getRequestURI().getPath().substring(7));
                    if (method.equals("DELETE")) {
                        lotes.removeIf(lote -> lote.id() == id);
                        code = 204;
                    } else {
                        GuardarLoteCommand datos = json.readValue(exchange.getRequestBody(), GuardarLoteCommand.class);
                        int version = method.equals("POST") ? 1 : lotes.stream().filter(lote -> lote.id() == id).mapToInt(Lote::version).findFirst().orElse(0) + 1;
                        Lote updated = new Lote(id, datos.socioId(), datos.pesoKg(), datos.estado(), version);
                        lotes.removeIf(lote -> lote.id() == id);
                        lotes.add(updated);
                        response = updated;
                        code = method.equals("POST") ? 201 : 200;
                    }
                } else if (code == 200) {
                    var params = exchange.getRequestURI().getQuery() == null ? "" : exchange.getRequestURI().getQuery();
                    String socioFilter = params.contains("socioId=") ? params.substring(params.indexOf("socioId=") + 8).split("&")[0] : null;
                    var content = lotes.stream().filter(lote -> socioFilter == null || Integer.toString(lote.socioId()).equals(socioFilter))
                            .map(lote -> java.util.Map.of("id", lote.id(), "socioId", lote.socioId(), "pesoKg", lote.pesoKg(),
                                    "estado", lote.estado().name(), "version", lote.version())).toList();
                    response = java.util.Map.of("content", content, "page", 0, "size", 20,
                            "totalElements", content.size(), "totalPages", content.isEmpty() ? 0 : 1);
                }
                byte[] bytes = json.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, code == 204 ? -1 : bytes.length);
                if (code != 204) { exchange.getResponseBody().write(bytes); }
            }
        });
        server.start();
        URI url = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try (var adapter = HttpLoteQueryAdapter.create(new ApiClientConfig(url, Duration.ofSeconds(2)));
             var worker = Executors.newVirtualThreadPerTaskExecutor()) {
            adapter.setCredentials("admin", "test-password");
            onFx(() -> {
                view = new LotesWindow();
                view.mostrarSocios(List.of(
                        new Socio(1, new DatosSocio("11111111", "Ana", "Zona", "")),
                        new Socio(2, new DatosSocio("22222222", "Luis", "Zona", "")),
                        new Socio(3, new DatosSocio("33333333", "Rosa", "Zona", "")),
                        new Socio(4, new DatosSocio("44444444", "Paco", "Zona", "")),
                        new Socio(5, new DatosSocio("55555555", "Luz", "Zona", ""))));
                controller = new LotesController(adapter, view, worker, Platform::runLater,
                        new CrearLoteUseCase(adapter), new ActualizarLoteUseCase(adapter), new EliminarLoteUseCase(adapter));
                view.setOnConsultar(controller::consultar);
                view.setOnBuscar(controller::consultar);
                view.setOnGuardar(controller::guardar);
                view.setOnEliminar(controller::eliminar);
                Scene scene = new Scene(view, 1020, 680);
                scene.getStylesheets().add(getClass().getResource("/desktop.css").toExternalForm());
                stage = new Stage();
                stage.setTitle("CacaoSelva - Lotes");
                stage.setScene(scene);
                stage.show();
                assertEquals("Estado: Listo", label().getText());
                assertTrue(table().getItems().isEmpty());
                return null;
            });

            consultar();
            esperarEstado("3 lotes recibidos.");
            onFx(() -> {
                assertEquals(3, table().getItems().size());
                assertFalse(button().isDisabled());
                TextField filter = (TextField) view.lookup("#filtro-socio");
                filter.setText("3");
                return null;
            });
            esperarEstado("1 lotes recibidos.");
            onFx(() -> {
                assertEquals(1, table().getItems().size());
                TextField filter = (TextField) view.lookup("#filtro-socio");
                filter.clear();
                guardarCaptura();
                return null;
            });

            status.set(503);
            consultar();
            esperarEstado("No se pudo conectar con la API.");
            onFx(() -> {
                assertTrue(table().getItems().isEmpty());
                assertFalse(button().isDisabled());
                return null;
            });

            status.set(200);
            consultar();
            esperarEstado("3 lotes recibidos.");
            onFx(() -> {
                assertEquals(3, table().getItems().size());
                ((ComboBox<Socio>) view.lookup("#socio")).setValue(new Socio(4, new DatosSocio("44444444", "Paco", "Zona", "")));
                ((TextField) view.lookup("#peso")).setText("12.375");
                ((Button) view.lookup("#guardar")).fire();
                return null;
            });
            esperarEstado("4 lotes recibidos.");
            onFx(() -> {
                table().getSelectionModel().select(3);
                assertEquals(4, ((ComboBox<Socio>) view.lookup("#socio")).getValue().id());
                ((ComboBox<Socio>) view.lookup("#socio")).setValue(new Socio(5, new DatosSocio("55555555", "Luz", "Zona", "")));
                ((Button) view.lookup("#guardar")).fire();
                return null;
            });
            esperarEstado("4 lotes recibidos.");
            onFx(() -> {
                assertTrue(table().getItems().stream().anyMatch(item -> ((Lote) item).socioId().equals(5)));
                controller.eliminar(4);
                return null;
            });
            esperarEstado("3 lotes recibidos.");
        } finally {
            server.stop(0);
            onFx(() -> {
                if (controller != null) {
                    controller.cerrar();
                }
                if (stage != null) {
                    stage.close();
                }
                return null;
            });
        }
    }

    private void consultar() throws Exception {
        onFx(() -> {
            button().fire();
            assertTrue(button().isDisabled());
            assertEquals("Consultando...", label().getText());
            assertTrue(table().getItems().isEmpty());
            return null;
        });
    }

    private void esperarEstado(String expected) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (onFx(() -> expected.equals(label().getText()))) {
                return;
            }
            Thread.sleep(20);
        }
        fail("No se alcanzó el estado: " + expected);
    }

    private void guardarCaptura() throws java.io.IOException {
        var image = view.snapshot(null, null);
        BufferedImage png = new BufferedImage((int) image.getWidth(), (int) image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) {
            for (int x = 0; x < png.getWidth(); x++) {
                png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        Path output = Path.of("target", "javafx-smoke.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(png, "png", output.toFile());
    }

    private Button button() { return (Button) view.lookup("#consultar"); }
    private Label label() { return (Label) view.lookup("#estado"); }
    private TableView<?> table() { return (TableView<?>) view.lookup("#lotes"); }

    private static <T> T onFx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
