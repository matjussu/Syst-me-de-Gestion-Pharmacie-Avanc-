package com.sgpa.service;

import com.sgpa.dao.UtilisateurDAO;
import com.sgpa.dao.impl.UtilisateurDAOImpl;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.exception.ServiceException.ErrorType;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.Role;
import com.sgpa.utils.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service de gestion des utilisateurs.
 * <p>
 * Ce service fournit les operations CRUD pour les utilisateurs
 * ainsi que les fonctionnalites de changement de mot de passe
 * et d'activation/desactivation des comptes.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class UtilisateurService {

    private static final Logger logger = LoggerFactory.getLogger(UtilisateurService.class);

    private final UtilisateurDAO utilisateurDAO;

    /**
     * Constructeur par defaut.
     */
    public UtilisateurService() {
        this.utilisateurDAO = new UtilisateurDAOImpl();
    }

    /**
     * Constructeur avec injection du DAO (pour tests).
     *
     * @param utilisateurDAO le DAO utilisateur
     */
    public UtilisateurService(UtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
    }

    /**
     * Recupere tous les utilisateurs.
     *
     * @return la liste de tous les utilisateurs
     * @throws ServiceException si une erreur survient
     */
    public List<Utilisateur> getAllUtilisateurs() throws ServiceException {
        try {
            return utilisateurDAO.findAll();
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des utilisateurs", e);
            throw new ServiceException("Erreur lors de la recuperation des utilisateurs", e);
        }
    }

    /**
     * Recupere les utilisateurs actifs.
     *
     * @return la liste des utilisateurs actifs
     * @throws ServiceException si une erreur survient
     */
    public List<Utilisateur> getUtilisateursActifs() throws ServiceException {
        try {
            return utilisateurDAO.findAllActive();
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des utilisateurs actifs", e);
            throw new ServiceException("Erreur lors de la recuperation des utilisateurs actifs", e);
        }
    }

    /**
     * Recupere les utilisateurs par role.
     *
     * @param role le role recherche
     * @return la liste des utilisateurs ayant ce role
     * @throws ServiceException si une erreur survient
     */
    public List<Utilisateur> getUtilisateursByRole(Role role) throws ServiceException {
        try {
            return utilisateurDAO.findByRole(role);
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des utilisateurs par role", e);
            throw new ServiceException("Erreur lors de la recuperation des utilisateurs", e);
        }
    }

    /**
     * Recupere un utilisateur par son ID.
     *
     * @param id l'ID de l'utilisateur
     * @return l'utilisateur
     * @throws ServiceException si l'utilisateur n'existe pas
     */
    public Utilisateur getUtilisateurById(int id) throws ServiceException {
        try {
            return utilisateurDAO.findById(id)
                    .orElseThrow(() -> new ServiceException(
                            "Utilisateur non trouve: " + id,
                            ErrorType.NOT_FOUND));
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation de l'utilisateur", e);
            throw new ServiceException("Erreur lors de la recuperation de l'utilisateur", e);
        }
    }

    /**
     * Cree un nouvel utilisateur.
     *
     * @param nomUtilisateur le nom d'utilisateur
     * @param motDePasse     le mot de passe (non hashe)
     * @param role           le role
     * @param nomComplet     le nom complet
     * @return l'utilisateur cree
     * @throws ServiceException si une erreur survient ou si le nom existe deja
     */
    public Utilisateur creerUtilisateur(String nomUtilisateur, String motDePasse,
                                        Role role, String nomComplet) throws ServiceException {
        // Validation
        if (nomUtilisateur == null || nomUtilisateur.trim().isEmpty()) {
            throw new ServiceException("Le nom d'utilisateur est obligatoire", ErrorType.VALIDATION);
        }
        if (motDePasse == null || motDePasse.length() < 4) {
            throw new ServiceException("Le mot de passe doit contenir au moins 4 caracteres", ErrorType.VALIDATION);
        }
        if (role == null) {
            throw new ServiceException("Le role est obligatoire", ErrorType.VALIDATION);
        }
        if (nomComplet == null || nomComplet.trim().isEmpty()) {
            throw new ServiceException("Le nom complet est obligatoire", ErrorType.VALIDATION);
        }

        try {
            // Verifier unicite du nom d'utilisateur
            if (utilisateurDAO.existsByNomUtilisateur(nomUtilisateur.trim())) {
                throw new ServiceException(
                        "Ce nom d'utilisateur existe deja: " + nomUtilisateur,
                        ErrorType.VALIDATION);
            }

            // Hasher le mot de passe
            String motDePasseHash = PasswordUtils.hashPassword(motDePasse);

            // Creer l'utilisateur
            Utilisateur utilisateur = new Utilisateur(
                    nomUtilisateur.trim(),
                    motDePasseHash,
                    role,
                    nomComplet.trim()
            );

            utilisateur = utilisateurDAO.save(utilisateur);
            logger.info("Utilisateur cree: {} ({})", nomUtilisateur, role);
            return utilisateur;

        } catch (DAOException e) {
            logger.error("Erreur lors de la creation de l'utilisateur", e);
            throw new ServiceException("Erreur lors de la creation de l'utilisateur", e);
        }
    }

    /**
     * Met a jour un utilisateur existant.
     * <p>
     * Note: Cette methode ne modifie pas le mot de passe.
     * Utiliser {@link #changerMotDePasse} pour cela.
     * </p>
     *
     * @param utilisateur l'utilisateur a mettre a jour
     * @throws ServiceException si une erreur survient
     */
    public void mettreAJourUtilisateur(Utilisateur utilisateur) throws ServiceException {
        if (utilisateur == null || utilisateur.getIdUtilisateur() == null) {
            throw new ServiceException("Utilisateur invalide", ErrorType.VALIDATION);
        }

        try {
            // Verifier que l'utilisateur existe
            if (!utilisateurDAO.existsById(utilisateur.getIdUtilisateur())) {
                throw new ServiceException(
                        "Utilisateur non trouve: " + utilisateur.getIdUtilisateur(),
                        ErrorType.NOT_FOUND);
            }

            // Verifier unicite du nom si modifie
            Utilisateur existant = utilisateurDAO.findById(utilisateur.getIdUtilisateur()).orElse(null);
            if (existant != null && !existant.getNomUtilisateur().equals(utilisateur.getNomUtilisateur())) {
                if (utilisateurDAO.existsByNomUtilisateur(utilisateur.getNomUtilisateur())) {
                    throw new ServiceException(
                            "Ce nom d'utilisateur existe deja",
                            ErrorType.VALIDATION);
                }
            }

            utilisateurDAO.update(utilisateur);
            logger.info("Utilisateur mis a jour: {}", utilisateur.getIdUtilisateur());

        } catch (DAOException e) {
            logger.error("Erreur lors de la mise a jour de l'utilisateur", e);
            throw new ServiceException("Erreur lors de la mise a jour de l'utilisateur", e);
        }
    }

    /**
     * Change le mot de passe d'un utilisateur.
     *
     * @param idUtilisateur    l'ID de l'utilisateur
     * @param nouveauMotDePasse le nouveau mot de passe (non hashe)
     * @throws ServiceException si une erreur survient
     */
    public void changerMotDePasse(int idUtilisateur, String nouveauMotDePasse) throws ServiceException {
        if (nouveauMotDePasse == null || nouveauMotDePasse.length() < 4) {
            throw new ServiceException(
                    "Le nouveau mot de passe doit contenir au moins 4 caracteres",
                    ErrorType.VALIDATION);
        }

        try {
            String motDePasseHash = PasswordUtils.hashPassword(nouveauMotDePasse);
            utilisateurDAO.updateMotDePasse(idUtilisateur, motDePasseHash);
            logger.info("Mot de passe change pour utilisateur: {}", idUtilisateur);

        } catch (DAOException e) {
            logger.error("Erreur lors du changement de mot de passe", e);
            throw new ServiceException("Erreur lors du changement de mot de passe", e);
        }
    }

    /**
     * Active ou desactive un utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @param actif         true pour activer, false pour desactiver
     * @throws ServiceException si une erreur survient
     */
    public void setActif(int idUtilisateur, boolean actif) throws ServiceException {
        try {
            utilisateurDAO.setActif(idUtilisateur, actif);
            logger.info("Utilisateur {} {}", idUtilisateur, actif ? "active" : "desactive");

        } catch (DAOException e) {
            logger.error("Erreur lors du changement de statut", e);
            throw new ServiceException("Erreur lors du changement de statut", e);
        }
    }

    /**
     * Supprime un utilisateur.
     * <p>
     * Attention: La suppression peut echouer si l'utilisateur
     * est reference dans des ventes.
     * </p>
     *
     * @param idUtilisateur l'ID de l'utilisateur a supprimer
     * @throws ServiceException si une erreur survient
     */
    public void supprimerUtilisateur(int idUtilisateur) throws ServiceException {
        try {
            utilisateurDAO.delete(idUtilisateur);
            logger.info("Utilisateur supprime: {}", idUtilisateur);

        } catch (DAOException e) {
            logger.error("Erreur lors de la suppression de l'utilisateur", e);
            throw new ServiceException(
                    "Impossible de supprimer cet utilisateur. Il est peut-etre reference dans des ventes.",
                    e);
        }
    }

    /**
     * Compte le nombre total d'utilisateurs.
     *
     * @return le nombre d'utilisateurs
     * @throws ServiceException si une erreur survient
     */
    public long compterUtilisateurs() throws ServiceException {
        try {
            return utilisateurDAO.count();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du comptage des utilisateurs", e);
        }
    }
}
