package com.sgpa.dao.impl;

import com.sgpa.dao.FournisseurDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Fournisseur;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC de l'interface {@link FournisseurDAO}.
 * <p>
 * Gere les operations CRUD sur les fournisseurs.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class FournisseurDAOImpl implements FournisseurDAO {

    private static final Logger logger = LoggerFactory.getLogger(FournisseurDAOImpl.class);

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM fournisseurs WHERE id_fournisseur = ?";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM fournisseurs ORDER BY nom";

    private static final String SQL_FIND_ALL_ACTIVE =
            "SELECT * FROM fournisseurs WHERE actif = TRUE ORDER BY nom";

    private static final String SQL_INSERT =
            "INSERT INTO fournisseurs (nom, contact, adresse, telephone, email, actif) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE fournisseurs SET nom = ?, contact = ?, adresse = ?, telephone = ?, " +
            "email = ?, actif = ? WHERE id_fournisseur = ?";

    private static final String SQL_DELETE =
            "DELETE FROM fournisseurs WHERE id_fournisseur = ?";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM fournisseurs";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM fournisseurs WHERE id_fournisseur = ?";

    private static final String SQL_FIND_BY_NOM =
            "SELECT * FROM fournisseurs WHERE nom LIKE ? ORDER BY nom";

    private static final String SQL_FIND_BY_NOM_EXACT =
            "SELECT * FROM fournisseurs WHERE nom = ?";

    private static final String SQL_SET_ACTIF =
            "UPDATE fournisseurs SET actif = ? WHERE id_fournisseur = ?";

    @Override
    public Optional<Fournisseur> findById(Integer id) throws DAOException {
        logger.debug("Recherche fournisseur par ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToFournisseur(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche du fournisseur ID: {}", id, e);
            throw new DAOException("Erreur lors de la recherche du fournisseur", e);
        }
    }

    @Override
    public List<Fournisseur> findAll() throws DAOException {
        logger.debug("Recherche de tous les fournisseurs");
        List<Fournisseur> fournisseurs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                fournisseurs.add(mapResultSetToFournisseur(rs));
            }
            return fournisseurs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de tous les fournisseurs", e);
            throw new DAOException("Erreur lors de la recherche des fournisseurs", e);
        }
    }

    @Override
    public List<Fournisseur> findAllActive() throws DAOException {
        logger.debug("Recherche des fournisseurs actifs");
        List<Fournisseur> fournisseurs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL_ACTIVE)) {

            while (rs.next()) {
                fournisseurs.add(mapResultSetToFournisseur(rs));
            }
            return fournisseurs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des fournisseurs actifs", e);
            throw new DAOException("Erreur lors de la recherche des fournisseurs actifs", e);
        }
    }

    @Override
    public Fournisseur save(Fournisseur fournisseur) throws DAOException {
        logger.debug("Sauvegarde du fournisseur: {}", fournisseur.getNom());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fournisseur.getNom());
            ps.setString(2, fournisseur.getContact());
            ps.setString(3, fournisseur.getAdresse());
            ps.setString(4, fournisseur.getTelephone());
            ps.setString(5, fournisseur.getEmail());
            ps.setBoolean(6, fournisseur.isActif());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("La creation du fournisseur a echoue");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    fournisseur.setIdFournisseur(generatedKeys.getInt(1));
                    logger.info("Fournisseur cree avec ID: {}", fournisseur.getIdFournisseur());
                }
            }

            return fournisseur;

        } catch (SQLException e) {
            logger.error("Erreur lors de la sauvegarde du fournisseur", e);
            throw new DAOException("Erreur lors de la sauvegarde du fournisseur", e);
        }
    }

    @Override
    public void update(Fournisseur fournisseur) throws DAOException {
        logger.debug("Mise a jour du fournisseur ID: {}", fournisseur.getIdFournisseur());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, fournisseur.getNom());
            ps.setString(2, fournisseur.getContact());
            ps.setString(3, fournisseur.getAdresse());
            ps.setString(4, fournisseur.getTelephone());
            ps.setString(5, fournisseur.getEmail());
            ps.setBoolean(6, fournisseur.isActif());
            ps.setInt(7, fournisseur.getIdFournisseur());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Fournisseur non trouve pour mise a jour");
            }
            logger.info("Fournisseur mis a jour: {}", fournisseur.getIdFournisseur());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour du fournisseur", e);
            throw new DAOException("Erreur lors de la mise a jour du fournisseur", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        logger.debug("Suppression du fournisseur ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Fournisseur non trouve pour suppression");
            }
            logger.info("Fournisseur supprime: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression du fournisseur", e);
            throw new DAOException("Erreur lors de la suppression du fournisseur", e);
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
            throw new DAOException("Erreur lors du comptage des fournisseurs", e);
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
    public List<Fournisseur> findByNom(String nom) throws DAOException {
        logger.debug("Recherche fournisseurs par nom: {}", nom);
        List<Fournisseur> fournisseurs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NOM)) {

            ps.setString(1, "%" + nom + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fournisseurs.add(mapResultSetToFournisseur(rs));
                }
            }
            return fournisseurs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par nom", e);
            throw new DAOException("Erreur lors de la recherche par nom", e);
        }
    }

    @Override
    public Fournisseur findByNomExact(String nom) throws DAOException {
        logger.debug("Recherche fournisseur par nom exact: {}", nom);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NOM_EXACT)) {

            ps.setString(1, nom);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFournisseur(rs);
                }
            }
            return null;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par nom exact", e);
            throw new DAOException("Erreur lors de la recherche par nom exact", e);
        }
    }

    @Override
    public void setActif(int idFournisseur, boolean actif) throws DAOException {
        logger.debug("Changement statut actif fournisseur {}: {}", idFournisseur, actif);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SET_ACTIF)) {

            ps.setBoolean(1, actif);
            ps.setInt(2, idFournisseur);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Fournisseur non trouve");
            }

        } catch (SQLException e) {
            logger.error("Erreur lors du changement de statut", e);
            throw new DAOException("Erreur lors du changement de statut", e);
        }
    }

    private Fournisseur mapResultSetToFournisseur(ResultSet rs) throws SQLException {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setIdFournisseur(rs.getInt("id_fournisseur"));
        fournisseur.setNom(rs.getString("nom"));
        fournisseur.setContact(rs.getString("contact"));
        fournisseur.setAdresse(rs.getString("adresse"));
        fournisseur.setTelephone(rs.getString("telephone"));
        fournisseur.setEmail(rs.getString("email"));
        fournisseur.setActif(rs.getBoolean("actif"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            fournisseur.setDateCreation(dateCreation.toLocalDateTime());
        }

        return fournisseur;
    }
}
