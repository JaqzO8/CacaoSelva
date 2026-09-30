package pe.edu.cacaoselva.infrastructure.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import pe.edu.cacaoselva.application.port.UsuarioRepository;
import pe.edu.cacaoselva.application.port.UsuarioWritePort;
import pe.edu.cacaoselva.domain.model.Rol;
import pe.edu.cacaoselva.domain.model.Usuario;

public final class JdbcUsuarioRepository implements UsuarioRepository, UsuarioWritePort {
    private final DataSource dataSource;

    public JdbcUsuarioRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public Optional<Usuario> findByUsuario(String usuario) {
        String sql = "SELECT id, usuario, hash, rol, activo FROM cacaoselva.usuarios WHERE usuario = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Usuario(
                        rs.getInt("id"),
                        rs.getString("usuario"),
                        rs.getString("hash"),
                        Rol.valueOf(rs.getString("rol")),
                        rs.getBoolean("activo")));
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public Usuario create(String usuario, String passwordHash, Rol rol) {
        String sql = "INSERT INTO cacaoselva.usuarios (usuario, hash, rol) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario); stmt.setString(2, passwordHash); stmt.setString(3, rol.name());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("El INSERT no devolvió el ID del usuario.");
                return new Usuario(keys.getInt(1), usuario, passwordHash, rol, true);
            }
        } catch (SQLException error) {
            if ("23505".equals(error.getSQLState())) {
                throw new pe.edu.cacaoselva.application.exception.RegistroDuplicadoException("Ya existe ese nombre de usuario.");
            }
            throw new PersistenciaLoteException(error);
        }
    }
}
