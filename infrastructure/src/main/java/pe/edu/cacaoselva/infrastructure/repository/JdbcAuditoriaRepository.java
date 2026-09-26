package pe.edu.cacaoselva.infrastructure.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import pe.edu.cacaoselva.application.dto.EventoAuditoria;
import pe.edu.cacaoselva.application.exception.PersistenciaLoteException;
import pe.edu.cacaoselva.application.port.AuditoriaPort;
import pe.edu.cacaoselva.application.port.AuditoriaQueryPort;

public final class JdbcAuditoriaRepository implements AuditoriaPort, AuditoriaQueryPort {
    private final DataSource dataSource;

    public JdbcAuditoriaRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    @Override
    public void registrar(Integer loteId, String accion, String usuario, String datosJson) {
        String sql = "INSERT INTO cacaoselva.auditoria_lotes (lote_id, accion, usuario, datos_json) VALUES (?, ?, ?, ?::jsonb)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loteId);
            stmt.setString(2, accion);
            if (usuario != null) {
                stmt.setString(3, usuario);
            } else {
                stmt.setNull(3, Types.VARCHAR);
            }
            if (datosJson != null) {
                stmt.setString(4, datosJson);
            } else {
                stmt.setNull(4, Types.OTHER);
            }
            stmt.executeUpdate();
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }

    @Override
    public List<EventoAuditoria> listarPorLote(Integer loteId) {
        String sql = "SELECT id, lote_id, accion, usuario, datos_json::text AS datos_json, fecha FROM cacaoselva.auditoria_lotes WHERE lote_id = ? ORDER BY fecha DESC, id DESC";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, loteId);
            try (var rs = stmt.executeQuery()) {
                var events = new ArrayList<EventoAuditoria>();
                while (rs.next()) {
                    events.add(new EventoAuditoria(rs.getLong("id"), rs.getInt("lote_id"), rs.getString("accion"),
                            rs.getString("usuario"), rs.getString("datos_json"), rs.getTimestamp("fecha").toInstant()));
                }
                return List.copyOf(events);
            }
        } catch (SQLException error) {
            throw new PersistenciaLoteException(error);
        }
    }
}
