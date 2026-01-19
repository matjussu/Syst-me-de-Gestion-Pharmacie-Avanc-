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

import java.time.LocalDateTime;

/**
 * Service d'authentification et de gestion des sessions.
 * <p>
 * Gere la connexion des utilisateurs, la verification des mots de passe
 * avec BCrypt et le controle des permissions selon les roles.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UtilisateurDAO utilisateurDAO;
    private final AuditService auditService;

    /** Utilisateur actuellement connecte (session) */
    private Utilisateur utilisateurConnecte;

    /**
     * Constructeur avec injection du DAO.
     */
    public AuthenticationService() {
        this.utilisateurDAO = new UtilisateurDAOImpl();
        this.auditService = new AuditService();
    }

    /**
     * Constructeur avec injection du DAO (pour tests).
     *
     * @param utilisateurDAO le DAO a utiliser
     */
    public AuthenticationService(UtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
        this.auditService = new AuditService();
    }

    /**
     * Authentifie un utilisateur avec son nom d'utilisateur et mot de passe.
     *
     * @param nomUtilisateur le nom d'utilisateur
     * @param motDePasse     le mot de passe en clair
     * @return l'utilisateur authentifie
     * @throws ServiceException si l'authentification echoue
     */
    public Utilisateur authenticate(String nomUtilisateur, String motDePasse) throws ServiceException {
        logger.info("Tentative de connexion pour: {}", nomUtilisateur);

        if (nomUtilisateur == null || nomUtilisateur.trim().isEmpty()) {
            throw new ServiceException("Le nom d'utilisateur est requis", ErrorType.VALIDATION);
        }
        if (motDePasse == null || motDePasse.isEmpty()) {
            throw new ServiceException("Le mot de passe est requis", ErrorType.VALIDATION);
        }

        try {
            Utilisateur utilisateur = utilisateurDAO.findByNomUtilisateur(nomUtilisateur)
                    .orElse(null);

            if (utilisateur == null) {
                logger.warn("Utilisateur non trouve: {}", nomUtilisateur);
                throw new ServiceException("Identifiants incorrects", ErrorType.NON_AUTORISE);
            }

            if (!utilisateur.isActif()) {
                logger.warn("Compte desactive: {}", nomUtilisateur);
                throw new ServiceException("Ce compte est desactive", ErrorType.NON_AUTORISE);
            }

            if (!PasswordUtils.verifyPassword(motDePasse, utilisateur.getMotDePasse())) {
                logger.warn("Mot de passe incorrect pour: {}", nomUtilisateur);
                throw new ServiceException("Identifiants incorrects", ErrorType.NON_AUTORISE);
            }

            // Mise a jour de la derniere connexion
            utilisateur.setDerniereConnexion(LocalDateTime.now());
            utilisateurDAO.updateDerniereConnexion(utilisateur.getIdUtilisateur());

            // Enregistrer la session
            this.utilisateurConnecte = utilisateur;

            // Enregistrer dans l'audit
            AuditService.setCurrentUser(utilisateur);
            auditService.logConnexion(utilisateur);

            logger.info("Connexion reussie pour: {} (Role: {})", nomUtilisateur, utilisateur.getRole());
            return utilisateur;

        } catch (DAOException e) {
            logger.error("Erreur lors de l'authentification", e);
            throw new ServiceException("Erreur lors de la connexion", e);
        }
    }

    /**
     * Deconnecte l'utilisateur actuel.
     */
    public void logout() {
        if (utilisateurConnecte != null) {
            logger.info("Deconnexion de: {}", utilisateurConnecte.getNomUtilisateur());
            auditService.logDeconnexion(utilisateurConnecte);
            AuditService.setCurrentUser(null);
            utilisateurConnecte = null;
        }
    }

    /**
     * Retourne l'utilisateur actuellement connecte.
     *
     * @return l'utilisateur connecte ou null
     */
    public Utilisateur getUtilisateurConnecte() {
        return utilisateurConnecte;
    }

    /**
     * Verifie si un utilisateur est connecte.
     *
     * @return true si un utilisateur est connecte
     */
    public boolean isConnecte() {
        return utilisateurConnecte != null;
    }

    /**
     * Verifie si l'utilisateur connecte a un role specifique.
     *
     * @param role le role requis
     * @return true si l'utilisateur a ce role
     */
    public boolean hasRole(Role role) {
        return utilisateurConnecte != null && utilisateurConnecte.getRole() == role;
    }

    /**
     * Verifie si l'utilisateur connecte est pharmacien.
     *
     * @return true si l'utilisateur est pharmacien
     */
    public boolean isPharmacien() {
        return hasRole(Role.PHARMACIEN);
    }

    /**
     * Verifie si l'utilisateur connecte est preparateur.
     *
     * @return true si l'utilisateur est preparateur
     */
    public boolean isPreparateur() {
        return hasRole(Role.PREPARATEUR);
    }

    /**
     * Verifie si l'utilisateur a la permission d'effectuer une action.
     * <p>
     * Permissions par role :
     * <ul>
     *   <li>PHARMACIEN : Toutes les permissions</li>
     *   <li>PREPARATEUR : Ventes uniquement</li>
     * </ul>
     * </p>
     *
     * @param permission la permission a verifier
     * @return true si l'utilisateur a la permission
     */
    public boolean hasPermission(String permission) {
        if (utilisateurConnecte == null) {
            return false;
        }

        // Pharmacien a toutes les permissions
        if (utilisateurConnecte.getRole() == Role.PHARMACIEN) {
            return true;
        }

        // Preparateur : permissions limitees
        if (utilisateurConnecte.getRole() == Role.PREPARATEUR) {
            return permission.equals("VENTE") ||
                   permission.equals("CONSULTER_STOCK") ||
                   permission.equals("CONSULTER_MEDICAMENTS");
        }

        return false;
    }

    /**
     * Verifie la permission et lance une exception si non autorise.
     *
     * @param permission la permission requise
     * @throws ServiceException si l'utilisateur n'a pas la permission
     */
    public void requirePermission(String permission) throws ServiceException {
        if (!isConnecte()) {
            throw new ServiceException("Vous devez etre connecte", ErrorType.NON_AUTORISE);
        }
        if (!hasPermission(permission)) {
            throw new ServiceException("Vous n'avez pas la permission: " + permission, ErrorType.NON_AUTORISE);
        }
    }

    /**
     * Change le mot de passe d'un utilisateur.
     *
     * @param idUtilisateur       l'ID de l'utilisateur
     * @param ancienMotDePasse    l'ancien mot de passe
     * @param nouveauMotDePasse   le nouveau mot de passe
     * @throws ServiceException si le changement echoue
     */
    public void changerMotDePasse(int idUtilisateur, String ancienMotDePasse, String nouveauMotDePasse)
            throws ServiceException {
        try {
            Utilisateur utilisateur = utilisateurDAO.findById(idUtilisateur)
                    .orElseThrow(() -> new ServiceException("Utilisateur non trouve", ErrorType.NOT_FOUND));

            if (!PasswordUtils.verifyPassword(ancienMotDePasse, utilisateur.getMotDePasse())) {
                throw new ServiceException("Ancien mot de passe incorrect", ErrorType.VALIDATION);
            }

            PasswordUtils.validatePassword(nouveauMotDePasse);
            String nouveauHash = PasswordUtils.hashPassword(nouveauMotDePasse);
            utilisateurDAO.updateMotDePasse(idUtilisateur, nouveauHash);

            logger.info("Mot de passe change pour l'utilisateur ID: {}", idUtilisateur);

        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du changement de mot de passe", e);
        }
    }
}
