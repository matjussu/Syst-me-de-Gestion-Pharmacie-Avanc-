package com.sgpa.dao.impl;

import com.sgpa.dao.UtilisateurDAO;
import com.sgpa.exception.DAOException;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.Role;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation JDBC de l'interface {@link UtilisateurDAO}.
 * <p>
 * Gere les operations CRUD et l'authentification des utilisateurs.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class UtilisateurDAOImpl implements UtilisateurDAO {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurDAOImpl.class);

    private static final String SQL_FIND_BY_ID =
            "SELECT * FROM utilisateurs WHERE id_utilisateur = ?";

    private static final String SQL_FIND_ALL =
            "SELECT * FROM utilisateurs ORDER BY nom_complet";

    private static final String SQL_FIND_ALL_ACTIVE =
            "SELECT * FROM utilisateurs WHERE actif = TRUE ORDER BY nom_complet";

    private static final String SQL_INSERT =
            "INSERT INTO utilisateurs (nom_utilisateur, mot_de_passe, role, nom_complet, actif) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_UPDATE =
            "UPDATE utilisateurs SET nom_utilisateur = ?, role = ?, nom_complet = ?, actif = ? " +
            "WHERE id_utilisateur = ?";

    private static final String SQL_DELETE =
            "DELETE FROM utilisateurs WHERE id_utilisateur = ?";

    private static final String SQL_COUNT =
            "SELECT COUNT(*) FROM utilisateurs";

    private static final String SQL_EXISTS =
            "SELECT COUNT(*) FROM utilisateurs WHERE id_utilisateur = ?";

    private static final String SQL_FIND_BY_NOM_UTILISATEUR =
            "SELECT * FROM utilisateurs WHERE nom_utilisateur = ?";

    private static final String SQL_FIND_BY_ROLE =
            "SELECT * FROM utilisateurs WHERE role = ? ORDER BY nom_complet";

    private static final String SQL_EXISTS_BY_NOM_UTILISATEUR =
            "SELECT COUNT(*) FROM utilisateurs WHERE nom_utilisateur = ?";

    private static final String SQL_UPDATE_DERNIERE_CONNEXION =
            "UPDATE utilisateurs SET derniere_connexion = CURRENT_TIMESTAMP WHERE id_utilisateur = ?";

    private static final String SQL_UPDATE_MOT_DE_PASSE =
            "UPDATE utilisateurs SET mot_de_passe = ? WHERE id_utilisateur = ?";

    private static final String SQL_SET_ACTIF =
            "UPDATE utilisateurs SET actif = ? WHERE id_utilisateur = ?";

    @Override
    public Optional<Utilisateur> findById(Integer id) throws DAOException {
        logger.debug("Recherche utilisateur par ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUtilisateur(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de l'utilisateur ID: {}", id, e);
            throw new DAOException("Erreur lors de la recherche de l'utilisateur", e);
        }
    }

    @Override
    public List<Utilisateur> findAll() throws DAOException {
        logger.debug("Recherche de tous les utilisateurs");
        List<Utilisateur> utilisateurs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL)) {

            while (rs.next()) {
                utilisateurs.add(mapResultSetToUtilisateur(rs));
            }
            return utilisateurs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche de tous les utilisateurs", e);
            throw new DAOException("Erreur lors de la recherche des utilisateurs", e);
        }
    }

    @Override
    public List<Utilisateur> findAllActive() throws DAOException {
        logger.debug("Recherche des utilisateurs actifs");
        List<Utilisateur> utilisateurs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_FIND_ALL_ACTIVE)) {

            while (rs.next()) {
                utilisateurs.add(mapResultSetToUtilisateur(rs));
            }
            return utilisateurs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche des utilisateurs actifs", e);
            throw new DAOException("Erreur lors de la recherche des utilisateurs actifs", e);
        }
    }

    @Override
    public Utilisateur save(Utilisateur utilisateur) throws DAOException {
        logger.debug("Sauvegarde de l'utilisateur: {}", utilisateur.getNomUtilisateur());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, utilisateur.getNomUtilisateur());
            ps.setString(2, utilisateur.getMotDePasse());
            ps.setString(3, utilisateur.getRole().name());
            ps.setString(4, utilisateur.getNomComplet());
            ps.setBoolean(5, utilisateur.isActif());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("La creation de l'utilisateur a echoue");
            }

            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    utilisateur.setIdUtilisateur(generatedKeys.getInt(1));
                    logger.info("Utilisateur cree avec ID: {}", utilisateur.getIdUtilisateur());
                }
            }

            return utilisateur;

        } catch (SQLException e) {
            logger.error("Erreur lors de la sauvegarde de l'utilisateur", e);
            throw new DAOException("Erreur lors de la sauvegarde de l'utilisateur", e);
        }
    }

    @Override
    public void update(Utilisateur utilisateur) throws DAOException {
        logger.debug("Mise a jour de l'utilisateur ID: {}", utilisateur.getIdUtilisateur());

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, utilisateur.getNomUtilisateur());
            ps.setString(2, utilisateur.getRole().name());
            ps.setString(3, utilisateur.getNomComplet());
            ps.setBoolean(4, utilisateur.isActif());
            ps.setInt(5, utilisateur.getIdUtilisateur());

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Utilisateur non trouve pour mise a jour");
            }
            logger.info("Utilisateur mis a jour: {}", utilisateur.getIdUtilisateur());

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour de l'utilisateur", e);
            throw new DAOException("Erreur lors de la mise a jour de l'utilisateur", e);
        }
    }

    @Override
    public void delete(Integer id) throws DAOException {
        logger.debug("Suppression de l'utilisateur ID: {}", id);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Utilisateur non trouve pour suppression");
            }
            logger.info("Utilisateur supprime: {}", id);

        } catch (SQLException e) {
            logger.error("Erreur lors de la suppression de l'utilisateur", e);
            throw new DAOException("Erreur lors de la suppression de l'utilisateur", e);
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
            throw new DAOException("Erreur lors du comptage des utilisateurs", e);
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
    public Optional<Utilisateur> findByNomUtilisateur(String nomUtilisateur) throws DAOException {
        logger.debug("Recherche utilisateur par nom: {}", nomUtilisateur);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NOM_UTILISATEUR)) {

            ps.setString(1, nomUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUtilisateur(rs));
                }
            }
            return Optional.empty();

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par nom d'utilisateur", e);
            throw new DAOException("Erreur lors de la recherche par nom d'utilisateur", e);
        }
    }

    @Override
    public List<Utilisateur> findByRole(Role role) throws DAOException {
        logger.debug("Recherche utilisateurs par role: {}", role);
        List<Utilisateur> utilisateurs = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ROLE)) {

            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    utilisateurs.add(mapResultSetToUtilisateur(rs));
                }
            }
            return utilisateurs;

        } catch (SQLException e) {
            logger.error("Erreur lors de la recherche par role", e);
            throw new DAOException("Erreur lors de la recherche par role", e);
        }
    }

    @Override
    public boolean existsByNomUtilisateur(String nomUtilisateur) throws DAOException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_NOM_UTILISATEUR)) {

            ps.setString(1, nomUtilisateur);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;

        } catch (SQLException e) {
            throw new DAOException("Erreur lors de la verification du nom d'utilisateur", e);
        }
    }

    @Override
    public void updateDerniereConnexion(int idUtilisateur) throws DAOException {
        logger.debug("Mise a jour derniere connexion: {}", idUtilisateur);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_DERNIERE_CONNEXION)) {

            ps.setInt(1, idUtilisateur);
            ps.executeUpdate();

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour de la derniere connexion", e);
            throw new DAOException("Erreur lors de la mise a jour de la derniere connexion", e);
        }
    }

    @Override
    public void updateMotDePasse(int idUtilisateur, String nouveauMotDePasse) throws DAOException {
        logger.debug("Mise a jour mot de passe: {}", idUtilisateur);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_MOT_DE_PASSE)) {

            ps.setString(1, nouveauMotDePasse);
            ps.setInt(2, idUtilisateur);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Utilisateur non trouve pour mise a jour du mot de passe");
            }
            logger.info("Mot de passe mis a jour pour utilisateur: {}", idUtilisateur);

        } catch (SQLException e) {
            logger.error("Erreur lors de la mise a jour du mot de passe", e);
            throw new DAOException("Erreur lors de la mise a jour du mot de passe", e);
        }
    }

    @Override
    public void setActif(int idUtilisateur, boolean actif) throws DAOException {
        logger.debug("Changement statut actif utilisateur {}: {}", idUtilisateur, actif);

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SET_ACTIF)) {

            ps.setBoolean(1, actif);
            ps.setInt(2, idUtilisateur);

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new DAOException("Utilisateur non trouve");
            }

        } catch (SQLException e) {
            logger.error("Erreur lors du changement de statut", e);
            throw new DAOException("Erreur lors du changement de statut", e);
        }
    }

    private Utilisateur mapResultSetToUtilisateur(ResultSet rs) throws SQLException {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setIdUtilisateur(rs.getInt("id_utilisateur"));
        utilisateur.setNomUtilisateur(rs.getString("nom_utilisateur"));
        utilisateur.setMotDePasse(rs.getString("mot_de_passe"));
        utilisateur.setRole(Role.valueOf(rs.getString("role")));
        utilisateur.setNomComplet(rs.getString("nom_complet"));
        utilisateur.setActif(rs.getBoolean("actif"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            utilisateur.setDateCreation(dateCreation.toLocalDateTime());
        }

        Timestamp derniereConnexion = rs.getTimestamp("derniere_connexion");
        if (derniereConnexion != null) {
            utilisateur.setDerniereConnexion(derniereConnexion.toLocalDateTime());
        }

        return utilisateur;
    }
}
