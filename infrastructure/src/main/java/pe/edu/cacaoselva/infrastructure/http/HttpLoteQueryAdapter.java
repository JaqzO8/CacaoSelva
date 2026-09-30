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
import pe.edu.cacaoselva.domain.exception.ConflictoEdicionException;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.dto.GuardarSocioCommand;
import pe.edu.cacaoselva.application.dto.GuardarLoteCommand;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.DatosSocio;
import pe.edu.cacaoselva.domain.model.Lote;
import pe.edu.cacaoselva.domain.model.Socio;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;

/** Adaptador bloqueante: sus consumidores deben ejecutarlo fuera del hilo gráfico. */
public final class HttpLoteQueryAdapter implements LoteQueryPort, LoteWritePort, AutoCloseable {
    private static final int HTTP_OK = 200;
    private static final TypeReference<List<HttpLoteResponse>> RESPONSE_TYPE = new TypeReference<>() { };
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final ApiClientConfig config;
    private volatile String bearerToken;
    private volatile String usuarioSesion;
    private volatile String contrasenaSesion;
    private volatile long tokenValidoHasta;

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

    public void setBearerToken(String token) {
        bearerToken = token == null || token.isBlank() ? null : token;
        tokenValidoHasta = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(55);
    }

    public void setCredentials(String usuario, String contrasena) {
        usuarioSesion = usuario;
        contrasenaSesion = contrasena;
        bearerToken = null;
        tokenValidoHasta = 0;
    }

