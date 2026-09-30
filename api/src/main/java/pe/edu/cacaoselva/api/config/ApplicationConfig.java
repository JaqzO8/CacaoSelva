package pe.edu.cacaoselva.api.config;

import javax.sql.DataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import pe.edu.cacaoselva.api.filter.JwtAuthFilter;
import pe.edu.cacaoselva.application.port.AuditoriaPort;
import pe.edu.cacaoselva.application.port.LoteQueryPort;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.application.port.PasswordPort;
import pe.edu.cacaoselva.application.port.SocioRepository;
import pe.edu.cacaoselva.application.port.SocioWritePort;
import pe.edu.cacaoselva.application.port.TokenPort;
import pe.edu.cacaoselva.application.port.UsuarioRepository;
import pe.edu.cacaoselva.application.port.UsuarioWritePort;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.AutenticarUsuarioUseCase;
import pe.edu.cacaoselva.application.usecase.BuscarLotePorIdUseCase;
import pe.edu.cacaoselva.application.usecase.BuscarSocioPorDniUseCase;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.CrearSocioUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ListarLotesUseCase;
import pe.edu.cacaoselva.application.usecase.ListarLotesPaginadoUseCase;
import pe.edu.cacaoselva.application.usecase.ListarSociosUseCase;
import pe.edu.cacaoselva.application.usecase.RegistrarUsuarioUseCase;
import pe.edu.cacaoselva.infrastructure.repository.JdbcLoteRepository;
import pe.edu.cacaoselva.infrastructure.repository.JdbcSocioRepository;
import pe.edu.cacaoselva.infrastructure.repository.JdbcUsuarioRepository;
import pe.edu.cacaoselva.infrastructure.repository.JdbcAuditoriaRepository;
import pe.edu.cacaoselva.infrastructure.security.BCryptPasswordAdapter;
import pe.edu.cacaoselva.infrastructure.security.JwtTokenAdapter;

@Configuration
public class ApplicationConfig {

    // ── Infrastructure adapters ──────────────────────────────────────────

    @Bean
    public JdbcLoteRepository loteRepository(DataSource dataSource) {
        return new JdbcLoteRepository(new TransactionAwareDataSourceProxy(dataSource));
    }

    @Bean
    public JdbcSocioRepository socioRepository(DataSource dataSource) {
        return new JdbcSocioRepository(dataSource);
    }

    @Bean
    public JdbcUsuarioRepository usuarioRepository(DataSource dataSource) {
        return new JdbcUsuarioRepository(dataSource);
    }

    @Bean
    public BCryptPasswordAdapter passwordAdapter() {
        return new BCryptPasswordAdapter();
    }

    @Bean
    public JwtTokenAdapter tokenAdapter(@Value("${cacaoselva.jwt.secret:}") String secret) {
        return new JwtTokenAdapter(secret);
    }

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(TokenPort tokenPort, ObjectMapper mapper,
            @Value("${cacaoselva.security.enabled:true}") boolean enabled) {
        var registration = new FilterRegistrationBean<>(new JwtAuthFilter(tokenPort, mapper, enabled));
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public JdbcAuditoriaRepository auditoriaRepository(DataSource dataSource) {
        return new JdbcAuditoriaRepository(new TransactionAwareDataSourceProxy(dataSource));
    }

    @Bean
    public FilterRegistrationBean<pe.edu.cacaoselva.api.filter.AuthRateLimitFilter> authRateLimit(ObjectMapper mapper) {
        var registration = new FilterRegistrationBean<>(new pe.edu.cacaoselva.api.filter.AuthRateLimitFilter(mapper));
        registration.addUrlPatterns("/auth/*");
        registration.setOrder(2);
        return registration;
    }

    @Bean
    public ApplicationRunner bootstrapAdmin(JdbcTemplate jdbc, BCryptPasswordAdapter passwords,
            @Value("${cacaoselva.admin.password:}") String adminPassword) {
        return args -> {
            if (adminPassword == null || adminPassword.length() < 12) {
                throw new IllegalStateException("Configura CACAOSELVA_ADMIN_PASSWORD (mínimo 12 caracteres) en .env.local.");
            }
            jdbc.update("INSERT INTO cacaoselva.usuarios (usuario, hash, rol) VALUES ('admin', ?, 'ADMIN') ON CONFLICT (usuario) DO NOTHING",
                    passwords.hash(adminPassword));
        };
    }

    // ── Lote use cases ───────────────────────────────────────────────────

    @Bean
    public ListarLotesUseCase listarLotesUseCase(LoteRepository repository) {
        return new ListarLotesUseCase(repository);
    }

    @Bean
    public ListarLotesPaginadoUseCase listarLotesPaginadoUseCase(LoteQueryPort queryPort) {
        return new ListarLotesPaginadoUseCase(queryPort);
    }

    @Bean
    public BuscarLotePorIdUseCase buscarLotePorIdUseCase(LoteRepository repository) {
        return new BuscarLotePorIdUseCase(repository);
    }

    @Bean
    public ContarLotesPendientesUseCase contarLotesPendientesUseCase(LoteRepository repository) {
        return new ContarLotesPendientesUseCase(repository);
    }

    @Bean
    public CrearLoteUseCase crearLoteUseCase(LoteWritePort writer, AuditoriaPort auditoria) {
        return new CrearLoteUseCase(writer, auditoria);
    }

    @Bean
    public ActualizarLoteUseCase actualizarLoteUseCase(LoteWritePort writer, AuditoriaPort auditoria) {
        return new ActualizarLoteUseCase(writer, auditoria);
    }

    @Bean
    public EliminarLoteUseCase eliminarLoteUseCase(LoteWritePort writer, LoteRepository reader, AuditoriaPort auditoria) {
        return new EliminarLoteUseCase(writer, reader, auditoria);
    }

    // ── Socio use cases ──────────────────────────────────────────────────

    @Bean
    public CrearSocioUseCase crearSocioUseCase(SocioWritePort writer) {
        return new CrearSocioUseCase(writer);
    }

    @Bean
    public ListarSociosUseCase listarSociosUseCase(SocioRepository repository) {
        return new ListarSociosUseCase(repository);
    }

    @Bean
    public BuscarSocioPorDniUseCase buscarSocioPorDniUseCase(SocioRepository repository) {
        return new BuscarSocioPorDniUseCase(repository);
    }

    // ── Auth use case ────────────────────────────────────────────────────

    @Bean
    public AutenticarUsuarioUseCase autenticarUsuarioUseCase(UsuarioRepository usuarios,
                                                             PasswordPort passwords,
                                                             TokenPort tokens) {
        return new AutenticarUsuarioUseCase(usuarios, passwords, tokens);
    }

    @Bean
    public RegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioWritePort usuarios, PasswordPort passwords) {
        return new RegistrarUsuarioUseCase(usuarios, passwords);
    }
}
