package com.sgpa.dao.impl;

import com.sgpa.dao.PromotionDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Promotion;
import com.sgpa.model.enums.TypePromotion;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC de l'interface {@link PromotionDAO}.
 * <p>
 * Gere les operations CRUD sur les promotions.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class PromotionDAOImpl implements PromotionDAO {

    private static final Logger logger = LoggerFactory.getLogger(PromotionDAOImpl.class);

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM promotions WHERE id_promotion = ?";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM promotions ORDER BY date_debut DESC";

    private static final String SQL_FIND_BY_CODE =
            "SELECT * FROM promotions WHERE code_promo = ?";

    private static final String SQL_FIND_ACTIVES =
            "SELECT * FROM promotions WHERE actif = TRUE " +
            "AND date_debut <= CURDATE() AND date_fin >= CURDATE() ORDER BY nom";

    private static final String SQL_FIND_EXPIREES =
            "SELECT * FROM promotions WHERE date_fin < CURDATE() ORDER BY date_fin DESC";

    private static final String SQL_FIND_FUTURES =
            "SELECT * FROM promotions WHERE date_debut > CURDATE() ORDER BY date_debut";

    private static final String SQL_FIND_BY_MEDICAMENT =
            "SELECT p.* FROM promotions p " +
            "JOIN promotion_medicaments pm ON p.id_promotion = pm.id_promotion " +
            "WHERE pm.id_medicament = ? AND p.actif = TRUE " +
            "AND p.date_debut <= CURDATE() AND p.date_fin >= CURDATE()";

    private static final String SQL_INSERT =
            "INSERT INTO promotions (code_promo, nom, description, type_promotion, valeur, " +
            "quantite_requise, quantite_offerte, date_debut, date_fin, actif, usage_unique, " +
            "cumulable, cree_par) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE promotions SET code_promo = ?, nom = ?, description = ?, type_promotion = ?, " +
            "valeur = ?, quantite_requise = ?, quantite_offerte = ?, date_debut = ?, date_fin = ?, " +
            "actif = ?, usage_unique = ?, cumulable = ? WHERE id_promotion = ?";

    private static final String SQL_DELETE =
            "DELETE FROM promotions WHERE id_promotion = ?";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM promotions";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM promotions WHERE id_promotion = ?";

    private static final String SQL_SET_ACTIF =
            "UPDATE promotions SET actif = ? WHERE id_promotion = ?";

    private static final String SQL_ADD_MEDICAMENT =
            "INSERT INTO promotion_medicaments (id_promotion, id_medicament) VALUES (?, ?)";

    private static final String SQL_REMOVE_MEDICAMENT =
            "DELETE FROM promotion_medicaments WHERE id_promotion = ? AND id_medicament = ?";

    private static final String SQL_REMOVE_ALL_MEDICAMENTS =
            "DELETE FROM promotion_medicaments WHERE id_promotion = ?";

    private static final String SQL_GET_MEDICAMENT_IDS =
            "SELECT id_medicament FROM promotion_medicaments WHERE id_promotion = ?";

    private static final String SQL_INSERT_UTILISATION =
            "INSERT INTO utilisation_promotions (id_promotion, id_vente, id_ligne_vente, montant_reduction) " +
            "VALUES (?, ?, ?, ?)";

    private static final String SQL_COUNT_UTILISATIONS =
            "SELECT COUNT(*) FROM utilisation_promotions WHERE id_promotion = ?";

    @Override
    public Optional<Promotion> findById(Integer id) throws DAOException {
        logger.debug("Recherche promotion par ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Promotion promotion = mapResultSetToPromotion(rs);
                    promotion.setMedicamentIds(getMedicamentIds(id));
                    return Optional.of(promotion);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de la promotion ID: {}", id, e);
            throw new DAOException("Erreur lors de la recherche de la promotion", e);
        }
    }

    @Override
    public Optional<Promotion> findByCode(String codePromo) throws DAOException {
        logger.debug("Recherche promotion par code: {}", codePromo);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CODE)) {

            ps.setString(1, codePromo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Promotion promotion = mapResultSetToPromotion(rs);
                    promotion.setMedicamentIds(getMedicamentIds(promotion.getIdPromotion()));
                    return Optional.of(promotion);
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de la promotion par code: {}", codePromo, e);
            throw new DAOException("Erreur lors de la recherche de la promotion", e);
        }
    }

    @Override
    public List<Promotion> findAll() throws DAOException {
        logger.debug("Recherche de toutes les promotions");
        List<Promotion> promotions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                Promotion promotion = mapResultSetToPromotion(rs);
                promotion.setMedicamentIds(getMedicamentIds(promotion.getIdPromotion()));
                promotions.add(promotion);
            }
            return promotions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de toutes les promotions", e);
            throw new DAOException("Erreur lors de la recherche des promotions", e);
        }
    }

    @Override
    public List<Promotion> findActives() throws DAOException {
        logger.debug("Recherche des promotions actives");
        List<Promotion> promotions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ACTIVES)) {

            while (rs.next()) {
                Promotion promotion = mapResultSetToPromotion(rs);
                promotion.setMedicamentIds(getMedicamentIds(promotion.getIdPromotion()));
                promotions.add(promotion);
            }
            return promotions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des promotions actives", e);
            throw new DAOException("Erreur lors de la recherche des promotions actives", e);
        }
    }

    @Override
    public List<Promotion> findByMedicament(int idMedicament) throws DAOException {
        logger.debug("Recherche promotions pour medicament: {}", idMedicament);
        List<Promotion> promotions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_MEDICAMENT)) {

            ps.setInt(1, idMedicament);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Promotion promotion = mapResultSetToPromotion(rs);
                    promotion.setMedicamentIds(getMedicamentIds(promotion.getIdPromotion()));
                    promotions.add(promotion);
                }
            }
            return promotions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des promotions par medicament", e);
            throw new DAOException("Erreur lors de la recherche des promotions", e);
        }
    }

    @Override
    public List<Promotion> findExpirees() throws DAOException {
        logger.debug("Recherche des promotions expirees");
        List<Promotion> promotions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_EXPIREES)) {

            while (rs.next()) {
                promotions.add(mapResultSetToPromotion(rs));
            }
            return promotions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des promotions expirees", e);
            throw new DAOException("Erreur lors de la recherche des promotions expirees", e);
        }
    }

    @Override
    public List<Promotion> findFutures() throws DAOException {
        logger.debug("Recherche des promotions futures");
        List<Promotion> promotions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_FUTURES)) {

            while (rs.next()) {
                promotions.add(mapResultSetToPromotion(rs));
            }
            return promotions;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des promotions futures", e);
            throw new DAOException("Erreur lors de la recherche des promotions futures", e);
        }
    }

    @Override
    public Promotion save(Promotion promotion) throws DAOException {
        logger.debug("Sauvegarde de la promotion: {}", promotion.getNom());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, promotion.getCodePromo());
            ps.setString(2, promotion.getNom());
            ps.setString(3, promotion.getDescription());
            ps.setString(4, promotion.getTypePromotion().name());
            ps.setBigDecimal(5, promotion.getValeur());
            ps.setInt(6, promotion.getQuantiteRequise());
            ps.setInt(7, promotion.getQuantiteOfferte());
            ps.setDate(8, Date.valueOf(promotion.getDateDebut()));
            ps.setDate(9, Date.valueOf(promotion.getDateFin()));
            ps.setBoolean(10, promotion.isActif());
            ps.setBoolean(11, promotion.isUsageUnique());
            ps.setBoolean(12, promotion.isCumulable());
            if (promotion.getCreePar() != null) {
                ps.setInt(13, promotion.getCreePar());
            } else {
                ps.setNull(13, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("La creation de la promotion a echoue");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    promotion.setIdPromotion(generatedKeys.getInt(1));
                    logger.info("Promotion creee avec ID: {}", promotion.getIdPromotion());

                    // Sauvegarder les medicaments associes
                    if (promotion.getMedicamentIds() != null && !promotion.getMedicamentIds().isEmpty()) {
                        for (Integer idMedicament : promotion.getMedicamentIds()) {
                            addMedicament(promotion.getIdPromotion(), idMedicament);
                        }
                    }
                }
            }

            return promotion;

        } catch (SQLException e) {
            logger.error("Erreur lors de la sauvegarde de la promotion", e);
            throw new DAOException("Erreur lors de la sauvegarde de la promotion", e);
        }
    }

    @Override
    public void update(Promotion promotion) throws DAOException {
        logger.debug("Mise a jour de la promotion ID: {}", promotion.getIdPromotion());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, promotion.getCodePromo());
            ps.setString(2, promotion.getNom());
            ps.setString(3, promotion.getDescription());
            ps.setString(4, promotion.getTypePromotion().name());
            ps.setBigDecimal(5, promotion.getValeur());
            ps.setInt(6, promotion.getQuantiteRequise());
            ps.setInt(7, promotion.getQuantiteOfferte());
            ps.setDate(8, Date.valueOf(promotion.getDateDebut()));
            ps.setDate(9, Date.valueOf(promotion.getDateFin()));
            ps.setBoolean(10, promotion.isActif());
            ps.setBoolean(11, promotion.isUsageUnique());
            ps.setBoolean(12, promotion.isCumulable());
            ps.setInt(13, promotion.getIdPromotion());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Promotion non trouvee pour mise a jour");
            }

            // Mettre a jour les medicaments associes
            removeAllMedicaments(promotion.getIdPromotion());
            if (promotion.getMedicamentIds() != null) {
                for (Integer idMedicament : promotion.getMedicamentIds()) {
                    addMedicament(promotion.getIdPromotion(), idMedicament);
                }
            }

            logger.info("Promotion mise a jour: {}", promotion.getIdPromotion());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour de la promotion", e);
            throw new DAOException("Erreur lors de la mise a jour de la promotion", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        logger.debug("Suppression de la promotion ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            // Les medicaments associes sont supprimes en cascade
            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Promotion non trouvee pour suppression");
            }
            logger.info("Promotion supprimee: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression de la promotion", e);
            throw new DAOException("Erreur lors de la suppression de la promotion", e);
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
            throw new DAOException("Erreur lors du comptage des promotions", e);
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
            throw new DAOException("Erreur lors de la verification d'existence", e);
        }
    }

    @Override
    public void setActif(int idPromotion, boolean actif) throws DAOException {
        logger.debug("Changement statut actif promotion {}: {}", idPromotion, actif);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SET_ACTIF)) {

            ps.setBoolean(1, actif);
            ps.setInt(2, idPromotion);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Promotion non trouvee");
            }

        } catch (SQLException e) {
            logger.error("Erreur lors du changement de statut", e);
            throw new DAOException("Erreur lors du changement de statut", e);
        }
    }

    @Override
    public void addMedicament(int idPromotion, int idMedicament) throws DAOException {
        logger.debug("Ajout medicament {} a promotion {}", idMedicament, idPromotion);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_ADD_MEDICAMENT)) {

            ps.setInt(1, idPromotion);
            ps.setInt(2, idMedicament);
            ps.executeUpdate();

        } catch (SQLException e) {
            // Ignorer si deja existe (cle primaire dupliquee)
            if (!e.getMessage().contains("Duplicate")) {
                logger.error("Erreur lors de l'ajout du medicament", e);
                throw new DAOException("Erreur lors de l'ajout du medicament", e);
            }
        }
    }

    @Override
    public void removeMedicament(int idPromotion, int idMedicament) throws DAOException {
        logger.debug("Retrait medicament {} de promotion {}", idMedicament, idPromotion);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_REMOVE_MEDICAMENT)) {

            ps.setInt(1, idPromotion);
            ps.setInt(2, idMedicament);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Erreur lors du retrait du medicament", e);
            throw new DAOException("Erreur lors du retrait du medicament", e);
        }
    }

    @Override
    public void removeAllMedicaments(int idPromotion) throws DAOException {
        logger.debug("Suppression tous medicaments de promotion {}", idPromotion);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_REMOVE_ALL_MEDICAMENTS)) {

            ps.setInt(1, idPromotion);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression des medicaments", e);
            throw new DAOException("Erreur lors de la suppression des medicaments", e);
        }
    }

    @Override
    public List<Integer> getMedicamentIds(int idPromotion) throws DAOException {
        List<Integer> ids = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_MEDICAMENT_IDS)) {

            ps.setInt(1, idPromotion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id_medicament"));
                }
            }
            return ids;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recuperation des medicaments", e);
            throw new DAOException("Erreur lors de la recuperation des medicaments", e);
        }
    }

    @Override
    public void enregistrerUtilisation(int idPromotion, int idVente, Integer idLigneVente,
                                        BigDecimal montantReduction) throws DAOException {
        logger.debug("Enregistrement utilisation promo {} vente {}", idPromotion, idVente);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_UTILISATION)) {

            ps.setInt(1, idPromotion);
            ps.setInt(2, idVente);
            if (idLigneVente != null) {
                ps.setInt(3, idLigneVente);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setBigDecimal(4, montantReduction);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Erreur lors de l'enregistrement de l'utilisation", e);
            throw new DAOException("Erreur lors de l'enregistrement de l'utilisation", e);
        }
    }

    @Override
    public int countUtilisations(int idPromotion) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_UTILISATIONS)) {

            ps.setInt(1, idPromotion);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;

        } catch (SQLException e) {
            throw new DAOException("Erreur lors du comptage des utilisations", e);
        }
    }

    private Promotion mapResultSetToPromotion(ResultSet rs) throws SQLException {
        Promotion promotion = new Promotion();
        promotion.setIdPromotion(rs.getInt("id_promotion"));
        promotion.setCodePromo(rs.getString("code_promo"));
        promotion.setNom(rs.getString("nom"));
        promotion.setDescription(rs.getString("description"));
        promotion.setTypePromotion(TypePromotion.valueOf(rs.getString("type_promotion")));
        promotion.setValeur(rs.getBigDecimal("valeur"));
        promotion.setQuantiteRequise(rs.getInt("quantite_requise"));
        promotion.setQuantiteOfferte(rs.getInt("quantite_offerte"));

        Date dateDebut = rs.getDate("date_debut");
        if (dateDebut != null) {
            promotion.setDateDebut(dateDebut.toLocalDate());
        }

        Date dateFin = rs.getDate("date_fin");
        if (dateFin != null) {
            promotion.setDateFin(dateFin.toLocalDate());
        }

        promotion.setActif(rs.getBoolean("actif"));
        promotion.setUsageUnique(rs.getBoolean("usage_unique"));
        promotion.setCumulable(rs.getBoolean("cumulable"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            promotion.setDateCreation(dateCreation.toLocalDateTime());
        }

        int creePar = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            promotion.setCreePar(creePar);
        }

        return promotion;
    }
}
