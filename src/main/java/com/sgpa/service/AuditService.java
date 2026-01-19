package com.sgpa.service;

import com.sgpa.dao.AuditLogDAO;
import com.sgpa.dao.impl.AuditLogDAOImpl;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.model.AuditLog;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.TypeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

/**
 * Service de gestion du journal d'audit.
 * <p>
 * Ce service permet d'enregistrer et de consulter les actions
 * effectuees dans le systeme pour assurer la tracabilite.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogDAO auditLogDAO;

    /** Utilisateur courant pour l'enregistrement automatique */
    private static Utilisateur currentUser;

    /**
     * Constructeur par defaut.
     */
    public AuditService() {
        this.auditLogDAO = new AuditLogDAOImpl();
    }

    /**
     * Constructeur avec injection du DAO (pour tests).
     *
     * @param auditLogDAO le DAO audit
     */
    public AuditService(AuditLogDAO auditLogDAO) {
        this.auditLogDAO = auditLogDAO;
    }

    /**
     * Definit l'utilisateur courant pour l'audit automatique.
     *
     * @param user l'utilisateur connecte
     */
    public static void setCurrentUser(Utilisateur user) {
        currentUser = user;
    }

    /**
     * Retourne l'utilisateur courant.
     *
     * @return l'utilisateur courant
     */
    public static Utilisateur getCurrentUser() {
        return currentUser;
    }

    // =====================================================
    // METHODES D'ENREGISTREMENT
    // =====================================================

    /**
     * Enregistre une action dans le journal d'audit.
     *
     * @param typeAction  le type d'action
     * @param entite      le type d'entite concernee
     * @param idEntite    l'ID de l'entite (peut etre null)
     * @param description la description de l'action
     */
    public void log(TypeAction typeAction, String entite, Integer idEntite, String description) {
        try {
            AuditLog log = new AuditLog();
            log.setTypeAction(typeAction);
            log.setEntite(entite);
            log.setIdEntite(idEntite);
            log.setDescription(description);

            if (currentUser != null) {
                log.setIdUtilisateur(currentUser.getIdUtilisateur());
                log.setNomUtilisateur(currentUser.getNomComplet());
            }

            auditLogDAO.save(log);
            logger.debug("Audit: {} - {} - {}", typeAction, entite, description);

        } catch (DAOException e) {
            // Ne pas faire echouer l'operation principale si l'audit echoue
            logger.error("Erreur lors de l'enregistrement de l'audit", e);
        }
    }

    /**
     * Enregistre une action simple dans le journal.
     *
     * @param typeAction  le type d'action
     * @param description la description de l'action
     */
    public void log(TypeAction typeAction, String description) {
        log(typeAction, null, null, description);
    }

    /**
     * Enregistre une connexion utilisateur.
     *
     * @param utilisateur l'utilisateur qui se connecte
     */
    public void logConnexion(Utilisateur utilisateur) {
        try {
            AuditLog log = new AuditLog(
                    utilisateur.getIdUtilisateur(),
                    utilisateur.getNomComplet(),
                    TypeAction.CONNEXION,
                    "UTILISATEUR",
                    "Connexion au systeme"
            );
            log.setIdEntite(utilisateur.getIdUtilisateur());
            auditLogDAO.save(log);
            logger.info("Connexion enregistree: {}", utilisateur.getNomUtilisateur());

        } catch (DAOException e) {
            logger.error("Erreur lors de l'enregistrement de la connexion", e);
        }
    }

    /**
     * Enregistre une deconnexion utilisateur.
     *
     * @param utilisateur l'utilisateur qui se deconnecte
     */
    public void logDeconnexion(Utilisateur utilisateur) {
        if (utilisateur == null) return;

        try {
            AuditLog log = new AuditLog(
                    utilisateur.getIdUtilisateur(),
                    utilisateur.getNomComplet(),
                    TypeAction.DECONNEXION,
                    "UTILISATEUR",
                    "Deconnexion du systeme"
            );
            log.setIdEntite(utilisateur.getIdUtilisateur());
            auditLogDAO.save(log);
            logger.info("Deconnexion enregistree: {}", utilisateur.getNomUtilisateur());

        } catch (DAOException e) {
            logger.error("Erreur lors de l'enregistrement de la deconnexion", e);
        }
    }

    /**
     * Enregistre une creation d'entite.
     *
     * @param entite   le type d'entite
     * @param idEntite l'ID de l'entite creee
     * @param details  les details (ex: nom de l'entite)
     */
    public void logCreation(String entite, Integer idEntite, String details) {
        log(TypeAction.CREATION, entite, idEntite, "Creation: " + details);
    }

    /**
     * Enregistre une modification d'entite.
     *
     * @param entite   le type d'entite
     * @param idEntite l'ID de l'entite modifiee
     * @param details  les details des modifications
     */
    public void logModification(String entite, Integer idEntite, String details) {
        log(TypeAction.MODIFICATION, entite, idEntite, "Modification: " + details);
    }

    /**
     * Enregistre une suppression d'entite.
     *
     * @param entite   le type d'entite
     * @param idEntite l'ID de l'entite supprimee
     * @param details  les details
     */
    public void logSuppression(String entite, Integer idEntite, String details) {
        log(TypeAction.SUPPRESSION, entite, idEntite, "Suppression: " + details);
    }

    /**
     * Enregistre une vente.
     *
     * @param idVente     l'ID de la vente
     * @param montant     le montant total
     * @param nbArticles  le nombre d'articles
     */
    public void logVente(Integer idVente, String montant, int nbArticles) {
        log(TypeAction.VENTE, "VENTE", idVente,
                String.format("Vente #%d - Montant: %s - %d article(s)", idVente, montant, nbArticles));
    }

    /**
     * Enregistre une commande fournisseur.
     *
     * @param idCommande    l'ID de la commande
     * @param fournisseur   le nom du fournisseur
     */
    public void logCommande(Integer idCommande, String fournisseur) {
        log(TypeAction.COMMANDE, "COMMANDE", idCommande,
                String.format("Commande #%d creee - Fournisseur: %s", idCommande, fournisseur));
    }

    /**
     * Enregistre une reception de commande.
     *
     * @param idCommande l'ID de la commande recue
     */
    public void logReception(Integer idCommande) {
        log(TypeAction.RECEPTION, "COMMANDE", idCommande,
                String.format("Commande #%d recue", idCommande));
    }

    // =====================================================
    // METHODES DE CONSULTATION
    // =====================================================

    /**
     * Recupere les dernieres entrees du journal.
     *
     * @param limit le nombre maximum d'entrees
     * @return la liste des entrees
     * @throws ServiceException si une erreur survient
     */
    public List<AuditLog> getRecentLogs(int limit) throws ServiceException {
        try {
            return auditLogDAO.findAllPaginated(limit, 0);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des logs", e);
        }
    }

    /**
     * Recupere les entrees du journal pour une periode.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des entrees
     * @throws ServiceException si une erreur survient
     */
    public List<AuditLog> getLogsByPeriode(LocalDate dateDebut, LocalDate dateFin) throws ServiceException {
        try {
            return auditLogDAO.findByDateRange(dateDebut, dateFin);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des logs", e);
        }
    }

    /**
     * Recupere les entrees du journal pour un utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @return la liste des entrees
     * @throws ServiceException si une erreur survient
     */
    public List<AuditLog> getLogsByUtilisateur(int idUtilisateur) throws ServiceException {
        try {
            return auditLogDAO.findByUtilisateur(idUtilisateur);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des logs", e);
        }
    }

    /**
     * Recupere les entrees du journal par type d'action.
     *
     * @param typeAction le type d'action
     * @return la liste des entrees
     * @throws ServiceException si une erreur survient
     */
    public List<AuditLog> getLogsByType(TypeAction typeAction) throws ServiceException {
        try {
            return auditLogDAO.findByTypeAction(typeAction);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des logs", e);
        }
    }

    /**
     * Recherche les entrees du journal selon plusieurs criteres.
     *
     * @param dateDebut     la date de debut (peut etre null)
     * @param dateFin       la date de fin (peut etre null)
     * @param typeAction    le type d'action (peut etre null)
     * @param entite        le type d'entite (peut etre null)
     * @param idUtilisateur l'ID de l'utilisateur (peut etre null)
     * @param limit         le nombre maximum d'entrees
     * @return la liste des entrees
     * @throws ServiceException si une erreur survient
     */
    public List<AuditLog> search(LocalDate dateDebut, LocalDate dateFin, TypeAction typeAction,
                                  String entite, Integer idUtilisateur, int limit) throws ServiceException {
        try {
            return auditLogDAO.search(dateDebut, dateFin, typeAction, entite, idUtilisateur, limit);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche", e);
        }
    }

    /**
     * Compte le nombre total d'entrees dans le journal.
     *
     * @return le nombre d'entrees
     * @throws ServiceException si une erreur survient
     */
    public long countLogs() throws ServiceException {
        try {
            return auditLogDAO.count();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du comptage", e);
        }
    }

    /**
     * Purge les anciennes entrees du journal.
     *
     * @param joursRetention nombre de jours de retention
     * @return le nombre d'entrees supprimees
     * @throws ServiceException si une erreur survient
     */
    public int purgerAnciennesEntrees(int joursRetention) throws ServiceException {
        try {
            LocalDate dateLimit = LocalDate.now().minusDays(joursRetention);
            return auditLogDAO.deleteOlderThan(dateLimit);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la purge", e);
        }
    }
}
