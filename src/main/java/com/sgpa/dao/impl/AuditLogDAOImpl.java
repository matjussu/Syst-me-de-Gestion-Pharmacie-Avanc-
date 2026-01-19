package com.sgpa.dao.impl;

import com.sgpa.dao.AuditLogDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.AuditLog;
import com.sgpa.model.enums.TypeAction;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation JDBC de l'interface {@link AuditLogDAO}.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AuditLogDAOImpl implements AuditLogDAO {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogDAOImpl.class);

    private static final String SQL_INSERT =
            "INSERT INTO audit_log (id_utilisateur, nom_utilisateur, type_action, entite, id_entite, description, details_json, adresse_ip) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM audit_log ORDER BY date_action DESC";

    private static final String SQL_FIND_PAGINATED =
            "SELECT * FROM audit_log ORDER BY date_action DESC LIMIT ? OFFSET ?";

    private static final String SQL_FIND_BY_DATE_RANGE =
            "SELECT * FROM audit_log WHERE DATE(date_action) BETWEEN ? AND ? ORDER BY date_action DESC";

    private static final String SQL_FIND_BY_UTILISATEUR =
            "SELECT * FROM audit_log WHERE id_utilisateur = ? ORDER BY date_action DESC";

    private static final String SQL_FIND_BY_TYPE =
            "SELECT * FROM audit_log WHERE type_action = ? ORDER BY date_action DESC";

    private static final String SQL_FIND_BY_ENTITE =
            "SELECT * FROM audit_log WHERE entite = ? AND id_entite = ? ORDER BY date_action DESC";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM audit_log";

    private static final String SQL_DELETE_OLDER =
            "DELETE FROM audit_log WHERE DATE(date_action) < ?";

    @Override
    public AuditLog save(AuditLog auditLog) throws DAOException {
        logger.debug("Enregistrement audit: {} - {}", auditLog.getTypeAction(), auditLog.getDescription());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            if (auditLog.getIdUtilisateur() != null) {
                ps.setInt(1, auditLog.getIdUtilisateur());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, auditLog.getNomUtilisateur());
            ps.setString(3, auditLog.getTypeAction().name());
            ps.setString(4, auditLog.getEntite());
            if (auditLog.getIdEntite() != null) {
                ps.setInt(5, auditLog.getIdEntite());
            } else {
                ps.setNull(5, Types.INTEGER);
            }
            ps.setString(6, auditLog.getDescription());
            ps.setString(7, auditLog.getDetailsJson());
            ps.setString(8, auditLog.getAdresseIp());

            ps.executeUpdate();

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    auditLog.setIdAudit(generatedKeys.getInt(1));
                }
            }

            return auditLog;

        } catch (SQLException e) {
            logger.error("Erreur lors de l'enregistrement de l'audit", e);
            throw new DAOException("Erreur lors de l'enregistrement de l'audit", e);
        }
    }

    @Override
    public List<AuditLog> findAll() throws DAOException {
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                logs.add(mapResultSetToAuditLog(rs));
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des logs", e);
            throw new DAOException("Erreur lors de la recuperation des logs", e);
        }
    }

    @Override
    public List<AuditLog> findAllPaginated(int limit, int offset) throws DAOException {
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PAGINATED)) {

            ps.setInt(1, limit);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des logs pagines", e);
            throw new DAOException("Erreur lors de la recuperation des logs", e);
        }
    }

    @Override
    public List<AuditLog> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException {
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DATE_RANGE)) {

            ps.setDate(1, Date.valueOf(dateDebut));
            ps.setDate(2, Date.valueOf(dateFin));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par date", e);
            throw new DAOException("Erreur lors de la recherche par date", e);
        }
    }

    @Override
    public List<AuditLog> findByUtilisateur(int idUtilisateur) throws DAOException {
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_UTILISATEUR)) {

            ps.setInt(1, idUtilisateur);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par utilisateur", e);
            throw new DAOException("Erreur lors de la recherche par utilisateur", e);
        }
    }

    @Override
    public List<AuditLog> findByTypeAction(TypeAction typeAction) throws DAOException {
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_TYPE)) {

            ps.setString(1, typeAction.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par type", e);
            throw new DAOException("Erreur lors de la recherche par type", e);
        }
    }

    @Override
    public List<AuditLog> findByEntite(String entite, int idEntite) throws DAOException {
        List<AuditLog> logs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ENTITE)) {

            ps.setString(1, entite);
            ps.setInt(2, idEntite);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par entite", e);
            throw new DAOException("Erreur lors de la recherche par entite", e);
        }
    }

    @Override
    public List<AuditLog> search(LocalDate dateDebut, LocalDate dateFin, TypeAction typeAction,
                                  String entite, Integer idUtilisateur, int limit) throws DAOException {
        List<AuditLog> logs = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (dateDebut != null) {
            sql.append(" AND DATE(date_action) >= ?");
            params.add(Date.valueOf(dateDebut));
        }
        if (dateFin != null) {
            sql.append(" AND DATE(date_action) <= ?");
            params.add(Date.valueOf(dateFin));
        }
        if (typeAction != null) {
            sql.append(" AND type_action = ?");
            params.add(typeAction.name());
        }
        if (entite != null && !entite.isEmpty()) {
            sql.append(" AND entite = ?");
            params.add(entite);
        }
        if (idUtilisateur != null) {
            sql.append(" AND id_utilisateur = ?");
            params.add(idUtilisateur);
        }

        sql.append(" ORDER BY date_action DESC LIMIT ?");
        params.add(limit);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Date) {
                    ps.setDate(i + 1, (Date) param);
                } else if (param instanceof String) {
                    ps.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    ps.setInt(i + 1, (Integer) param);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapResultSetToAuditLog(rs));
                }
            }
            return logs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche", e);
            throw new DAOException("Erreur lors de la recherche", e);
        }
    }

    @Override
    public long count() throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_COUNT)) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            throw new DAOException("Erreur lors du comptage", e);
        }
    }

    @Override
    public int deleteOlderThan(LocalDate dateAvant) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE_OLDER)) {

            ps.setDate(1, Date.valueOf(dateAvant));
            int deleted = ps.executeUpdate();
            logger.info("{} entrees d'audit supprimees (anterieures a {})", deleted, dateAvant);
            return deleted;

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression", e);
            throw new DAOException("Erreur lors de la suppression", e);
        }
    }

    private AuditLog mapResultSetToAuditLog(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setIdAudit(rs.getInt("id_audit"));

        Timestamp dateAction = rs.getTimestamp("date_action");
        if (dateAction != null) {
            log.setDateAction(dateAction.toLocalDateTime());
        }

        int idUtilisateur = rs.getInt("id_utilisateur");
        if (!rs.wasNull()) {
            log.setIdUtilisateur(idUtilisateur);
        }

        log.setNomUtilisateur(rs.getString("nom_utilisateur"));

        String typeActionStr = rs.getString("type_action");
        if (typeActionStr != null) {
            log.setTypeAction(TypeAction.valueOf(typeActionStr));
        }

        log.setEntite(rs.getString("entite"));

        int idEntite = rs.getInt("id_entite");
        if (!rs.wasNull()) {
            log.setIdEntite(idEntite);
        }

        log.setDescription(rs.getString("description"));
        log.setDetailsJson(rs.getString("details_json"));
        log.setAdresseIp(rs.getString("adresse_ip"));

        return log;
    }
}
