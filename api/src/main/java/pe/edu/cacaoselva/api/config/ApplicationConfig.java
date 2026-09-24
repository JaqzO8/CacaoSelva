package pe.edu.cacaoselva.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import javax.sql.DataSource;
import pe.edu.cacaoselva.application.usecase.BuscarLotePorIdUseCase;
import pe.edu.cacaoselva.application.usecase.ContarLotesPendientesUseCase;
import pe.edu.cacaoselva.application.usecase.ListarLotesUseCase;
import pe.edu.cacaoselva.infrastructure.repository.JdbcLoteRepository;
import pe.edu.cacaoselva.application.usecase.CrearLoteUseCase;
import pe.edu.cacaoselva.application.usecase.ActualizarLoteUseCase;
import pe.edu.cacaoselva.application.usecase.EliminarLoteUseCase;

@Configuration
public class ApplicationConfig {
    @Bean
    public JdbcLoteRepository loteRepository(DataSource dataSource) {
        return new JdbcLoteRepository(dataSource);
    }

    @Bean
    public ListarLotesUseCase listarLotesUseCase(LoteRepository repository) {
        return new ListarLotesUseCase(repository);
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
    public CrearLoteUseCase crearLoteUseCase(LoteWritePort writer) {
        return new CrearLoteUseCase(writer);
    }

    @Bean
    public ActualizarLoteUseCase actualizarLoteUseCase(LoteWritePort writer) {
        return new ActualizarLoteUseCase(writer);
    }

    @Bean
    public EliminarLoteUseCase eliminarLoteUseCase(LoteWritePort writer) {
        return new EliminarLoteUseCase(writer);
    }
}
