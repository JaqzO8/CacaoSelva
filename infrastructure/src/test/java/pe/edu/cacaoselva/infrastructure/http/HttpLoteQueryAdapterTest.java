package pe.edu.cacaoselva.infrastructure.http;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.cacaoselva.application.exception.ApiNoDisponibleException;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;

import static org.junit.jupiter.api.Assertions.*;

class HttpLoteQueryAdapterTest {
    private static final String VALID_JSON = """
            [{"id":1,"socio":"Ana","pesoKg":120.5,"estado":"PENDIENTE"}]
            """;
    private HttpServer server;
    private ExecutorService executor;
    private URI baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.start();
        baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        executor.shutdownNow();
    }

    private void respond(int status, String body, Duration bodyDelay) {
        server.createContext("/lotes", exchange -> {
            try (exchange) {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                try {
                    Thread.sleep(bodyDelay);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    return;
                }
                exchange.getResponseBody().write(bytes);
            }
        });
    }

    private HttpLoteQueryAdapter adapter(Duration timeout) {
        return HttpLoteQueryAdapter.create(new ApiClientConfig(baseUrl, timeout));
    }

    @Test
    void convierteJsonADominioConPesoExacto() {
        respond(200, VALID_JSON, Duration.ZERO);
        try (var adapter = adapter(Duration.ofSeconds(5))) {
            var lotes = adapter.findAll();
            assertEquals(1, lotes.size());
            assertEquals("Ana", lotes.getFirst().socio());
            assertEquals(new BigDecimal("120.5"), lotes.getFirst().pesoKg());
            assertEquals(EstadoLote.PENDIENTE, lotes.getFirst().estado());
        }
    }

    @Test
    void admiteUnaListaVaciaValida() {
        respond(200, "[]", Duration.ZERO);
        try (var adapter = adapter(Duration.ofSeconds(5))) {
            assertTrue(adapter.findAll().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 404, 500, 503})
    void traduceEstadosHttpFallidos(int status) {
        respond(status, "{}", Duration.ZERO);
        try (var adapter = adapter(Duration.ofSeconds(5))) {
            var error = assertThrows(ApiNoDisponibleException.class, adapter::findAll);
            assertTrue(error.getMessage().contains(String.valueOf(status)));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"no es JSON", "{}", "null", "[null]", "[{}]",
            "[{\"id\":1,\"socio\":\"Ana\",\"pesoKg\":1,\"estado\":\"OTRO\"}]",
            "[{\"id\":1.5,\"socio\":\"Ana\",\"pesoKg\":1,\"estado\":\"PENDIENTE\"}]",
            "[] {}"})
    void traduceJsonInvalido(String body) {
        respond(200, body, Duration.ZERO);
        try (var adapter = adapter(Duration.ofSeconds(5))) {
            assertThrows(ApiNoDisponibleException.class, adapter::findAll);
        }
    }

    @Test
    void limitaTambienLaEsperaDelCuerpoDeRespuesta() {
        respond(200, VALID_JSON, Duration.ofSeconds(2));
        try (var adapter = adapter(Duration.ofMillis(150))) {
            assertTimeoutPreemptively(Duration.ofSeconds(1),
                    () -> assertThrows(ApiNoDisponibleException.class, adapter::findAll));
        }
    }

    @Test
    void traduceConexionRechazada() {
        server.stop(0);
        try (var adapter = adapter(Duration.ofSeconds(1))) {
            assertThrows(ApiNoDisponibleException.class, adapter::findAll);
        }
    }

    @Test
    void conservaLaSenalDeInterrupcion() {
        respond(200, VALID_JSON, Duration.ofSeconds(2));
        try (var adapter = adapter(Duration.ofSeconds(5))) {
            Thread.currentThread().interrupt();
            assertThrows(ApiNoDisponibleException.class, adapter::findAll);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
