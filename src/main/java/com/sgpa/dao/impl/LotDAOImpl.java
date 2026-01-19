package com.sgpa.dao.impl;

import com.sgpa.dao.LotDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Lot;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC de l'interface {@link LotDAO}.
 * <p>
 * Gere les operations CRUD sur les lots et fournit les methodes
 * essentielles pour l'algorithme FEFO (First Expired, First Out).
 * </p>
 * <p>
 * <b>Methode cle FEFO:</b> {@link #findByMedicamentIdSortedByExpiration(int)}
 * retourne les lots tries par date de peremption croissante pour permettre
 * de vendre en priorite les lots qui expirent le plus tot.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class LotDAOImpl implements LotDAO {

    private static final Logger logger = LoggerFactory.getLogger(LotDAOImpl.class);

    // Requetes SQL
    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM lots WHERE id_lot = ?";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM lots ORDER BY date_peremption";

    private static final String SQL_INSERT =
            "INSERT INTO lots (id_medicament, id_fournisseur, numero_lot, date_peremption, " +
            "date_fabrication, quantite_stock, prix_achat) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE lots SET id_medicament = ?, id_fournisseur = ?, numero_lot = ?, " +
            "date_peremption = ?, date_fabrication = ?, quantite_stock = ?, prix_achat = ? " +
            "WHERE id_lot = ?";

    private static final String SQL_DELETE =
            "DELETE FROM lots WHERE id_lot = ?";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM lots";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM lots WHERE id_lot = ?";

    // Requete FEFO - Lots tries par date de peremption croissante
    private static final String SQL_FIND_BY_MEDICAMENT_SORTED_BY_EXPIRATION =
            "SELECT * FROM lots " +
            "WHERE id_medicament = ? AND quantite_stock > 0 " +
            "ORDER BY date_peremption ASC";

    // Lots vendables (non perimes et avec stock)
    private static final String SQL_FIND_VENDABLE_BY_MEDICAMENT =
            "SELECT * FROM lots " +
            "WHERE id_medicament = ? AND quantite_stock > 0 AND date_peremption >= CURDATE() " +
            "ORDER BY date_peremption ASC";

    private static final String SQL_FIND_EXPIRING_BEFORE =
            "SELECT * FROM lots WHERE date_peremption < ? AND quantite_stock > 0 " +
            "ORDER BY date_peremption ASC";

    private static final String SQL_FIND_EXPIRED =
            "SELECT * FROM lots WHERE date_peremption < CURDATE() AND quantite_stock > 0 " +
            "ORDER BY date_peremption ASC";

    private static final String SQL_GET_TOTAL_STOCK =
            "SELECT COALESCE(SUM(quantite_stock), 0) FROM lots WHERE id_medicament = ?";

    private static final String SQL_FIND_WITH_LOW_STOCK =
            "SELECT l.* FROM lots l " +
            "JOIN medicaments m ON l.id_medicament = m.id_medicament " +
            "WHERE l.quantite_stock > 0 " +
            "GROUP BY l.id_medicament " +
            "HAVING SUM(l.quantite_stock) < m.seuil_min";

    private static final String SQL_FIND_BY_FOURNISSEUR =
            "SELECT * FROM lots WHERE id_fournisseur = ? ORDER BY date_peremption";

    private static final String SQL_FIND_BY_NUMERO_LOT =
            "SELECT * FROM lots WHERE numero_lot = ?";

    private static final String SQL_UPDATE_QUANTITE =
            "UPDATE lots SET quantite_stock = ? WHERE id_lot = ?";

    private static final String SQL_FIND_BY_DATE_RECEPTION =
            "SELECT * FROM lots WHERE DATE(date_reception) BETWEEN ? AND ? ORDER BY date_reception";

    @Override
    public Optional<Lot> findById(Integer id) throws DAOException {
        logger.debug("Recherche lot par ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLot(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche du lot ID: {}", id, e);
            throw new DAOException("Erreur lors de la recherche du lot", e);
        }
    }

    @Override
    public List<Lot> findAll() throws DAOException {
        logger.debug("Recherche de tous les lots");
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                lots.add(mapResultSetToLot(rs));
            }
            logger.debug("{} lots trouves", lots.size());
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de tous les lots", e);
            throw new DAOException("Erreur lors de la recherche des lots", e);
        }
    }

    @Override
    public Lot save(Lot lot) throws DAOException {
        logger.debug("Sauvegarde du lot: {}", lot.getNumeroLot());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            setLotParameters(ps, lot);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("La creation du lot a echoue, aucune ligne affectee");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    lot.setIdLot(generatedKeys.getInt(1));
                    logger.info("Lot cree avec ID: {}", lot.getIdLot());
                } else {
                    throw new DAOException("La creation du lot a echoue, aucun ID obtenu");
                }
            }

            return lot;

        } catch (SQLException e) {
            logger.error("Erreur lors de la sauvegarde du lot", e);
            throw new DAOException("Erreur lors de la sauvegarde du lot", e);
        }
    }

    @Override
    public void update(Lot lot) throws DAOException {
        logger.debug("Mise a jour du lot ID: {}", lot.getIdLot());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setLotParameters(ps, lot);
            ps.setInt(8, lot.getIdLot());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Lot non trouve pour mise a jour: " + lot.getIdLot());
            }
            logger.info("Lot mis a jour: {}", lot.getIdLot());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour du lot", e);
            throw new DAOException("Erreur lors de la mise a jour du lot", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        logger.debug("Suppression du lot ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Lot non trouve pour suppression: " + id);
            }
            logger.info("Lot supprime: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du lot", e);
            throw new DAOException("Erreur lors de la suppression du lot", e);
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
            logger.error("Erreur lors du comptage des lots", e);
            throw new DAOException("Erreur lors du comptage des lots", e);
        }
    }

    @Override
    public boolean existsById(Integer id) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            logger.error("Erreur lors de la verification d'existence du lot", e);
            throw new DAOException("Erreur lors de la verification d'existence", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>METHODE CLE POUR L'ALGORITHME FEFO</b>
     * </p>
     * <p>
     * Retourne les lots d'un medicament tries par date de peremption croissante.
     * Les lots avec la date de peremption la plus proche sont en premier.
     * Seuls les lots avec du stock disponible sont retournes.
     * </p>
     */
    @Override
    public List<Lot> findByMedicamentIdSortedByExpiration(int medicamentId) throws DAOException {
        logger.debug("Recherche lots FEFO pour medicament: {}", medicamentId);
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_MEDICAMENT_SORTED_BY_EXPIRATION)) {

            ps.setInt(1, medicamentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lots.add(mapResultSetToLot(rs));
                }
            }
            logger.debug("{} lots trouves pour medicament {} (tries par peremption)", lots.size(), medicamentId);
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche FEFO", e);
            throw new DAOException("Erreur lors de la recherche des lots par FEFO", e);
        }
    }

    @Override
    public List<Lot> findVendableByMedicament(int medicamentId) throws DAOException {
        logger.debug("Recherche lots vendables pour medicament: {}", medicamentId);
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_VENDABLE_BY_MEDICAMENT)) {

            ps.setInt(1, medicamentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lots.add(mapResultSetToLot(rs));
                }
            }
            logger.debug("{} lots vendables trouves pour medicament {}", lots.size(), medicamentId);
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des lots vendables", e);
            throw new DAOException("Erreur lors de la recherche des lots vendables", e);
        }
    }

    @Override
    public List<Lot> findExpiringBefore(LocalDate date) throws DAOException {
        logger.debug("Recherche lots expirant avant: {}", date);
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_EXPIRING_BEFORE)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lots.add(mapResultSetToLot(rs));
                }
            }
            logger.debug("{} lots expirant avant {}", lots.size(), date);
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des lots proches de peremption", e);
            throw new DAOException("Erreur lors de la recherche des lots proches de peremption", e);
        }
    }

    @Override
    public List<Lot> findExpired() throws DAOException {
        logger.debug("Recherche lots perimes");
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_EXPIRED)) {

            while (rs.next()) {
                lots.add(mapResultSetToLot(rs));
            }
            logger.debug("{} lots perimes trouves", lots.size());
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des lots perimes", e);
            throw new DAOException("Erreur lors de la recherche des lots perimes", e);
        }
    }

    @Override
    public int getTotalStockByMedicament(int medicamentId) throws DAOException {
        logger.debug("Calcul stock total pour medicament: {}", medicamentId);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_TOTAL_STOCK)) {

            ps.setInt(1, medicamentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            logger.error("Erreur lors du calcul du stock total", e);
            throw new DAOException("Erreur lors du calcul du stock total", e);
        }
    }

    @Override
    public List<Lot> findWithLowStock() throws DAOException {
        logger.debug("Recherche lots avec stock bas");
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_WITH_LOW_STOCK)) {

            while (rs.next()) {
                lots.add(mapResultSetToLot(rs));
            }
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des lots en stock bas", e);
            throw new DAOException("Erreur lors de la recherche des lots en stock bas", e);
        }
    }

    @Override
    public List<Lot> findByFournisseur(int fournisseurId) throws DAOException {
        logger.debug("Recherche lots par fournisseur: {}", fournisseurId);
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_FOURNISSEUR)) {

            ps.setInt(1, fournisseurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lots.add(mapResultSetToLot(rs));
                }
            }
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des lots par fournisseur", e);
            throw new DAOException("Erreur lors de la recherche des lots par fournisseur", e);
        }
    }

    @Override
    public Lot findByNumeroLot(String numeroLot) throws DAOException {
        logger.debug("Recherche lot par numero: {}", numeroLot);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NUMERO_LOT)) {

            ps.setString(1, numeroLot);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLot(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par numero de lot", e);
            throw new DAOException("Erreur lors de la recherche par numero de lot", e);
        }
    }

    @Override
    public void updateQuantite(int idLot, int nouvelleQuantite) throws DAOException {
        logger.debug("Mise a jour quantite lot {}: {}", idLot, nouvelleQuantite);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_QUANTITE)) {

            ps.setInt(1, nouvelleQuantite);
            ps.setInt(2, idLot);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Lot non trouve pour mise a jour quantite: " + idLot);
            }
            logger.info("Quantite mise a jour pour lot {}: {}", idLot, nouvelleQuantite);

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour de la quantite", e);
            throw new DAOException("Erreur lors de la mise a jour de la quantite", e);
        }
    }

    @Override
    public List<Lot> findByDateReception(LocalDate dateDebut, LocalDate dateFin) throws DAOException {
        logger.debug("Recherche lots recus entre {} et {}", dateDebut, dateFin);
        List<Lot> lots = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DATE_RECEPTION)) {

            ps.setDate(1, Date.valueOf(dateDebut));
            ps.setDate(2, Date.valueOf(dateFin));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lots.add(mapResultSetToLot(rs));
                }
            }
            return lots;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par date de reception", e);
            throw new DAOException("Erreur lors de la recherche par date de reception", e);
        }
    }

    /**
     * Mappe un ResultSet vers un objet Lot.
     *
     * @param rs le ResultSet positionne sur une ligne
     * @return l'objet Lot
     * @throws SQLException si une erreur d'acces aux donnees survient
     */
    private Lot mapResultSetToLot(ResultSet rs) throws SQLException {
        Lot lot = new Lot();
        lot.setIdLot(rs.getInt("id_lot"));
        lot.setIdMedicament(rs.getInt("id_medicament"));

        int idFournisseur = rs.getInt("id_fournisseur");
        if (!rs.wasNull()) {
            lot.setIdFournisseur(idFournisseur);
        }

        lot.setNumeroLot(rs.getString("numero_lot"));

        Date datePeremption = rs.getDate("date_peremption");
        if (datePeremption != null) {
            lot.setDatePeremption(datePeremption.toLocalDate());
        }

        Date dateFabrication = rs.getDate("date_fabrication");
        if (dateFabrication != null) {
            lot.setDateFabrication(dateFabrication.toLocalDate());
        }

        Timestamp dateReception = rs.getTimestamp("date_reception");
        if (dateReception != null) {
            lot.setDateReception(dateReception.toLocalDateTime());
        }

        lot.setQuantiteStock(rs.getInt("quantite_stock"));

        BigDecimal prixAchat = rs.getBigDecimal("prix_achat");
        if (prixAchat != null) {
            lot.setPrixAchat(prixAchat);
        }

        return lot;
    }

    /**
     * Configure les parametres d'un PreparedStatement pour un Lot.
     *
     * @param ps  le PreparedStatement
     * @param lot le lot
     * @throws SQLException si une erreur survient
     */
    private void setLotParameters(PreparedStatement ps, Lot lot) throws SQLException {
        ps.setInt(1, lot.getIdMedicament());

        if (lot.getIdFournisseur() != null) {
            ps.setInt(2, lot.getIdFournisseur());
        } else {
            ps.setNull(2, Types.INTEGER);
        }

        ps.setString(3, lot.getNumeroLot());
        ps.setDate(4, Date.valueOf(lot.getDatePeremption()));

        if (lot.getDateFabrication() != null) {
            ps.setDate(5, Date.valueOf(lot.getDateFabrication()));
        } else {
            ps.setNull(5, Types.DATE);
        }

        ps.setInt(6, lot.getQuantiteStock());

        if (lot.getPrixAchat() != null) {
            ps.setBigDecimal(7, lot.getPrixAchat());
        } else {
            ps.setNull(7, Types.DECIMAL);
        }
    }
}
