package com.sgpa.dao.impl;

import com.sgpa.dao.RetourDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import com.sgpa.model.Retour;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.Vente;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC du DAO pour les retours.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class RetourDAOImpl implements RetourDAO {

    private static final Logger logger = LoggerFactory.getLogger(RetourDAOImpl.class);

    private static final String INSERT_SQL =
            "INSERT INTO retours (id_vente, id_lot, id_utilisateur, quantite, motif, date_retour, reintegre, commentaire) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_ID_SQL =
            "SELECT r.*, v.date_vente, v.montant_total, " +
            "l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM retours r " +
            "LEFT JOIN ventes v ON r.id_vente = v.id_vente " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur " +
            "WHERE r.id_retour = ?";

    private static final String SELECT_ALL_SQL =
            "SELECT r.*, v.date_vente, v.montant_total, " +
            "l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM retours r " +
            "LEFT JOIN ventes v ON r.id_vente = v.id_vente " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur " +
            "ORDER BY r.date_retour DESC";

    private static final String SELECT_BY_VENTE_SQL =
            "SELECT r.*, v.date_vente, v.montant_total, " +
            "l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM retours r " +
            "LEFT JOIN ventes v ON r.id_vente = v.id_vente " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur " +
            "WHERE r.id_vente = ? " +
            "ORDER BY r.date_retour DESC";

    private static final String SELECT_BY_DATE_RANGE_SQL =
            "SELECT r.*, v.date_vente, v.montant_total, " +
            "l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM retours r " +
            "LEFT JOIN ventes v ON r.id_vente = v.id_vente " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur " +
            "WHERE DATE(r.date_retour) BETWEEN ? AND ? " +
            "ORDER BY r.date_retour DESC";

    private static final String SELECT_BY_UTILISATEUR_SQL =
            "SELECT r.*, v.date_vente, v.montant_total, " +
            "l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM retours r " +
            "LEFT JOIN ventes v ON r.id_vente = v.id_vente " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur " +
            "WHERE r.id_utilisateur = ? " +
            "ORDER BY r.date_retour DESC";

    private static final String SELECT_BY_REINTEGRE_SQL =
            "SELECT r.*, v.date_vente, v.montant_total, " +
            "l.numero_lot, l.date_peremption, l.quantite_stock, " +
            "m.id_medicament, m.nom_commercial, m.principe_actif, " +
            "u.nom_complet " +
            "FROM retours r " +
            "LEFT JOIN ventes v ON r.id_vente = v.id_vente " +
            "LEFT JOIN lots l ON r.id_lot = l.id_lot " +
            "LEFT JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "LEFT JOIN utilisateurs u ON r.id_utilisateur = u.id_utilisateur " +
            "WHERE r.reintegre = ? " +
            "ORDER BY r.date_retour DESC";

    private static final String UPDATE_SQL =
            "UPDATE retours SET id_vente = ?, id_lot = ?, id_utilisateur = ?, quantite = ?, " +
            "motif = ?, date_retour = ?, reintegre = ?, commentaire = ? WHERE id_retour = ?";

    private static final String DELETE_SQL = "DELETE FROM retours WHERE id_retour = ?";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM retours";

    @Override
    public Retour save(Retour retour) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, retour.getIdVente());
            stmt.setInt(2, retour.getIdLot());
            stmt.setInt(3, retour.getIdUtilisateur());
            stmt.setInt(4, retour.getQuantite());
            stmt.setString(5, retour.getMotif());
            stmt.setTimestamp(6, Timestamp.valueOf(retour.getDateRetour()));
            stmt.setBoolean(7, retour.isReintegre());
            stmt.setString(8, retour.getCommentaire());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Echec de la creation du retour");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    retour.setIdRetour(generatedKeys.getInt(1));
                }
            }

            logger.info("Retour cree avec ID: {}", retour.getIdRetour());
            return retour;

        } catch (SQLException e) {
            logger.error("Erreur lors de la creation du retour", e);
            throw new DAOException("Erreur lors de la creation du retour", e);
        }
    }

    @Override
    public Optional<Retour> findById(Integer id) throws DAOException {
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
            logger.error("Erreur lors de la recherche du retour par ID: {}", id, e);
            throw new DAOException("Erreur lors de la recherche du retour", e);
        }
    }

    @Override
    public List<Retour> findAll() throws DAOException {
        List<Retour> retours = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                retours.add(mapResultSet(rs));
            }

            return retours;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation de tous les retours", e);
            throw new DAOException("Erreur lors de la recuperation des retours", e);
        }
    }

    @Override
    public void update(Retour retour) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_SQL)) {

            stmt.setInt(1, retour.getIdVente());
            stmt.setInt(2, retour.getIdLot());
            stmt.setInt(3, retour.getIdUtilisateur());
            stmt.setInt(4, retour.getQuantite());
            stmt.setString(5, retour.getMotif());
            stmt.setTimestamp(6, Timestamp.valueOf(retour.getDateRetour()));
            stmt.setBoolean(7, retour.isReintegre());
            stmt.setString(8, retour.getCommentaire());
            stmt.setInt(9, retour.getIdRetour());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Retour non trouve: " + retour.getIdRetour());
            }

            logger.info("Retour mis a jour: {}", retour.getIdRetour());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour du retour: {}", retour.getIdRetour(), e);
            throw new DAOException("Erreur lors de la mise a jour du retour", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_SQL)) {

            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new DAOException("Retour non trouve: " + id);
            }

            logger.info("Retour supprime: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du retour: {}", id, e);
            throw new DAOException("Erreur lors de la suppression du retour", e);
        }
    }

    @Override
    public boolean existsById(Integer id) throws DAOException {
        String sql = "SELECT COUNT(*) FROM retours WHERE id_retour = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            logger.error("Erreur lors de la verification de l'existence du retour: {}", id, e);
            throw new DAOException("Erreur lors de la verification du retour", e);
        }
    }

    @Override
    public List<Retour> findByVente(int idVente) throws DAOException {
        List<Retour> retours = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_VENTE_SQL)) {

            stmt.setInt(1, idVente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    retours.add(mapResultSet(rs));
                }
            }

            return retours;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des retours par vente: {}", idVente, e);
            throw new DAOException("Erreur lors de la recherche des retours", e);
        }
    }

    @Override
    public List<Retour> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException {
        List<Retour> retours = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_DATE_RANGE_SQL)) {

            stmt.setDate(1, Date.valueOf(dateDebut));
            stmt.setDate(2, Date.valueOf(dateFin));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    retours.add(mapResultSet(rs));
                }
            }

            return retours;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des retours par periode", e);
            throw new DAOException("Erreur lors de la recherche des retours", e);
        }
    }

    @Override
    public List<Retour> findByUtilisateur(int idUtilisateur) throws DAOException {
        List<Retour> retours = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_UTILISATEUR_SQL)) {

            stmt.setInt(1, idUtilisateur);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    retours.add(mapResultSet(rs));
                }
            }

            return retours;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des retours par utilisateur: {}", idUtilisateur, e);
            throw new DAOException("Erreur lors de la recherche des retours", e);
        }
    }

    @Override
    public List<Retour> findByReintegre(boolean reintegre) throws DAOException {
        List<Retour> retours = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_BY_REINTEGRE_SQL)) {

            stmt.setBoolean(1, reintegre);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    retours.add(mapResultSet(rs));
                }
            }

            return retours;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des retours par reintegration", e);
            throw new DAOException("Erreur lors de la recherche des retours", e);
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
            logger.error("Erreur lors du comptage des retours", e);
            throw new DAOException("Erreur lors du comptage des retours", e);
        }
    }

    /**
     * Mappe un ResultSet vers un objet Retour.
     */
    private Retour mapResultSet(ResultSet rs) throws SQLException {
        Retour retour = new Retour();
        retour.setIdRetour(rs.getInt("id_retour"));
        retour.setIdVente(rs.getInt("id_vente"));
        retour.setIdLot(rs.getInt("id_lot"));
        retour.setIdUtilisateur(rs.getInt("id_utilisateur"));
        retour.setQuantite(rs.getInt("quantite"));
        retour.setMotif(rs.getString("motif"));

        Timestamp dateRetour = rs.getTimestamp("date_retour");
        if (dateRetour != null) {
            retour.setDateRetour(dateRetour.toLocalDateTime());
        }

        retour.setReintegre(rs.getBoolean("reintegre"));
        retour.setCommentaire(rs.getString("commentaire"));

        // Mapper la vente
        Vente vente = new Vente();
        vente.setIdVente(rs.getInt("id_vente"));
        Timestamp dateVente = rs.getTimestamp("date_vente");
        if (dateVente != null) {
            vente.setDateVente(dateVente.toLocalDateTime());
        }
        vente.setMontantTotal(rs.getBigDecimal("montant_total"));
        retour.setVente(vente);

        // Mapper le lot et le medicament
        Lot lot = new Lot();
        lot.setIdLot(rs.getInt("id_lot"));
        lot.setNumeroLot(rs.getString("numero_lot"));
        Date datePeremption = rs.getDate("date_peremption");
        if (datePeremption != null) {
            lot.setDatePeremption(datePeremption.toLocalDate());
        }
        lot.setQuantiteStock(rs.getInt("quantite_stock"));

        Medicament med = new Medicament();
        med.setIdMedicament(rs.getInt("id_medicament"));
        med.setNomCommercial(rs.getString("nom_commercial"));
        med.setPrincipeActif(rs.getString("principe_actif"));
        lot.setMedicament(med);

        retour.setLot(lot);

        // Mapper l'utilisateur
        Utilisateur user = new Utilisateur();
        user.setIdUtilisateur(rs.getInt("id_utilisateur"));
        user.setNomComplet(rs.getString("nom_complet"));
        retour.setUtilisateur(user);

        return retour;
    }
}
