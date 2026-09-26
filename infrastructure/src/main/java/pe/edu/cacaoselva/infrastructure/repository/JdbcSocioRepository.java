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
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import pe.edu.cacaoselva.application.port.SocioRepository;
import pe.edu.cacaoselva.application.port.SocioWritePort;
import pe.edu.cacaoselva.domain.model.DatosSocio;
import pe.edu.cacaoselva.domain.model.Socio;

public final class JdbcSocioRepository implements SocioRepository, SocioWritePort {
    private final DataSource dataSource;

    public JdbcSocioRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public List<Socio> findAll() {
        String sql = "SELECT id, dni, nombre, zona, telefono FROM cacaoselva.socios ORDER BY nombre";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            List<Socio> socios = new ArrayList<>();
            while (rs.next()) {
                socios.add(read(rs));
            }
            return List.copyOf(socios);
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Optional<Socio> findById(Integer id) {
        String sql = "SELECT id, dni, nombre, zona, telefono FROM cacaoselva.socios WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Optional<Socio> findByDni(String dni) {
        String sql = "SELECT id, dni, nombre, zona, telefono FROM cacaoselva.socios WHERE dni = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dni);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(read(rs)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Socio create(DatosSocio datos) {
        String sql = "INSERT INTO cacaoselva.socios (dni, nombre, zona, telefono) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, datos.dni());
            stmt.setString(2, datos.nombre());
            if (datos.zona() != null) {
                stmt.setString(3, datos.zona());
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            if (datos.telefono() != null) {
                stmt.setString(4, datos.telefono());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("El INSERT no devolvió la identidad generada.");
                }
                return new Socio(keys.getInt(1), datos);
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    private static Socio read(ResultSet rs) throws SQLException {
        return new Socio(rs.getInt("id"),
                new DatosSocio(rs.getString("dni"), rs.getString("nombre"),
                        rs.getString("zona"), rs.getString("telefono")));
    }
}
