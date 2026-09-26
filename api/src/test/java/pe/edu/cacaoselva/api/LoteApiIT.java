package pe.edu.cacaoselva.api;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import pe.edu.cacaoselva.api.dto.GuardarLoteRequest;
import pe.edu.cacaoselva.api.dto.ConteoPendientesResponse;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.test.web.server.LocalServerPort;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;
import pe.edu.cacaoselva.infrastructure.http.HttpLoteQueryAdapter;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.api.dto.ApiErrorResponse;
import pe.edu.cacaoselva.api.dto.LoteResponse;
import pe.edu.cacaoselva.api.dto.PaginaLotesResponse;
import pe.edu.cacaoselva.application.dto.EventoAuditoria;
import pe.edu.cacaoselva.domain.model.Rol;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoteApiIT {
    @Autowired private TestRestTemplate http;
    @LocalServerPort private int port;
    @Value("${cacaoselva.admin.password}") private String adminPassword;

    @BeforeEach
    void autenticar() {
        var login = http.postForObject("/auth/login", java.util.Map.of("usuario", "admin", "contrasena", adminPassword),
                pe.edu.cacaoselva.api.dto.LoginResponse.class);
        assertNotNull(login);
        http.getRestTemplate().getInterceptors().add((request, body, execution) -> {
            request.getHeaders().setBearerAuth(login.token());
            return execution.execute(request, body);
        });
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("CACAOSELVA_TEST_DB_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/cacaoselva_test_[a-z0-9]+")) {
            throw new IllegalStateException("Ejecuta scripts/verify.ps1: las pruebas requieren una base cacaoselva_test_* aislada.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> System.getenv("CACAOSELVA_TEST_DB_USER"));
        properties.add("spring.datasource.password", () -> System.getenv("CACAOSELVA_TEST_DB_PASSWORD"));
    }

    @Test
    void p01DevuelveTreintaLotesYVeintePendientes() {
        var response = http.getForEntity("/lotes", LoteResponse[].class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        LoteResponse[] lotes = response.getBody();
        assertNotNull(lotes);
        assertEquals(30, lotes.length);
        assertEquals(20, java.util.Arrays.stream(lotes)
                .filter(lote -> lote.estado().equals("PENDIENTE")).count());
        assertEquals(0, new BigDecimal("120.5").compareTo(lotes[0].pesoKg()));
    }

    @Test
    void p02DevuelvePrimerLote() {
        var response = http.getForEntity("/lotes/1", LoteResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().socioId());
        assertEquals(1, response.getBody().id());
    }

    @Test
    void paginacionFiltraYRechazaParametrosFueraDeRango() {
        var page = http.getForObject("/lotes?page=0&size=5&estado=PENDIENTE", PaginaLotesResponse.class);
        assertNotNull(page);
        assertEquals(5, page.content().size());
        assertEquals(20, page.totalElements());
        assertEquals(4, page.totalPages());
        assertEquals(HttpStatus.BAD_REQUEST,
                http.getForEntity("/lotes?page=0&size=101", ApiErrorResponse.class).getStatusCode());
    }

    @Test
    void jwtExigeTokenYAplicaPermisosPorRol() {
        var anonymous = new TestRestTemplate();
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.getForEntity("http://localhost:" + port + "/lotes", String.class).getStatusCode());
        HttpHeaders invalidHeaders = new HttpHeaders();
        invalidHeaders.setBearerAuth("token-invalido");
        assertEquals(HttpStatus.UNAUTHORIZED, anonymous.exchange("http://localhost:" + port + "/lotes",
                HttpMethod.GET, new HttpEntity<>(invalidHeaders), String.class).getStatusCode());

        String consultantName = "consultor_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        var consultantCreated = http.postForEntity("/auth/usuarios",
                java.util.Map.of("usuario", consultantName, "contrasena", "Consultor-12345", "rol", Rol.CONSULTOR),
                pe.edu.cacaoselva.application.dto.UsuarioDto.class);
        assertEquals(HttpStatus.CREATED, consultantCreated.getStatusCode());
        var consultant = anonymous.postForObject("http://localhost:" + port + "/auth/login",
                java.util.Map.of("usuario", consultantName, "contrasena", "Consultor-12345"),
                pe.edu.cacaoselva.api.dto.LoginResponse.class);
        assertNotNull(consultant);
        HttpHeaders consultantHeaders = new HttpHeaders();
        consultantHeaders.setBearerAuth(consultant.token());
        assertEquals(HttpStatus.OK, anonymous.exchange("http://localhost:" + port + "/lotes",
                HttpMethod.GET, new HttpEntity<>(consultantHeaders), String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, anonymous.exchange("http://localhost:" + port + "/socios",
                HttpMethod.GET, new HttpEntity<>(consultantHeaders), String.class).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, anonymous.exchange("http://localhost:" + port + "/auth/usuarios",
                HttpMethod.POST, new HttpEntity<>(java.util.Map.of("usuario", "denegado", "contrasena", "Contraseña-123", "rol", "OPERADOR"), consultantHeaders),
                String.class).getStatusCode());

        String operatorName = "operador_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        assertEquals(HttpStatus.CREATED, http.postForEntity("/auth/usuarios",
                java.util.Map.of("usuario", operatorName, "contrasena", "Operador-12345", "rol", Rol.OPERADOR),
                pe.edu.cacaoselva.application.dto.UsuarioDto.class).getStatusCode());
        var operator = anonymous.postForObject("http://localhost:" + port + "/auth/login",
                java.util.Map.of("usuario", operatorName, "contrasena", "Operador-12345"),
                pe.edu.cacaoselva.api.dto.LoginResponse.class);
        assertNotNull(operator);
        HttpHeaders operatorHeaders = new HttpHeaders();
        operatorHeaders.setBearerAuth(operator.token());
        var created = anonymous.postForEntity("http://localhost:" + port + "/lotes",
                new HttpEntity<>(new GuardarLoteRequest(1, BigDecimal.ONE, EstadoLote.PENDIENTE, null), operatorHeaders),
                LoteResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        assertEquals(HttpStatus.FORBIDDEN, anonymous.exchange("http://localhost:" + port + "/lotes/" + created.getBody().id(),
                HttpMethod.DELETE, new HttpEntity<>(operatorHeaders), String.class).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, http.exchange("/lotes/" + created.getBody().id(),
                HttpMethod.DELETE, HttpEntity.EMPTY, Void.class).getStatusCode());
    }

    @Test
    void p03Devuelve404ConErrorConsistente() {
        var response = http.getForEntity("/lotes/999", ApiErrorResponse.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("No existe el lote con id 999", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
        assertFalse(response.getBody().timestamp().isAfter(Instant.now()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "0", "-1", "2147483648", "1.5"})
    void p04Devuelve400ParaIdentificadoresInvalidos(String id) {
        var response = http.getForEntity("/lotes/" + id, ApiErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertTrue(response.getBody().message().contains("entero mayor que cero"));
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void completaCrudConPersistenciaDecimalYConteo() {
        var original = new GuardarLoteRequest(1, new BigDecimal("12.375"), EstadoLote.PENDIENTE, null);
        var created = http.postForEntity("/lotes", original, LoteResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        String path = "/lotes/" + created.getBody().id();
        try {
            assertEquals(path, created.getHeaders().getLocation().toString());
            assertEquals(21, http.getForObject("/lotes/pendientes/conteo", ConteoPendientesResponse.class).pendientes());
            var stored = http.getForObject(path, LoteResponse.class);
            assertEquals(original.pesoKg(), stored.pesoKg());
            var update = new GuardarLoteRequest(2, new BigDecimal("20.125"), EstadoLote.LIQUIDADO, stored.version());
            var updated = http.exchange(path, HttpMethod.PUT, new HttpEntity<>(update), LoteResponse.class);
            assertEquals(HttpStatus.OK, updated.getStatusCode());
            assertEquals(2, http.getForObject(path, LoteResponse.class).socioId());
            var stale = new GuardarLoteRequest(2, new BigDecimal("21.125"), EstadoLote.LIQUIDADO, stored.version());
            assertEquals(HttpStatus.CONFLICT, http.exchange(path, HttpMethod.PUT, new HttpEntity<>(stale), ApiErrorResponse.class).getStatusCode());
            var history = http.getForObject(path + "/historial", EventoAuditoria[].class);
            assertNotNull(history);
            assertTrue(java.util.Arrays.stream(history).anyMatch(event -> event.accion().equals("CREAR")));
            assertTrue(java.util.Arrays.stream(history).anyMatch(event -> event.accion().equals("ACTUALIZAR")));
            assertEquals(20, http.getForObject("/lotes/pendientes/conteo", ConteoPendientesResponse.class).pendientes());
        } finally {
            assertEquals(HttpStatus.NO_CONTENT, http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class).getStatusCode());
        }
        assertEquals(HttpStatus.NOT_FOUND, http.getForEntity(path, ApiErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, ApiErrorResponse.class).getStatusCode());
    }

    @Test
    void actualizarInexistenteResponde404() {
        var request = new GuardarLoteRequest(1, BigDecimal.ONE, EstadoLote.PENDIENTE, 1);
        assertEquals(HttpStatus.NOT_FOUND, http.exchange("/lotes/999", HttpMethod.PUT,
                new HttpEntity<>(request), ApiErrorResponse.class).getStatusCode());
    }

    @Test
    void adaptadorCompartidoEscribeYLeeContraPostgresReal() {
        var config = new ApiClientConfig(URI.create("http://127.0.0.1:" + port), Duration.ofSeconds(5));
        try (var adapter = HttpLoteQueryAdapter.create(config)) {
            adapter.setCredentials("admin", adminPassword);
            var created = adapter.create(new DatosLote(1, new BigDecimal("31.125"), EstadoLote.PENDIENTE));
            try {
                assertTrue(adapter.findAll().stream().anyMatch(lote -> lote.id().equals(created.id())));
                var result = adapter.update(created.id(), new DatosLote(2, new BigDecimal("40.500"), EstadoLote.LIQUIDADO));
                assertEquals(2, result.orElseThrow().socioId());
                assertEquals(2, http.getForObject("/lotes/" + created.id(), LoteResponse.class).socioId());
            } finally {
                assertTrue(adapter.deleteById(created.id()));
            }
            assertFalse(adapter.deleteById(created.id()));
            assertTrue(adapter.update(created.id(), new DatosLote(1, BigDecimal.ONE, EstadoLote.PENDIENTE)).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{", "{\"socioId\":null,\"pesoKg\":1,\"estado\":\"PENDIENTE\"}",
            "{\"socioId\":1,\"pesoKg\":0,\"estado\":\"PENDIENTE\"}",
            "{\"socioId\":1,\"pesoKg\":1.1234,\"estado\":\"PENDIENTE\"}",
            "{\"socioId\":1,\"pesoKg\":1,\"estado\":\"OTRO\"}",
            "{\"socioId\":1,\"pesoKg\":1,\"estado\":\"PENDIENTE\",\"id\":100}"})
    void rechazaEscriturasInvalidasConErrorConsistente(String json) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var response = http.postForEntity("/lotes", new HttpEntity<>(json, headers), ApiErrorResponse.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertNotNull(response.getBody().timestamp());
    }
}
