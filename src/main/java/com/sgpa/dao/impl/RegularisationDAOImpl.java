package com.sgpa.dao.impl;

import com.sgpa.dao.RegularisationDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import com.sgpa.model.Regularisation;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.MotifEcart;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC du DAO pour les regularisations.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class RegularisationDAOImpl implements RegularisationDAO {

    private static final Logger logger = LoggerFactory.getLogger(RegularisationDAOImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO regularisations (id_session, id_lot, quantite_ancienne, quantite_nouvelle, " +
            "raison, justificatif, date_regularisation, id_utilisateur) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE regularisations SET quantite_nouvelle = ?, raison = ?, justificatif = ? " +
            "WHERE id_regularisation = ?";

    private static final String DELETE_SQL = "DELETE FROM regularisations WHERE id_regularisation = ?";

    private static final String BASE_SELECT =
            "SELECT r.*, l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM regularisations r " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur ";

    private static final String SELECT_BY_ID_SQL = BASE_SELECT + "WHERE r.id_regularisation = ?";

    private static final String SELECT_ALL_SQL = BASE_SELECT + "ORDER BY r.date_regularisation DESC";

    private static final String SELECT_BY_SESSION_SQL = BASE_SELECT +
            "WHERE r.id_session = ? ORDER BY r.date_regularisation DESC";

    private static final String SELECT_BY_LOT_SQL = BASE_SELECT +
            "WHERE r.id_lot = ? ORDER BY r.date_regularisation DESC";

    private static final String SELECT_BY_RAISON_SQL = BASE_SELECT +
            "WHERE r.raison = ? ORDER BY r.date_regularisation DESC";

    private static final String SELECT_BY_DATE_RANGE_SQL = BASE_SELECT +
            "WHERE DATE(r.date_regularisation) BETWEEN ? AND ? ORDER BY r.date_regularisation DESC";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM regularisations";

    private static final String COUNT_BY_SESSION_SQL = "SELECT COUNT(*) FROM regularisations WHERE id_session = ?";

    private static final String TOTAL_AJUSTEMENT_SQL =
            "SELECT COALESCE(SUM(quantite_nouvelle - quantite_ancienne), 0) FROM regularisations WHERE id_session = ?";

    private static final String EXISTS_SQL = "SELECT 1 FROM regularisations WHERE id_regularisation = ?";

    @Override
    public Regularisation save(Regularisation regularisation) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, regularisation.getIdSession());
            stmt.setInt(2, regularisation.getIdLot());
            stmt.setInt(3, regularisation.getQuantiteAncienne());
            stmt.setInt(4, regularisation.getQuantiteNouvelle());
            stmt.setString(5, regularisation.getRaison().name());
            stmt.setString(6, regularisation.getJustificatif());
            stmt.setTimestamp(7, Timestamp.valueOf(regularisation.getDateRegularisation()));
            stmt.setInt(8, regularisation.getIdUtilisateur());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Echec de la creation de la regularisation");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    regularisation.setIdRegularisation(generatedKeys.getInt(1));
                }
            }

            logger.debug("Regularisation creee: {}", regularisation.getIdRegularisation());
            return regularisation;

        } catch (SQLException e) {
            logger.error("Erreur lors de la creation de la regularisation", e);
            throw new DAOException("Erreur lors de la creation de la regularisation", e);
        }
    }

    @Override
    public Optional<Regularisation> findById(Integer id) throws DAOException {
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
            logger.error("Erreur lors de la recherche de la regularisation {}", id, e);
            throw new DAOException("Erreur lors de la recherche de la regularisation", e);
        }
    }

    @Override
    public List<Regularisation> findAll() throws DAOException {
        List<Regularisation> regularisations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                regularisations.add(mapResultSet(rs));
            }
            return regularisations;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des regularisations", e);
            throw new DAOException("Erreur lors de la recuperation des regularisations", e);
        }
    }

    @Override
    public void update(Regularisation regularisation) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setInt(1, regularisation.getQuantiteNouvelle());
            stmt.setString(2, regularisation.getRaison().name());
            stmt.setString(3, regularisation.getJustificatif());
            stmt.setInt(4, regularisation.getIdRegularisation());

            stmt.executeUpdate();
            logger.debug("Regularisation mise a jour: {}", regularisation.getIdRegularisation());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour de la regularisation", e);
            throw new DAOException("Erreur lors de la mise a jour de la regularisation", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.debug("Regularisation supprimee: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression de la regularisation", e);
            throw new DAOException("Erreur lors de la suppression de la regularisation", e);
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
            logger.error("Erreur lors du comptage", e);
            throw new DAOException("Erreur lors du comptage", e);
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
    public List<Regularisation> findBySession(int idSession) throws DAOException {
        List<Regularisation> regularisations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_SESSION_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    regularisations.add(mapResultSet(rs));
                }
            }
            return regularisations;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par session", e);
            throw new DAOException("Erreur lors de la recherche par session", e);
        }
    }

    @Override
    public List<Regularisation> findByLot(int idLot) throws DAOException {
        List<Regularisation> regularisations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_LOT_SQL)) {

            stmt.setInt(1, idLot);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    regularisations.add(mapResultSet(rs));
                }
            }
            return regularisations;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par lot", e);
            throw new DAOException("Erreur lors de la recherche par lot", e);
        }
    }

    @Override
    public List<Regularisation> findByRaison(MotifEcart raison) throws DAOException {
        List<Regularisation> regularisations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_RAISON_SQL)) {

            stmt.setString(1, raison.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    regularisations.add(mapResultSet(rs));
                }
            }
            return regularisations;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par raison", e);
            throw new DAOException("Erreur lors de la recherche par raison", e);
        }
    }

    @Override
    public List<Regularisation> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException {
        List<Regularisation> regularisations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_DATE_RANGE_SQL)) {

            stmt.setDate(1, Date.valueOf(dateDebut));
            stmt.setDate(2, Date.valueOf(dateFin));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    regularisations.add(mapResultSet(rs));
                }
            }
            return regularisations;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par date", e);
            throw new DAOException("Erreur lors de la recherche par date", e);
        }
    }

    @Override
    public int getTotalAjustement(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(TOTAL_AJUSTEMENT_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul du total des ajustements", e);
            throw new DAOException("Erreur lors du calcul du total des ajustements", e);
        }
    }

    @Override
    public int countBySession(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_BY_SESSION_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du comptage par session", e);
            throw new DAOException("Erreur lors du comptage par session", e);
        }
    }

    /**
     * Mappe un ResultSet vers une Regularisation.
     */
    private Regularisation mapResultSet(ResultSet rs) throws SQLException {
        Regularisation regularisation = new Regularisation();
        regularisation.setIdRegularisation(rs.getInt("id_regularisation"));
        regularisation.setIdSession(rs.getInt("id_session"));
        regularisation.setIdLot(rs.getInt("id_lot"));
        regularisation.setQuantiteAncienne(rs.getInt("quantite_ancienne"));
        regularisation.setQuantiteNouvelle(rs.getInt("quantite_nouvelle"));

        String raisonStr = rs.getString("raison");
        if (raisonStr != null) {
            regularisation.setRaison(MotifEcart.fromString(raisonStr));
        }

        regularisation.setJustificatif(rs.getString("justificatif"));

        Timestamp dateRegularisation = rs.getTimestamp("date_regularisation");
        if (dateRegularisation != null) {
            regularisation.setDateRegularisation(dateRegularisation.toLocalDateTime());
        }

        regularisation.setIdUtilisateur(rs.getInt("id_utilisateur"));

        // Mapper le lot
        Lot lot = new Lot();
        lot.setIdLot(rs.getInt("id_lot"));
        lot.setNumeroLot(rs.getString("numero_lot"));
        lot.setQuantiteStock(rs.getInt("quantite_stock"));

        Date datePeremption = rs.getDate("date_peremption");
        if (datePeremption != null) {
            lot.setDatePeremption(datePeremption.toLocalDate());
        }

        // Mapper le medicament
        Medicament med = new Medicament();
        med.setIdMedicament(rs.getInt("id_medicament"));
        med.setNomCommercial(rs.getString("nom_commercial"));
        med.setPrincipeActif(rs.getString("principe_actif"));
        lot.setMedicament(med);

        regularisation.setLot(lot);

        // Mapper l'utilisateur
        Utilisateur user = new Utilisateur();
        user.setIdUtilisateur(rs.getInt("id_utilisateur"));
        user.setNomComplet(rs.getString("nom_complet"));
        regularisation.setUtilisateur(user);

        return regularisation;
    }
}