    public String autenticar(String usuario, String contrasena) {
        try {
            var payload = mapper.writeValueAsString(new LoginBody(usuario, contrasena));
            var request = HttpRequest.newBuilder(config.authUri())
                    .timeout(config.timeout()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8)).build();
            var response = send(request);
            if (response.statusCode() != HTTP_OK) {
                throw new ApiNoDisponibleException(response.statusCode() == 401
                        ? "Usuario o contraseña incorrectos."
                        : "La API respondió con HTTP " + response.statusCode() + " al iniciar sesión.");
            }
            var token = mapper.readValue(response.body(), LoginToken.class);
            if (token == null || token.token() == null || token.token().isBlank()) {
                throw new IllegalArgumentException("No se recibió un token.");
            }
            bearerToken = token.token();
            tokenValidoHasta = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(55);
            return token.token();
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new ApiNoDisponibleException("No se pudo interpretar el inicio de sesión.", error);
        }
    }

    public boolean apiDisponible() {
        try {
            var request = HttpRequest.newBuilder(config.healthUri()).timeout(config.timeout()).GET().build();
            return send(request).statusCode() == HTTP_OK;
        } catch (RuntimeException error) {
            return false;
        }
    }

    @Override
    public List<Lote> findAll() {
        HttpRequest request = request(config.lotesUri().toString()).GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != HTTP_OK) {
            throw new ApiNoDisponibleException("La API respondió con HTTP " + response.statusCode() + ".");
        }
        return decode(response.body());
    }

    @Override
    public PaginaLotes findAll(FiltroLotes filtro) {
        var uri = config.lotesUri() + "?page=" + filtro.page() + "&size=" + filtro.size()
                + (filtro.estado() == null ? "" : "&estado=" + filtro.estado().name())
                + (filtro.socioId() == null ? "" : "&socioId=" + filtro.socioId())
                + (filtro.socio() == null ? "" : "&socio=" + java.net.URLEncoder.encode(filtro.socio(), StandardCharsets.UTF_8));
        var request = request(uri).GET().build();
        var response = send(request);
        requireStatus(response, HTTP_OK);
        try {
            var page = mapper.readValue(response.body(), HttpPaginaLotesResponse.class);
            if (page == null || page.content() == null) throw new IllegalArgumentException("Página inválida.");
            var content = page.content().stream().map(HttpLoteResponse::toDomain).map(LoteDto::from).toList();
            return new PaginaLotes(content, page.page(), page.size(), page.totalElements(), page.totalPages());
        } catch (JsonProcessingException | IllegalArgumentException error) {
            throw new ApiNoDisponibleException("La API devolvió una página de lotes inválida.", error);
        }
    }

    @Override
    public Lote create(DatosLote datos) {
        var response = send(writeRequest("POST", "", new GuardarLoteCommand(datos.socioId(), datos.pesoKg(), datos.estado())));
        requireStatus(response, 201);
        return decodeLote(response.body());
    }

    @Override
    public Optional<Lote> update(Integer id, DatosLote datos, Integer version) {
        Integer expectedVersion = version == null ? obtenerVersion(id).orElse(null) : version;
        if (expectedVersion == null) return Optional.empty();
        var response = send(writeRequest("PUT", "/" + id,
                new GuardarLoteCommand(datos.socioId(), datos.pesoKg(), datos.estado(), expectedVersion)));
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() == 409) throw new ConflictoEdicionException(id);
        requireStatus(response, HTTP_OK);
        return Optional.of(decodeLote(response.body()));
    }

    private Optional<Integer> obtenerVersion(Integer id) {
        var response = send(request(config.lotesUri() + "/" + id).GET().build());
        if (response.statusCode() == 404) return Optional.empty();
        requireStatus(response, HTTP_OK);
        return Optional.of(decodeLote(response.body()).version());
    }

    @Override
    public boolean deleteById(Integer id) {
        var request = request(config.lotesUri() + "/" + id).DELETE().build();
        var response = send(request);
        if (response.statusCode() == 404) {
            return false;
        }
        requireStatus(response, 204);
        return true;
    }

    private HttpRequest writeRequest(String method, String suffix, Object body) {
        try {
            return request(config.lotesUri() + suffix).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), StandardCharsets.UTF_8)).build();
        } catch (JsonProcessingException error) {
            throw new ApiNoDisponibleException("No se pudo preparar la petición de lotes.", error);
        }
    }

    public List<Socio> listarSocios() {
        var response = send(request(config.sociosUri().toString()).GET().build());
        requireStatus(response, HTTP_OK);
        try {
            List<HttpSocioResponse> records = mapper.readValue(response.body(), new TypeReference<>() { });
            return records.stream().map(HttpSocioResponse::toDomain).toList();
        } catch (JsonProcessingException | RuntimeException error) {
            throw new ApiNoDisponibleException("La API devolvió socios inválidos.", error);
        }
    }

    public List<pe.edu.cacaoselva.application.dto.SocioResumenDto> listarCatalogoSocios() {
        var response = send(request(config.sociosUri() + "/catalogo").GET().build());
        requireStatus(response, HTTP_OK);
        try {
            return mapper.readValue(response.body(), new TypeReference<>() { });
        } catch (JsonProcessingException error) {
            throw new ApiNoDisponibleException("La API devolvió un catálogo inválido.", error);
        }
    }

    public Optional<Socio> buscarSocioPorDni(String dni) {
        var uri = config.sociosUri() + "/buscar?dni=" + java.net.URLEncoder.encode(dni, StandardCharsets.UTF_8);
        var response = send(request(uri).GET().build());
        if (response.statusCode() == 404) return Optional.empty();
        requireStatus(response, HTTP_OK);
        try { return Optional.of(mapper.readValue(response.body(), HttpSocioResponse.class).toDomain()); }
        catch (JsonProcessingException | RuntimeException error) { throw new ApiNoDisponibleException("La API devolvió un socio inválido.", error); }
    }

    public Socio create(DatosSocio datos) {
        var response = send(writeRequestForSocio(datos));
        requireStatus(response, 201);
        try { return mapper.readValue(response.body(), HttpSocioResponse.class).toDomain(); }
        catch (JsonProcessingException | RuntimeException error) { throw new ApiNoDisponibleException("La API devolvió un socio inválido.", error); }
    }

    private HttpRequest writeRequestForSocio(DatosSocio datos) {
        try {
            return request(config.sociosUri().toString()).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(new GuardarSocioCommand(
                            datos.dni(), datos.nombre(), datos.zona(), datos.telefono())), StandardCharsets.UTF_8)).build();
        } catch (JsonProcessingException error) { throw new ApiNoDisponibleException("No se pudo preparar la petición de socios.", error); }
    }

    private HttpRequest.Builder request(String uri) {
        renovarTokenSiNecesario();
        var builder = HttpRequest.newBuilder(java.net.URI.create(uri)).timeout(config.timeout())
                .header("Accept", "application/json");
        if (bearerToken != null) builder.header("Authorization", "Bearer " + bearerToken);
        return builder;
    }

    private synchronized void renovarTokenSiNecesario() {
        if (usuarioSesion != null && contrasenaSesion != null
                && (bearerToken == null || System.currentTimeMillis() >= tokenValidoHasta)) {
            autenticar(usuarioSesion, contrasenaSesion);
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

    private record LoginBody(String usuario, String contrasena) { }
    private record LoginToken(String token) { }
}
