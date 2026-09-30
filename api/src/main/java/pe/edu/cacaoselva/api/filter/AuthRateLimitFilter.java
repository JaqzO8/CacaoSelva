package pe.edu.cacaoselva.api.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.cacaoselva.api.dto.ApiErrorResponse;

/** Límite por IP para login y registro; los datos de negocio siempre viven en PostgreSQL. */
public final class AuthRateLimitFilter extends OncePerRequestFilter {
    private record Window(long expiresAt, int attempts) { }
    private final Map<String, Window> windows = new HashMap<>();
    private final ObjectMapper mapper;

    public AuthRateLimitFilter(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!request.getMethod().equals("POST") || !(path.equals("/auth/login") || path.equals("/auth/register"))) {
            chain.doFilter(request, response);
            return;
        }
        long now = System.currentTimeMillis();
        boolean registration = path.equals("/auth/register");
        long duration = registration ? 3_600_000L : 60_000L;
        int limit = registration ? 5 : 30;
        long retryAfter = 0;
        synchronized (windows) {
            windows.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
            String key = request.getRemoteAddr() + path;
            Window previous = windows.get(key);
            if (previous == null && windows.size() >= 5000) {
                retryAfter = 60;
            } else if (previous != null && previous.attempts() >= limit) {
                retryAfter = Math.max(1, (previous.expiresAt() - now + 999) / 1000);
            } else {
                windows.put(key, previous == null ? new Window(now + duration, 1)
                        : new Window(previous.expiresAt(), previous.attempts() + 1));
            }
        }
        if (retryAfter > 0) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setHeader("Cache-Control", "no-store");
            mapper.writeValue(response.getOutputStream(), new ApiErrorResponse(429,
                    "Demasiados intentos. Espera antes de volver a intentarlo.", Instant.now()));
            return;
        }
        chain.doFilter(request, response);
    }
}
