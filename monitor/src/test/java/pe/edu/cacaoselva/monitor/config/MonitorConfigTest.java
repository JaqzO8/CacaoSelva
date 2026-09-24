package pe.edu.cacaoselva.monitor.config;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonitorConfigTest {
    @Test
    void admiteIntervaloDeUnSegundo() {
        assertEquals(Duration.ofSeconds(1), new MonitorConfig(Duration.ofSeconds(1)).interval());
    }

    @Test
    void rechazaIntervalosQueSaturanElServidor() {
        assertThrows(IllegalArgumentException.class, () -> new MonitorConfig(Duration.ofMillis(999)));
        assertThrows(IllegalArgumentException.class, () -> new MonitorConfig(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new MonitorConfig(null));
    }
}
