package pe.edu.cacaoselva.api;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoteApiIT {
    @Autowired private TestRestTemplate http;
    @LocalServerPort private int port;

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
    void p02DevuelveAna() {
        var response = http.getForEntity("/lotes/1", LoteResponse.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Ana", response.getBody().socio());
        assertEquals(1, response.getBody().id());
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
        var original = new GuardarLoteRequest("Socio de prueba", new BigDecimal("12.375"), EstadoLote.PENDIENTE);
        var created = http.postForEntity("/lotes", original, LoteResponse.class);
        assertEquals(HttpStatus.CREATED, created.getStatusCode());
        assertNotNull(created.getBody());
        String path = "/lotes/" + created.getBody().id();
        try {
            assertEquals(path, created.getHeaders().getLocation().toString());
            assertEquals(21, http.getForObject("/lotes/pendientes/conteo", ConteoPendientesResponse.class).pendientes());
            var stored = http.getForObject(path, LoteResponse.class);
            assertEquals(original.pesoKg(), stored.pesoKg());
            var update = new GuardarLoteRequest("Nombre corregido", new BigDecimal("20.125"), EstadoLote.LIQUIDADO);
            var updated = http.exchange(path, HttpMethod.PUT, new HttpEntity<>(update), LoteResponse.class);
            assertEquals(HttpStatus.OK, updated.getStatusCode());
            assertEquals("Nombre corregido", http.getForObject(path, LoteResponse.class).socio());
            assertEquals(20, http.getForObject("/lotes/pendientes/conteo", ConteoPendientesResponse.class).pendientes());
        } finally {
            assertEquals(HttpStatus.NO_CONTENT, http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class).getStatusCode());
        }
        assertEquals(HttpStatus.NOT_FOUND, http.getForEntity(path, ApiErrorResponse.class).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, http.exchange(path, HttpMethod.DELETE, HttpEntity.EMPTY, ApiErrorResponse.class).getStatusCode());
    }

    @Test
    void actualizarInexistenteResponde404() {
        var request = new GuardarLoteRequest("Prueba", BigDecimal.ONE, EstadoLote.PENDIENTE);
        assertEquals(HttpStatus.NOT_FOUND, http.exchange("/lotes/999", HttpMethod.PUT,
                new HttpEntity<>(request), ApiErrorResponse.class).getStatusCode());
    }

    @Test
    void adaptadorCompartidoEscribeYLeeContraPostgresReal() {
        var config = new ApiClientConfig(URI.create("http://127.0.0.1:" + port), Duration.ofSeconds(5));
        try (var adapter = HttpLoteQueryAdapter.create(config)) {
            var created = adapter.create(new DatosLote("Prueba HTTP", new BigDecimal("31.125"), EstadoLote.PENDIENTE));
            try {
                assertTrue(adapter.findAll().stream().anyMatch(lote -> lote.id().equals(created.id())));
                var result = adapter.update(created.id(), new DatosLote("Editado HTTP", new BigDecimal("40.500"), EstadoLote.LIQUIDADO));
                assertEquals("Editado HTTP", result.orElseThrow().socio());
                assertEquals("Editado HTTP", http.getForObject("/lotes/" + created.id(), LoteResponse.class).socio());
            } finally {
                assertTrue(adapter.deleteById(created.id()));
            }
            assertFalse(adapter.deleteById(created.id()));
            assertTrue(adapter.update(created.id(), new DatosLote("No existe", BigDecimal.ONE, EstadoLote.PENDIENTE)).isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{", "{\"socio\":\"\",\"pesoKg\":1,\"estado\":\"PENDIENTE\"}",
            "{\"socio\":\"Ana\",\"pesoKg\":0,\"estado\":\"PENDIENTE\"}",
            "{\"socio\":\"Ana\",\"pesoKg\":1.1234,\"estado\":\"PENDIENTE\"}",
            "{\"socio\":\"Ana\",\"pesoKg\":1,\"estado\":\"OTRO\"}",
            "{\"socio\":\"Ana\",\"pesoKg\":1,\"estado\":\"PENDIENTE\",\"id\":100}"})
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
