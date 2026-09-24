package pe.edu.cacaoselva.monitor.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import pe.edu.cacaoselva.application.exception.ApiNoDisponibleException;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PendientesMonitorTest {
    @Test
    void programaConsultaInmediataYEsperaDiezSegundosTrasCadaEjecucion() {
        var scheduler = mock(ScheduledExecutorService.class);
        var useCase = new ContarLotesPendientesUseCase(List::of);
        try (var monitor = new PendientesMonitor(useCase, scheduler, PendientesMonitor.DEFAULT_INTERVAL)) {
            monitor.start();
            verify(scheduler).scheduleWithFixedDelay(any(Runnable.class), eq(0L), eq(10000L),
                    eq(TimeUnit.MILLISECONDS));
        }
        verify(scheduler).shutdownNow();
    }

    @Test
    void sigueEjecutandoseTrasCaidaYRecuperacionDeLaApi() throws InterruptedException {
        AtomicInteger attempts = new AtomicInteger();
        CountDownLatch recovered = new CountDownLatch(2);
        LoteQueryPort port = () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new ApiNoDisponibleException("API detenida");
            }
            recovered.countDown();
            return List.of();
        };
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        try (var monitor = new PendientesMonitor(new ContarLotesPendientesUseCase(port),
                scheduler, Duration.ofMillis(20))) {
            monitor.start();
            assertTrue(recovered.await(3, TimeUnit.SECONDS), "El monitor debe continuar después de una falla.");
            assertTrue(attempts.get() >= 3);
        }
        assertTrue(scheduler.awaitTermination(1, TimeUnit.SECONDS));
    }

    @Test
    void rechazaIntervalosNoPositivos() {
        assertThrows(IllegalArgumentException.class, () -> new PendientesMonitor(
                new ContarLotesPendientesUseCase(List::of), mock(ScheduledExecutorService.class), Duration.ZERO));
    }

    @Test
    void evitaProgramarDosVecesElMismoMonitor() {
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        try (var monitor = new PendientesMonitor(new ContarLotesPendientesUseCase(List::of),
                scheduler, Duration.ofSeconds(10))) {
            monitor.start();
            assertThrows(IllegalStateException.class, monitor::start);
        }
    }

    @Test
    void impideReiniciarUnMonitorCerrado() {
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        var monitor = new PendientesMonitor(new ContarLotesPendientesUseCase(List::of), scheduler, Duration.ofSeconds(1));
        monitor.close();
        assertThrows(IllegalStateException.class, monitor::start);
        assertTrue(scheduler.isShutdown());
    }
}
