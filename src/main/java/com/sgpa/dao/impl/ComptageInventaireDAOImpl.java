package com.sgpa.dao.impl;

import com.sgpa.dao.ComptageInventaireDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.ComptageInventaire;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.MotifEcart;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC du DAO pour les comptages d'inventaire.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ComptageInventaireDAOImpl implements ComptageInventaireDAO {

    private static final Logger logger = LoggerFactory.getLogger(ComptageInventaireDAOImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO comptages_inventaire (id_session, id_lot, quantite_theorique, quantite_physique, " +
            "ecart, motif_ecart, commentaire, date_comptage, id_utilisateur) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_SQL =
            "UPDATE comptages_inventaire SET quantite_physique = ?, ecart = ?, motif_ecart = ?, " +
            "commentaire = ? WHERE id_comptage = ?";

    private static final String DELETE_SQL = "DELETE FROM comptages_inventaire WHERE id_comptage = ?";

    private static final String DELETE_BY_SESSION_SQL = "DELETE FROM comptages_inventaire WHERE id_session = ?";

    private static final String BASE_SELECT =
            "SELECT c.*, l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM comptages_inventaire c " +
            "LEFT JOIN lots l ON c.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON c.id_utilisateur = u.id_utilisateur ";

    private static final String SELECT_BY_ID_SQL = BASE_SELECT + "WHERE c.id_comptage = ?";

    private static final String SELECT_ALL_SQL = BASE_SELECT + "ORDER BY c.date_comptage DESC";

    private static final String SELECT_BY_SESSION_SQL = BASE_SELECT +
            "WHERE c.id_session = ? ORDER BY m.nom_commercial, l.numero_lot";

    private static final String SELECT_BY_SESSION_AND_LOT_SQL = BASE_SELECT +
            "WHERE c.id_session = ? AND c.id_lot = ?";

    private static final String SELECT_BY_LOT_SQL = BASE_SELECT +
            "WHERE c.id_lot = ? ORDER BY c.date_comptage DESC";

    private static final String SELECT_WITH_ECART_SQL = BASE_SELECT +
            "WHERE c.id_session = ? AND c.ecart != 0 ORDER BY ABS(c.ecart) DESC";

    private static final String SELECT_BY_MOTIF_SQL = BASE_SELECT +
            "WHERE c.id_session = ? AND c.motif_ecart = ? ORDER BY c.date_comptage DESC";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM comptages_inventaire";

    private static final String COUNT_BY_SESSION_SQL = "SELECT COUNT(*) FROM comptages_inventaire WHERE id_session = ?";

    private static final String COUNT_ECARTS_SQL =
            "SELECT COUNT(*) FROM comptages_inventaire WHERE id_session = ? AND ecart != 0";

    private static final String TOTAL_ECART_SQL =
            "SELECT COALESCE(SUM(ecart), 0) FROM comptages_inventaire WHERE id_session = ?";

    private static final String EXISTS_SQL = "SELECT 1 FROM comptages_inventaire WHERE id_comptage = ?";

    @Override
    public ComptageInventaire save(ComptageInventaire comptage) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, comptage.getIdSession());
            stmt.setInt(2, comptage.getIdLot());
            stmt.setInt(3, comptage.getQuantiteTheorique());
            stmt.setInt(4, comptage.getQuantitePhysique());
            stmt.setInt(5, comptage.getEcart());
            stmt.setString(6, comptage.getMotifEcart() != null ? comptage.getMotifEcart().name() : null);
            stmt.setString(7, comptage.getCommentaire());
            stmt.setTimestamp(8, Timestamp.valueOf(comptage.getDateComptage()));
            stmt.setInt(9, comptage.getIdUtilisateur());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Echec de la creation du comptage");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    comptage.setIdComptage(generatedKeys.getInt(1));
                }
            }

            logger.debug("Comptage cree: {}", comptage.getIdComptage());
            return comptage;

        } catch (SQLException e) {
            logger.error("Erreur lors de la creation du comptage", e);
            throw new DAOException("Erreur lors de la creation du comptage", e);
        }
    }

    @Override
    public Optional<ComptageInventaire> findById(Integer id) throws DAOException {
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
            logger.error("Erreur lors de la recherche du comptage {}", id, e);
            throw new DAOException("Erreur lors de la recherche du comptage", e);
        }
    }

    @Override
    public List<ComptageInventaire> findAll() throws DAOException {
        List<ComptageInventaire> comptages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                comptages.add(mapResultSet(rs));
            }
            return comptages;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des comptages", e);
            throw new DAOException("Erreur lors de la recuperation des comptages", e);
        }
    }

    @Override
    public void update(ComptageInventaire comptage) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setInt(1, comptage.getQuantitePhysique());
            stmt.setInt(2, comptage.getEcart());
            stmt.setString(3, comptage.getMotifEcart() != null ? comptage.getMotifEcart().name() : null);
            stmt.setString(4, comptage.getCommentaire());
            stmt.setInt(5, comptage.getIdComptage());

            stmt.executeUpdate();
            logger.debug("Comptage mis a jour: {}", comptage.getIdComptage());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour du comptage", e);
            throw new DAOException("Erreur lors de la mise a jour du comptage", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.debug("Comptage supprime: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du comptage", e);
            throw new DAOException("Erreur lors de la suppression du comptage", e);
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
    public List<ComptageInventaire> findBySession(int idSession) throws DAOException {
        List<ComptageInventaire> comptages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_SESSION_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comptages.add(mapResultSet(rs));
                }
            }
            return comptages;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par session", e);
            throw new DAOException("Erreur lors de la recherche par session", e);
        }
    }

    @Override
    public Optional<ComptageInventaire> findBySessionAndLot(int idSession, int idLot) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_SESSION_AND_LOT_SQL)) {

            stmt.setInt(1, idSession);
            stmt.setInt(2, idLot);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par session et lot", e);
            throw new DAOException("Erreur lors de la recherche par session et lot", e);
        }
    }

    @Override
    public List<ComptageInventaire> findByLot(int idLot) throws DAOException {
        List<ComptageInventaire> comptages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_LOT_SQL)) {

            stmt.setInt(1, idLot);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comptages.add(mapResultSet(rs));
                }
            }
            return comptages;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par lot", e);
            throw new DAOException("Erreur lors de la recherche par lot", e);
        }
    }

    @Override
    public List<ComptageInventaire> findWithEcart(int idSession) throws DAOException {
        List<ComptageInventaire> comptages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_WITH_ECART_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comptages.add(mapResultSet(rs));
                }
            }
            return comptages;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des ecarts", e);
            throw new DAOException("Erreur lors de la recherche des ecarts", e);
        }
    }

    @Override
    public List<ComptageInventaire> findByMotif(int idSession, MotifEcart motifEcart) throws DAOException {
        List<ComptageInventaire> comptages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_MOTIF_SQL)) {

            stmt.setInt(1, idSession);
            stmt.setString(2, motifEcart.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comptages.add(mapResultSet(rs));
                }
            }
            return comptages;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par motif", e);
            throw new DAOException("Erreur lors de la recherche par motif", e);
        }
    }

    @Override
    public int getTotalEcart(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(TOTAL_ECART_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul du total des ecarts", e);
            throw new DAOException("Erreur lors du calcul du total des ecarts", e);
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

    @Override
    public int countEcarts(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(COUNT_ECARTS_SQL)) {

            stmt.setInt(1, idSession);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du comptage des ecarts", e);
            throw new DAOException("Erreur lors du comptage des ecarts", e);
        }
    }

    @Override
    public void deleteBySession(int idSession) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_BY_SESSION_SQL)) {

            stmt.setInt(1, idSession);
            int deleted = stmt.executeUpdate();
            logger.debug("{} comptages supprimes pour la session {}", deleted, idSession);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression des comptages", e);
            throw new DAOException("Erreur lors de la suppression des comptages", e);
        }
    }

    /**
     * Mappe un ResultSet vers un ComptageInventaire.
     */
    private ComptageInventaire mapResultSet(ResultSet rs) throws SQLException {
        ComptageInventaire comptage = new ComptageInventaire();
        comptage.setIdComptage(rs.getInt("id_comptage"));
        comptage.setIdSession(rs.getInt("id_session"));
        comptage.setIdLot(rs.getInt("id_lot"));
        comptage.setQuantiteTheorique(rs.getInt("quantite_theorique"));
        comptage.setQuantitePhysique(rs.getInt("quantite_physique"));
        comptage.setEcart(rs.getInt("ecart"));

        String motifStr = rs.getString("motif_ecart");
        if (motifStr != null) {
            comptage.setMotifEcart(MotifEcart.fromString(motifStr));
        }

        comptage.setCommentaire(rs.getString("commentaire"));

        Timestamp dateComptage = rs.getTimestamp("date_comptage");
        if (dateComptage != null) {
            comptage.setDateComptage(dateComptage.toLocalDateTime());
        }

        comptage.setIdUtilisateur(rs.getInt("id_utilisateur"));

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

        comptage.setLot(lot);

        // Mapper l'utilisateur
        Utilisateur user = new Utilisateur();
        user.setIdUtilisateur(rs.getInt("id_utilisateur"));
        user.setNomComplet(rs.getString("nom_complet"));
        comptage.setUtilisateur(user);

        return comptage;
    }
}
