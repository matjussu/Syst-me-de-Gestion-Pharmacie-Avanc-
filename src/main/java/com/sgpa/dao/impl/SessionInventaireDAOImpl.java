package com.sgpa.dao.impl;

import com.sgpa.dao.SessionInventaireDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.SessionInventaire;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.StatutInventaire;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC du DAO pour les sessions d'inventaire.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class SessionInventaireDAOImpl implements SessionInventaireDAO {

    private static final Logger logger = LoggerFactory.getLogger(SessionInventaireDAOImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO sessions_inventaire (date_debut, statut, id_utilisateur, notes) VALUES (?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE sessions_inventaire SET date_fin = ?, statut = ?, notes = ? WHERE id_session = ?";

    private static final String DELETE_SQL =
            "DELETE FROM sessions_inventaire WHERE id_session = ?";

    private static final String SELECT_BY_ID_SQL =
            "SELECT s.*, u.nom_complet " +
            "FROM sessions_inventaire s " +
            "LEFT JOIN utilisateurs u ON s.id_utilisateur = u.id_utilisateur " +
            "WHERE s.id_session = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT s.*, u.nom_complet " +
            "FROM sessions_inventaire s " +
            "LEFT JOIN utilisateurs u ON s.id_utilisateur = u.id_utilisateur " +
            "ORDER BY s.date_debut DESC";

    private static final String SELECT_BY_STATUT_SQL =
            "SELECT s.*, u.nom_complet " +
            "FROM sessions_inventaire s " +
            "LEFT JOIN utilisateurs u ON s.id_utilisateur = u.id_utilisateur " +
            "WHERE s.statut = ? " +
            "ORDER BY s.date_debut DESC";

    private static final String SELECT_BY_DATE_RANGE_SQL =
            "SELECT s.*, u.nom_complet " +
            "FROM sessions_inventaire s " +
            "LEFT JOIN utilisateurs u ON s.id_utilisateur = u.id_utilisateur " +
            "WHERE DATE(s.date_debut) BETWEEN ? AND ? " +
            "ORDER BY s.date_debut DESC";

    private static final String SELECT_BY_UTILISATEUR_SQL =
            "SELECT s.*, u.nom_complet " +
            "FROM sessions_inventaire s " +
            "LEFT JOIN utilisateurs u ON s.id_utilisateur = u.id_utilisateur " +
            "WHERE s.id_utilisateur = ? " +
            "ORDER BY s.date_debut DESC";

    private static final String SELECT_RECENT_SQL =
            "SELECT s.*, u.nom_complet " +
            "FROM sessions_inventaire s " +
            "LEFT JOIN utilisateurs u ON s.id_utilisateur = u.id_utilisateur " +
            "ORDER BY s.date_debut DESC LIMIT ?";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM sessions_inventaire";

    private static final String EXISTS_SQL = "SELECT 1 FROM sessions_inventaire WHERE id_session = ?";

    private static final String HAS_EN_COURS_SQL =
            "SELECT COUNT(*) FROM sessions_inventaire WHERE statut = 'EN_COURS'";

    private static final String TERMINER_SQL =
            "UPDATE sessions_inventaire SET statut = 'TERMINEE', date_fin = NOW() WHERE id_session = ?";

    private static final String ANNULER_SQL =
            "UPDATE sessions_inventaire SET statut = 'ANNULEE', date_fin = NOW() WHERE id_session = ?";

    @Override
    public SessionInventaire save(SessionInventaire session) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(session.getDateDebut()));
            stmt.setString(2, session.getStatut().name());
            stmt.setInt(3, session.getIdUtilisateur());
            stmt.setString(4, session.getNotes());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Echec de la creation de la session");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    session.setIdSession(generatedKeys.getInt(1));
                }
            }

            logger.debug("Session d'inventaire creee: {}", session.getIdSession());
            return session;

        } catch (SQLException e) {
            logger.error("Erreur lors de la creation de la session", e);
            throw new DAOException("Erreur lors de la creation de la session", e);
        }
    }

    @Override
    public Optional<SessionInventaire> findById(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de la session {}", id, e);
            throw new DAOException("Erreur lors de la recherche de la session", e);
        }
    }

    @Override
    public List<SessionInventaire> findAll() throws DAOException {
        List<SessionInventaire> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                sessions.add(mapResultSet(rs));
            }
            return sessions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des sessions", e);
            throw new DAOException("Erreur lors de la recuperation des sessions", e);
        }
    }

    @Override
    public void update(SessionInventaire session) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            if (session.getDateFin() != null) {
                stmt.setTimestamp(1, Timestamp.valueOf(session.getDateFin()));
            } else {
                stmt.setNull(1, Types.TIMESTAMP);
            }
            stmt.setString(2, session.getStatut().name());
            stmt.setString(3, session.getNotes());
            stmt.setInt(4, session.getIdSession());

            stmt.executeUpdate();
            logger.debug("Session d'inventaire mise a jour: {}", session.getIdSession());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour de la session", e);
            throw new DAOException("Erreur lors de la mise a jour de la session", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.debug("Session d'inventaire supprimee: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression de la session", e);
            throw new DAOException("Erreur lors de la suppression de la session", e);
        }
    }

    @Override
    public long count() throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_SQL);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du comptage des sessions", e);
            throw new DAOException("Erreur lors du comptage des sessions", e);
        }
    }

    @Override
    public boolean existsById(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(EXISTS_SQL)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            logger.error("Erreur lors de la verification d'existence", e);
            throw new DAOException("Erreur lors de la verification d'existence", e);
        }
    }

    @Override
    public List<SessionInventaire> findByStatut(StatutInventaire statut) throws DAOException {
        List<SessionInventaire> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_STATUT_SQL)) {

            stmt.setString(1, statut.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSet(rs));
                }
            }
            return sessions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par statut", e);
            throw new DAOException("Erreur lors de la recherche par statut", e);
        }
    }

    @Override
    public List<SessionInventaire> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException {
        List<SessionInventaire> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_DATE_RANGE_SQL)) {

            stmt.setDate(1, Date.valueOf(dateDebut));
            stmt.setDate(2, Date.valueOf(dateFin));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSet(rs));
                }
            }
            return sessions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par date", e);
            throw new DAOException("Erreur lors de la recherche par date", e);
        }
    }

    @Override
    public List<SessionInventaire> findByUtilisateur(int idUtilisateur) throws DAOException {
        List<SessionInventaire> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_UTILISATEUR_SQL)) {

            stmt.setInt(1, idUtilisateur);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSet(rs));
                }
            }
            return sessions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par utilisateur", e);
            throw new DAOException("Erreur lors de la recherche par utilisateur", e);
        }
    }

    @Override
    public List<SessionInventaire> findRecent(int limit) throws DAOException {
        List<SessionInventaire> sessions = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_RECENT_SQL)) {

            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapResultSet(rs));
                }
            }
            return sessions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des sessions recentes", e);
            throw new DAOException("Erreur lors de la recherche des sessions recentes", e);
        }
    }

    @Override
    public boolean hasSessionEnCours() throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(HAS_EN_COURS_SQL);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;

        } catch (SQLException e) {
            logger.error("Erreur lors de la verification de session en cours", e);
            throw new DAOException("Erreur lors de la verification de session en cours", e);
        }
    }

    @Override
    public void terminerSession(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(TERMINER_SQL)) {

            stmt.setInt(1, idSession);
            stmt.executeUpdate();
            logger.debug("Session {} terminee", idSession);

        } catch (SQLException e) {
            logger.error("Erreur lors de la terminaison de la session", e);
            throw new DAOException("Erreur lors de la terminaison de la session", e);
        }
    }

    @Override
    public void annulerSession(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(ANNULER_SQL)) {

            stmt.setInt(1, idSession);
            stmt.executeUpdate();
            logger.debug("Session {} annulee", idSession);

        } catch (SQLException e) {
            logger.error("Erreur lors de l'annulation de la session", e);
            throw new DAOException("Erreur lors de l'annulation de la session", e);
        }
    }

    /**
     * Mappe un ResultSet vers une SessionInventaire.
     */
    private SessionInventaire mapResultSet(ResultSet rs) throws SQLException {
        SessionInventaire session = new SessionInventaire();
        session.setIdSession(rs.getInt("id_session"));

        Timestamp dateDebut = rs.getTimestamp("date_debut");
        if (dateDebut != null) {
            session.setDateDebut(dateDebut.toLocalDateTime());
        }

        Timestamp dateFin = rs.getTimestamp("date_fin");
        if (dateFin != null) {
            session.setDateFin(dateFin.toLocalDateTime());
        }

        String statutStr = rs.getString("statut");
        session.setStatut(StatutInventaire.fromString(statutStr));

        session.setIdUtilisateur(rs.getInt("id_utilisateur"));
        session.setNotes(rs.getString("notes"));

        // Mapper l'utilisateur
        Utilisateur user = new Utilisateur();
        user.setIdUtilisateur(rs.getInt("id_utilisateur"));
        user.setNomComplet(rs.getString("nom_complet"));
        session.setUtilisateur(user);

        return session;
    }
}
