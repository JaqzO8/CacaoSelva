package pe.edu.cacaoselva.infrastructure.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.domain.model.DatosLote;
import pe.edu.cacaoselva.domain.model.EstadoLote;
import pe.edu.cacaoselva.domain.model.Lote;

/** Cada operación toma una conexión del pool y la devuelve mediante try-with-resources. */
public final class JdbcLoteRepository implements LoteRepository, LoteWritePort {
    private final DataSource dataSource;

    public JdbcLoteRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public List<Lote> findAll() {
        String sql = "SELECT id, socio, peso_kg, estado FROM cacaoselva.lotes ORDER BY id";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<Lote> lotes = new ArrayList<>();
            while (result.next()) {
                lotes.add(read(result));
            }
            return List.copyOf(lotes);
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Optional<Lote> findById(Integer id) {
        String sql = "SELECT id, socio, peso_kg, estado FROM cacaoselva.lotes WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(read(result)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Lote create(DatosLote datos) {
        String sql = "INSERT INTO cacaoselva.lotes (socio, peso_kg, estado) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, datos);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("El INSERT no devolvió la identidad generada.");
                }
                return lote(keys.getInt(1), datos);
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Optional<Lote> update(Integer id, DatosLote datos) {
        String sql = "UPDATE cacaoselva.lotes SET socio = ?, peso_kg = ?, estado = ? WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, datos);
            statement.setInt(4, id);
            return statement.executeUpdate() == 1 ? Optional.of(lote(id, datos)) : Optional.empty();
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM cacaoselva.lotes WHERE id = ?")) {
            statement.setInt(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    private static void bind(PreparedStatement statement, DatosLote datos) throws SQLException {
        statement.setString(1, datos.socio());
        statement.setBigDecimal(2, datos.pesoKg());
        statement.setString(3, datos.estado().name());
    }

    private static Lote read(ResultSet result) throws SQLException {
        return new Lote(result.getInt("id"), result.getString("socio"), result.getBigDecimal("peso_kg"),
                EstadoLote.valueOf(result.getString("estado")));
    }

    private static Lote lote(Integer id, DatosLote datos) {
        return new Lote(id, datos.socio(), datos.pesoKg(), datos.estado());
    }
}
