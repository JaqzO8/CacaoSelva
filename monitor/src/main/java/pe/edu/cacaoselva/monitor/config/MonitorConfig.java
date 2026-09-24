package pe.edu.cacaoselva.monitor.config;

import java.time.Duration;
import pe.edu.cacaoselva.monitor.service.PendientesMonitor;

public record MonitorConfig(Duration interval) {
    public MonitorConfig {
        if (interval == null || interval.compareTo(Duration.ofSeconds(1)) < 0) {
            throw new IllegalArgumentException("El intervalo del monitor debe ser de al menos 1 segundo.");
        }
    }

    public static MonitorConfig fromEnvironment() {
        String value = System.getProperty("cacaoselva.monitor.intervalSeconds",
                System.getenv("CACAOSELVA_MONITOR_INTERVAL_SECONDS"));
        return new MonitorConfig(value == null || value.isBlank() ? PendientesMonitor.DEFAULT_INTERVAL
                : Duration.ofSeconds(Long.parseLong(value)));
    }
}
