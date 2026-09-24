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
        var scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("cacaoselva-monitor").factory());
        var monitor = new PendientesMonitor(new ContarLotesPendientesUseCase(adapter),
                scheduler, monitorConfig.interval());
        LoggerFactory.getLogger(CacaoSelvaMonitorApplication.class).info(
                "Monitor iniciado. API: {}. Intervalo: {} segundos. Timeout: {} segundos.",
                clientConfig.baseUrl(), monitorConfig.interval().toSeconds(), clientConfig.timeout().toSeconds());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            monitor.close();
            adapter.close();
        }, "cacaoselva-monitor-shutdown"));
        monitor.start();
    }
}
