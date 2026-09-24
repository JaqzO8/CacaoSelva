package pe.edu.cacaoselva.infrastructure.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import pe.edu.cacaoselva.application.exception.ApiNoDisponibleException;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;

/** Adaptador bloqueante: sus consumidores deben ejecutarlo fuera del hilo gráfico. */
public final class HttpLoteQueryAdapter implements LoteQueryPort, LoteWritePort, AutoCloseable {
    private static final int HTTP_OK = 200;
    private static final TypeReference<List<HttpLoteResponse>> RESPONSE_TYPE = new TypeReference<>() { };
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final ApiClientConfig config;

    public HttpLoteQueryAdapter(HttpClient client, ObjectMapper mapper, ApiClientConfig config) {
        this.client = Objects.requireNonNull(client);
        this.mapper = Objects.requireNonNull(mapper);
        this.config = Objects.requireNonNull(config);
    }

    public static HttpLoteQueryAdapter create(ApiClientConfig config) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(config.timeout()).build();
        ObjectMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();
        return new HttpLoteQueryAdapter(client, mapper, config);
    }

    @Override
    public List<Lote> findAll() {
        HttpRequest request = HttpRequest.newBuilder(config.lotesUri())
                .timeout(config.timeout())
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != HTTP_OK) {
            throw new ApiNoDisponibleException("La API respondió con HTTP " + response.statusCode() + ".");
        }
        return decode(response.body());
    }

    @Override
    public Lote create(DatosLote datos) {
        var response = send(writeRequest("POST", "", datos));
        requireStatus(response, 201);
        return decodeLote(response.body());
    }

    @Override
    public Optional<Lote> update(Integer id, DatosLote datos) {
        var response = send(writeRequest("PUT", "/" + id, datos));
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        requireStatus(response, HTTP_OK);
        return Optional.of(decodeLote(response.body()));
    }

    @Override
    public boolean deleteById(Integer id) {
        var request = HttpRequest.newBuilder(java.net.URI.create(config.lotesUri() + "/" + id))
                .timeout(config.timeout()).DELETE().build();
        var response = send(request);
        if (response.statusCode() == 404) {
            return false;
        }
        requireStatus(response, 204);
        return true;
    }

    private HttpRequest writeRequest(String method, String suffix, DatosLote datos) {
        try {
            return HttpRequest.newBuilder(java.net.URI.create(config.lotesUri() + suffix))
                    .timeout(config.timeout()).header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(datos), StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException error) {
            throw new ApiNoDisponibleException("No se pudo preparar la petición de lotes.", error);
        }
    }

    private void requireStatus(HttpResponse<String> response, int expected) {
        if (response.statusCode() != expected) {
            throw new ApiNoDisponibleException("La API respondió con HTTP " + response.statusCode() + ".");
        }
    }

    private Lote decodeLote(String body) {
        try {
            HttpLoteResponse record = mapper.readValue(body, HttpLoteResponse.class);
            if (record == null) {
                throw new IllegalArgumentException("Se esperaba un lote.");
            }
            return record.toDomain();
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new ApiNoDisponibleException("La API devolvió un lote inválido.", error);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        var pending = client.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        try {
            // Este límite incluye recibir el cuerpo completo, además de la conexión y cabeceras.
            return pending.get(config.timeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException error) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new ApiNoDisponibleException("La consulta a la API fue interrumpida.", error);
        } catch (TimeoutException error) {
            pending.cancel(true);
            throw new ApiNoDisponibleException("La API no respondió dentro del tiempo máximo.", error);
        } catch (ExecutionException error) {
            throw new ApiNoDisponibleException("No se pudo conectar con la API.", error.getCause());
        }
    }

    private List<Lote> decode(String body) {
        try {
            List<HttpLoteResponse> records = mapper.readValue(body, RESPONSE_TYPE);
            if (records == null || records.stream().anyMatch(Objects::isNull)) {
                throw new IllegalArgumentException("La respuesta debe ser una lista de lotes sin elementos nulos.");
            }
            return records.stream().map(HttpLoteResponse::toDomain).toList();
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new ApiNoDisponibleException("La API devolvió datos de lotes inválidos.", error);
        }
    }

    @Override
    public void close() {
        client.shutdownNow();
    }
}
