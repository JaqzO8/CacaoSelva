package pe.edu.cacaoselva.monitor;

import java.util.concurrent.Executors;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;
import pe.edu.cacaoselva.infrastructure.config.ApiClientConfig;
import pe.edu.cacaoselva.infrastructure.http.HttpLoteQueryAdapter;
import pe.edu.cacaoselva.monitor.service.PendientesMonitor;
import pe.edu.cacaoselva.monitor.config.MonitorConfig;
import org.slf4j.LoggerFactory;

public final class CacaoSelvaMonitorApplication {
    private CacaoSelvaMonitorApplication() {
    }

    public static void main(String[] args) {
        var clientConfig = ApiClientConfig.fromEnvironment();
        var monitorConfig = MonitorConfig.fromEnvironment();
        var adapter = HttpLoteQueryAdapter.create(clientConfig);
        String token = setting("cacaoselva.api.token", "CACAOSELVA_API_TOKEN");
        String user = setting("cacaoselva.api.username", "CACAOSELVA_API_USERNAME");
        String password = setting("cacaoselva.api.password", "CACAOSELVA_API_PASSWORD");
        if (token != null) adapter.setBearerToken(token);
        else if (user != null && password != null) adapter.setCredentials(user, password);
        var scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("cacaoselva-monitor").factory());
        var monitor = new PendientesMonitor(new ContarLotesPendientesUseCase(adapter),
                scheduler, monitorConfig.interval(), adapter::apiDisponible);
        LoggerFactory.getLogger(CacaoSelvaMonitorApplication.class).info(
                "Monitor iniciado. API: {}. Intervalo: {} segundos. Timeout: {} segundos.",
                clientConfig.baseUrl(), monitorConfig.interval().toSeconds(), clientConfig.timeout().toSeconds());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            monitor.close();
            adapter.close();
        }, "cacaoselva-monitor-shutdown"));
        monitor.start();
    }

    private static String setting(String property, String environment) {
        String value = System.getProperty(property);
        if (value == null || value.isBlank()) value = System.getenv(environment);
        return value == null || value.isBlank() ? null : value;
    }
}
