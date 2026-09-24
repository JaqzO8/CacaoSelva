package pe.edu.cacaoselva.infrastructure.config;

import java.net.URI;
import java.time.Duration;

public record ApiClientConfig(URI baseUrl, Duration timeout) {
    private static final String DEFAULT_BASE_URL = "http://localhost:5080";
    private static final String DEFAULT_TIMEOUT_SECONDS = "5";

    public ApiClientConfig {
        if (baseUrl == null || baseUrl.getHost() == null
                || !("http".equalsIgnoreCase(baseUrl.getScheme())
                || "https".equalsIgnoreCase(baseUrl.getScheme()))
                || baseUrl.getQuery() != null || baseUrl.getFragment() != null
                || baseUrl.getUserInfo() != null) {
            throw new IllegalArgumentException("La URL de la API debe ser HTTP(S), sin credenciales, query ni fragmento.");
        }
        if (timeout == null || timeout.toMillis() <= 0) {
            throw new IllegalArgumentException("El timeout HTTP debe ser positivo (al menos 1 ms).");
        }
    }

    public static ApiClientConfig fromEnvironment() {
        String baseUrl = setting("cacaoselva.api.baseUrl", "CACAOSELVA_API_BASE_URL", DEFAULT_BASE_URL);
        String seconds = setting("cacaoselva.api.timeoutSeconds", "CACAOSELVA_API_TIMEOUT_SECONDS",
                DEFAULT_TIMEOUT_SECONDS);
        return new ApiClientConfig(URI.create(baseUrl), Duration.ofSeconds(Long.parseLong(seconds)));
    }

    public URI lotesUri() {
        String url = baseUrl.toString();
        return URI.create(url.endsWith("/") ? url : url + "/").resolve("lotes");
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) {
            value = System.getenv(environment);
        }
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
