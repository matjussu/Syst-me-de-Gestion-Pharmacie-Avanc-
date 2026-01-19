package com.sgpa.dao.impl;

import com.sgpa.dao.MedicamentDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Medicament;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC de l'interface {@link MedicamentDAO}.
 * <p>
 * Gere les operations CRUD et les recherches specifiques sur les medicaments.
 * Utilise des PreparedStatement pour prevenir les injections SQL.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class MedicamentDAOImpl implements MedicamentDAO {

    private static final Logger logger = LoggerFactory.getLogger(MedicamentDAOImpl.class);

    // Requetes SQL
    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM medicaments WHERE id_medicament = ?";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM medicaments ORDER BY nom_commercial";

    private static final String SQL_FIND_ALL_ACTIVE =
            "SELECT * FROM medicaments WHERE actif = TRUE ORDER BY nom_commercial";

    private static final String SQL_INSERT =
            "INSERT INTO medicaments (nom_commercial, principe_actif, forme_galenique, dosage, " +
            "prix_public, necessite_ordonnance, seuil_min, description, actif) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE medicaments SET nom_commercial = ?, principe_actif = ?, forme_galenique = ?, " +
            "dosage = ?, prix_public = ?, necessite_ordonnance = ?, seuil_min = ?, description = ?, " +
            "actif = ? WHERE id_medicament = ?";

    private static final String SQL_DELETE =
            "DELETE FROM medicaments WHERE id_medicament = ?";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM medicaments";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM medicaments WHERE id_medicament = ?";

    private static final String SQL_FIND_BY_NOM =
            "SELECT * FROM medicaments WHERE nom_commercial LIKE ? ORDER BY nom_commercial";

    private static final String SQL_FIND_BY_NOM_EXACT =
            "SELECT * FROM medicaments WHERE nom_commercial = ?";

    private static final String SQL_FIND_BY_PRINCIPE_ACTIF =
            "SELECT * FROM medicaments WHERE principe_actif LIKE ? ORDER BY nom_commercial";

    private static final String SQL_FIND_REQUIRING_PRESCRIPTION =
            "SELECT * FROM medicaments WHERE necessite_ordonnance = TRUE AND actif = TRUE ORDER BY nom_commercial";

    private static final String SQL_FIND_BELOW_THRESHOLD =
            "SELECT m.* FROM medicaments m " +
            "LEFT JOIN (SELECT id_medicament, SUM(quantite_stock) AS stock_total FROM lots GROUP BY id_medicament) l " +
            "ON m.id_medicament = l.id_medicament " +
            "WHERE m.actif = TRUE AND (l.stock_total IS NULL OR l.stock_total < m.seuil_min) " +
            "ORDER BY m.nom_commercial";

    private static final String SQL_GET_STOCK_TOTAL =
            "SELECT COALESCE(SUM(quantite_stock), 0) FROM lots WHERE id_medicament = ?";

    @Override
    public Optional<Medicament> findById(Integer id) throws DAOException {
        logger.debug("Recherche medicament par ID: {}", id);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMedicament(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche du medicament ID: {}", id, e);
            throw new DAOException("Erreur lors de la recherche du medicament", e);
        }
    }

    @Override
    public List<Medicament> findAll() throws DAOException {
        logger.debug("Recherche de tous les medicaments");
        List<Medicament> medicaments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                medicaments.add(mapResultSetToMedicament(rs));
            }
            logger.debug("{} medicaments trouves", medicaments.size());
            return medicaments;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de tous les medicaments", e);
            throw new DAOException("Erreur lors de la recherche des medicaments", e);
        }
    }

    @Override
    public List<Medicament> findAllActive() throws DAOException {
        logger.debug("Recherche des medicaments actifs");
        List<Medicament> medicaments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL_ACTIVE)) {

            while (rs.next()) {
                medicaments.add(mapResultSetToMedicament(rs));
            }
            return medicaments;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des medicaments actifs", e);
            throw new DAOException("Erreur lors de la recherche des medicaments actifs", e);
        }
    }

    @Override
    public Medicament save(Medicament medicament) throws DAOException {
        logger.debug("Sauvegarde du medicament: {}", medicament.getNomCommercial());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            setMedicamentParameters(ps, medicament);
            ps.setBoolean(9, medicament.isActif());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("La creation du medicament a echoue, aucune ligne affectee");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    medicament.setIdMedicament(generatedKeys.getInt(1));
                    logger.info("Medicament cree avec ID: {}", medicament.getIdMedicament());
                } else {
                    throw new DAOException("La creation du medicament a echoue, aucun ID obtenu");
                }
            }

            return medicament;

        } catch (SQLException e) {
            logger.error("Erreur lors de la sauvegarde du medicament", e);
            throw new DAOException("Erreur lors de la sauvegarde du medicament", e);
        }
    }

    @Override
    public void update(Medicament medicament) throws DAOException {
        logger.debug("Mise a jour du medicament ID: {}", medicament.getIdMedicament());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            setMedicamentParameters(ps, medicament);
            ps.setBoolean(9, medicament.isActif());
            ps.setInt(10, medicament.getIdMedicament());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Medicament non trouve pour mise a jour: " + medicament.getIdMedicament());
            }
            logger.info("Medicament mis a jour: {}", medicament.getIdMedicament());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour du medicament", e);
            throw new DAOException("Erreur lors de la mise a jour du medicament", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        logger.debug("Suppression du medicament ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Medicament non trouve pour suppression: " + id);
            }
            logger.info("Medicament supprime: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du medicament", e);
            throw new DAOException("Erreur lors de la suppression du medicament", e);
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
            logger.error("Erreur lors du comptage des medicaments", e);
            throw new DAOException("Erreur lors du comptage des medicaments", e);
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
            logger.error("Erreur lors de la verification d'existence du medicament", e);
            throw new DAOException("Erreur lors de la verification d'existence", e);
        }
    }

    @Override
    public List<Medicament> findByNom(String nom) throws DAOException {
        logger.debug("Recherche medicaments par nom: {}", nom);
        List<Medicament> medicaments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NOM)) {

            ps.setString(1, "%" + nom + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicaments.add(mapResultSetToMedicament(rs));
                }
            }
            return medicaments;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par nom", e);
            throw new DAOException("Erreur lors de la recherche par nom", e);
        }
    }

    @Override
    public Medicament findByNomCommercialExact(String nomCommercial) throws DAOException {
        logger.debug("Recherche medicament par nom exact: {}", nomCommercial);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NOM_EXACT)) {

            ps.setString(1, nomCommercial);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMedicament(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par nom exact", e);
            throw new DAOException("Erreur lors de la recherche par nom exact", e);
        }
    }

    @Override
    public List<Medicament> findByPrincipeActif(String principeActif) throws DAOException {
        logger.debug("Recherche medicaments par principe actif: {}", principeActif);
        List<Medicament> medicaments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_PRINCIPE_ACTIF)) {

            ps.setString(1, "%" + principeActif + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    medicaments.add(mapResultSetToMedicament(rs));
                }
            }
            return medicaments;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par principe actif", e);
            throw new DAOException("Erreur lors de la recherche par principe actif", e);
        }
    }

    @Override
    public List<Medicament> findBelowThreshold() throws DAOException {
        logger.debug("Recherche medicaments en stock bas");
        List<Medicament> medicaments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_BELOW_THRESHOLD)) {

            while (rs.next()) {
                medicaments.add(mapResultSetToMedicament(rs));
            }
            logger.debug("{} medicaments en stock bas", medicaments.size());
            return medicaments;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des medicaments en stock bas", e);
            throw new DAOException("Erreur lors de la recherche des medicaments en stock bas", e);
        }
    }

    @Override
    public List<Medicament> findRequiringPrescription() throws DAOException {
        logger.debug("Recherche medicaments necessitant ordonnance");
        List<Medicament> medicaments = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_REQUIRING_PRESCRIPTION)) {

            while (rs.next()) {
                medicaments.add(mapResultSetToMedicament(rs));
            }
            return medicaments;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des medicaments sur ordonnance", e);
            throw new DAOException("Erreur lors de la recherche des medicaments sur ordonnance", e);
        }
    }

    @Override
    public int getStockTotal(int idMedicament) throws DAOException {
        logger.debug("Calcul stock total pour medicament: {}", idMedicament);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_GET_STOCK_TOTAL)) {

            ps.setInt(1, idMedicament);
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

    /**
     * Mappe un ResultSet vers un objet Medicament.
     *
     * @param rs le ResultSet positionne sur une ligne
     * @return l'objet Medicament
     * @throws SQLException si une erreur d'acces aux donnees survient
     */
    private Medicament mapResultSetToMedicament(ResultSet rs) throws SQLException {
        Medicament medicament = new Medicament();
        medicament.setIdMedicament(rs.getInt("id_medicament"));
        medicament.setNomCommercial(rs.getString("nom_commercial"));
        medicament.setPrincipeActif(rs.getString("principe_actif"));
        medicament.setFormeGalenique(rs.getString("forme_galenique"));
        medicament.setDosage(rs.getString("dosage"));
        medicament.setPrixPublic(rs.getBigDecimal("prix_public"));
        medicament.setNecessiteOrdonnance(rs.getBoolean("necessite_ordonnance"));
        medicament.setSeuilMin(rs.getInt("seuil_min"));
        medicament.setDescription(rs.getString("description"));
        medicament.setActif(rs.getBoolean("actif"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            medicament.setDateCreation(dateCreation.toLocalDateTime());
        }

        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            medicament.setDateModification(dateModification.toLocalDateTime());
        }

        return medicament;
    }

    /**
     * Configure les parametres d'un PreparedStatement pour un Medicament.
     *
     * @param ps         le PreparedStatement
     * @param medicament le medicament
     * @throws SQLException si une erreur survient
     */
    private void setMedicamentParameters(PreparedStatement ps, Medicament medicament) throws SQLException {
        ps.setString(1, medicament.getNomCommercial());
        ps.setString(2, medicament.getPrincipeActif());
        ps.setString(3, medicament.getFormeGalenique());
        ps.setString(4, medicament.getDosage());
        ps.setBigDecimal(5, medicament.getPrixPublic());
        ps.setBoolean(6, medicament.isNecessiteOrdonnance());
        ps.setInt(7, medicament.getSeuilMin());
        ps.setString(8, medicament.getDescription());
    }
}
