package pe.edu.cacaoselva.api.filter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.cacaoselva.api.dto.ApiErrorResponse;
import pe.edu.cacaoselva.application.port.TokenPort;
import pe.edu.cacaoselva.domain.model.Rol;

/** Filtro HTTP que intercepta y valida el JWT en la cabecera Authorization (Fase 3). */
public class JwtAuthFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final Set<String> PUBLIC_PATHS = Set.of("/", "/index.html", "/assets/app.js",
            "/assets/app.css", "/favicon.svg", "/auth/login", "/auth/register", "/actuator/health", "/actuator/info");

    private final TokenPort tokenPort;
    private final ObjectMapper mapper;
    private final boolean enabled;

    public JwtAuthFilter(TokenPort tokenPort, ObjectMapper mapper, boolean enabled) {
        this.tokenPort = tokenPort;
        this.mapper = mapper;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        response.setHeader("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; object-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
        response.setHeader("Cache-Control", "no-store");
        if (!enabled || isPublicPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendError(response, 401, "Se requiere autenticación. Envíe el token JWT en Authorization: Bearer <token>.");
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            String usuario = tokenPort.validarYObtenerUsuario(token);
            Rol rol = tokenPort.validarYObtenerRol(token);
            request.setAttribute("usuario", usuario);
            request.setAttribute("rol", rol);

            if (!tienePermiso(request.getMethod(), request.getRequestURI(), rol)) {
                sendError(response, 403, "No tiene permisos para esta operación.");
                return;
            }

            filterChain.doFilter(request, response);
        } catch (SecurityException error) {
            sendError(response, 401, "Token JWT inválido o expirado.");
        }
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        return PUBLIC_PATHS.stream().anyMatch(publicPath -> path.equals(publicPath)
                || (publicPath.equals("/actuator/health") && path.startsWith(publicPath + "/")));
    }

    private boolean tienePermiso(String method, String path, Rol rol) {
        if (rol == Rol.ADMIN) return true;
        if (method.equals("GET") && path.equals("/socios/catalogo")) return true;
        if (path.startsWith("/socios") || path.startsWith("/auth/usuarios")) return false;
        if (rol == Rol.CONSULTOR) return method.equals("GET")
                && (path.equals("/lotes") || path.matches("/lotes/\\d+")
                || path.equals("/lotes/pendientes/conteo"));
        if (rol == Rol.OPERADOR) return method.equals("GET")
                || method.equals("POST") && path.equals("/lotes")
                || method.equals("PUT") && path.matches("/lotes/\\d+");
        return false;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), new ApiErrorResponse(status, message, Instant.now()));
    }
}
