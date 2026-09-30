package pe.edu.cacaoselva.infrastructure.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import pe.edu.cacaoselva.application.dto.FiltroLotes;
import pe.edu.cacaoselva.application.dto.LoteDto;
import pe.edu.cacaoselva.application.dto.PaginaLotes;
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import pe.edu.cacaoselva.application.port.LoteRepository;
import pe.edu.cacaoselva.application.port.LoteWritePort;
import pe.edu.cacaoselva.domain.exception.ConflictoEdicionException;
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
        String sql = "SELECT id, socio_id, peso_kg, estado, version FROM cacaoselva.lotes ORDER BY id";
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
    public PaginaLotes findAll(FiltroLotes filtro) {
        var sql = new StringBuilder("SELECT l.id, l.socio_id, l.peso_kg, l.estado, l.version FROM cacaoselva.lotes l JOIN cacaoselva.socios s ON s.id = l.socio_id WHERE 1=1");
        var countSql = new StringBuilder("SELECT COUNT(*) FROM cacaoselva.lotes l JOIN cacaoselva.socios s ON s.id = l.socio_id WHERE 1=1");
        if (filtro.estado() != null) {
            sql.append(" AND l.estado = ?");
            countSql.append(" AND l.estado = ?");
        }
        if (filtro.socioId() != null) {
            sql.append(" AND l.socio_id = ?");
            countSql.append(" AND l.socio_id = ?");
        }
        if (filtro.socio() != null) {
            sql.append(" AND LOWER(s.nombre) LIKE LOWER(?)");
            countSql.append(" AND LOWER(s.nombre) LIKE LOWER(?)");
        }
        sql.append(" ORDER BY id LIMIT ? OFFSET ?");

        try (Connection connection = dataSource.getConnection()) {
            // Count
            long total;
            try (PreparedStatement stmt = connection.prepareStatement(countSql.toString())) {
                bindFilters(stmt, filtro);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getLong(1);
                }
            }
            // Page
            try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                int idx = bindFilters(stmt, filtro);
                stmt.setInt(idx, filtro.size());
                stmt.setInt(idx + 1, filtro.offset());
                try (ResultSet rs = stmt.executeQuery()) {
                    List<LoteDto> content = new ArrayList<>();
                    while (rs.next()) {
                        content.add(LoteDto.from(read(rs)));
                    }
                    int totalPages = (int) Math.ceil((double) total / filtro.size());
                    return new PaginaLotes(content, filtro.page(), filtro.size(), total, totalPages);
                }
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    private int bindFilters(PreparedStatement stmt, FiltroLotes filtro) throws SQLException {
        int idx = 1;
        if (filtro.estado() != null) {
            stmt.setString(idx++, filtro.estado().name());
        }
        if (filtro.socioId() != null) {
            stmt.setInt(idx++, filtro.socioId());
        }
        if (filtro.socio() != null) {
            stmt.setString(idx++, "%" + filtro.socio().replace("%", "\\%").replace("_", "\\_") + "%");
        }
        return idx;
    }

    @Override
    public Optional<Lote> findById(Integer id) {
        String sql = "SELECT id, socio_id, peso_kg, estado, version FROM cacaoselva.lotes WHERE id = ?";
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
        String sql = "INSERT INTO cacaoselva.lotes (socio_id, peso_kg, estado) VALUES (?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, datos);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("El INSERT no devolvió la identidad generada.");
                }
                return new Lote(keys.getInt(1), datos.socioId(), datos.pesoKg(), datos.estado(), 1);
            }
        } catch (SQLException error) {
            if ("23503".equals(error.getSQLState())) {
                throw new pe.edu.cacaoselva.application.exception.SocioNoEncontradoException(datos.socioId());
            }
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Optional<Lote> update(Integer id, DatosLote datos, Integer version) {
        String sql;
        if (version != null) {
            sql = "UPDATE cacaoselva.lotes SET socio_id = ?, peso_kg = ?, estado = ?, version = version + 1 WHERE id = ? AND version = ?";
        } else {
            sql = "UPDATE cacaoselva.lotes SET socio_id = ?, peso_kg = ?, estado = ?, version = version + 1 WHERE id = ?";
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, datos);
            statement.setInt(4, id);
            if (version != null) {
                statement.setInt(5, version);
            }
            int rows = statement.executeUpdate();
            if (rows == 0) {
                if (version != null && existsById(connection, id)) {
                    throw new ConflictoEdicionException(id);
                }
                return Optional.empty();
            }
            return findById(id);
        } catch (ConflictoEdicionException e) {
            throw e;
        } catch (SQLException error) {
            if ("23503".equals(error.getSQLState())) {
                throw new pe.edu.cacaoselva.application.exception.SocioNoEncontradoException(datos.socioId());
            }
            throw new PersistenciaLoteException(error);
        }
    }

    private boolean existsById(Connection connection, Integer id) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM cacaoselva.lotes WHERE id = ?")) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
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
        statement.setInt(1, datos.socioId());
        statement.setBigDecimal(2, datos.pesoKg());
        statement.setString(3, datos.estado().name());
    }

    private static Lote read(ResultSet result) throws SQLException {
        return new Lote(result.getInt("id"), result.getInt("socio_id"), result.getBigDecimal("peso_kg"),
                EstadoLote.valueOf(result.getString("estado")), result.getInt("version"));
    }
}
