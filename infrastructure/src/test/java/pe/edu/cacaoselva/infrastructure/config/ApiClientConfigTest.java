package pe.edu.cacaoselva.infrastructure.config;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientConfigTest {
    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5080/api", "http://localhost:5080/api/"})
    void conservaElPrefijoDeLaUrl(String url) {
        assertEquals(URI.create("http://localhost:5080/api/lotes"),
                new ApiClientConfig(URI.create(url), Duration.ofSeconds(5)).lotesUri());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/relative", "ftp://localhost", "http://localhost?x=1",
            "http://localhost#fragment", "http://user:pass@localhost"})
    void rechazaUrlsInvalidas(String url) {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiClientConfig(URI.create(url), Duration.ofSeconds(5)));
    }

    @Test
    void rechazaTimeoutCero() {
        assertThrows(IllegalArgumentException.class,
                () -> new ApiClientConfig(URI.create("http://localhost"), Duration.ZERO));
    }
}
