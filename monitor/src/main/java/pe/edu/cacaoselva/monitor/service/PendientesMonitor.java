package pe.edu.cacaoselva.monitor.service;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.cacaoselva.application.exception.ApiNoDisponibleException;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;

public final class PendientesMonitor implements AutoCloseable {
    public static final Duration DEFAULT_INTERVAL = Duration.ofSeconds(10);
    private static final Logger LOGGER = LoggerFactory.getLogger(PendientesMonitor.class);
    private final ContarLotesPendientesUseCase contarPendientes;
    private final ScheduledExecutorService scheduler;
    private final Duration interval;
    private final BooleanSupplier apiDisponible;
    private ScheduledFuture<?> task;
    private boolean closed;
    private int consecutiveFailures;
    private Long previousCount;
    private Boolean lastHealth;

    public PendientesMonitor(ContarLotesPendientesUseCase contarPendientes,
                             ScheduledExecutorService scheduler, Duration interval) {
        this(contarPendientes, scheduler, interval, () -> true);
    }

    public PendientesMonitor(ContarLotesPendientesUseCase contarPendientes,
                             ScheduledExecutorService scheduler, Duration interval,
                             BooleanSupplier apiDisponible) {
        this.contarPendientes = Objects.requireNonNull(contarPendientes);
        this.scheduler = Objects.requireNonNull(scheduler);
        this.apiDisponible = Objects.requireNonNull(apiDisponible);
        if (interval == null || interval.toMillis() <= 0) {
            throw new IllegalArgumentException("El intervalo del monitor debe ser positivo (al menos 1 ms).");
        }
        this.interval = interval;
    }

    public synchronized void start() {
        if (closed) {
            throw new IllegalStateException("El monitor está cerrado.");
        }
        if (task != null) {
            throw new IllegalStateException("El monitor ya está iniciado.");
        }
        task = scheduler.scheduleWithFixedDelay(this::consultar, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void consultar() {
        try {
            boolean healthy = apiDisponible.getAsBoolean();
            if (lastHealth == null || lastHealth != healthy) {
                LOGGER.info("Estado de API: {}.", healthy ? "UP" : "DOWN");
                lastHealth = healthy;
            }
            if (!healthy) throw new ApiNoDisponibleException("El health check de la API indica que no está disponible.");
            long count = contarPendientes.execute();
            if (consecutiveFailures > 0) {
                LOGGER.info("Conexión recuperada tras {} fallos consecutivos.", consecutiveFailures);
            }
            if (previousCount != null && previousCount != count) {
                LOGGER.info("Cambio de pendientes: {} -> {}", previousCount, count);
            }
            consecutiveFailures = 0;
            previousCount = count;
            LOGGER.info("Pendientes: {}", count);
        } catch (ApiNoDisponibleException error) {
            if (!Thread.currentThread().isInterrupted()) {
                consecutiveFailures++;
                LOGGER.warn("{} Fallos consecutivos: {}. Se reintentará en {} ms.",
                        error.getMessage(), consecutiveFailures, interval.toMillis());
            }
        } catch (RuntimeException error) {
            consecutiveFailures++;
            // Una excepción sin capturar cancelaría todas las futuras ejecuciones del scheduler.
            LOGGER.error("Fallo inesperado al consultar los lotes; el monitor volverá a intentar.", error);
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        scheduler.shutdownNow();
    }
}
