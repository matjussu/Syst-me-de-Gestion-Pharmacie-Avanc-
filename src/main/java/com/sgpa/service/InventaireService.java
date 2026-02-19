package com.sgpa.service;

import com.sgpa.dao.*;
import com.sgpa.dao.impl.*;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.exception.ServiceException.ErrorType;
import com.sgpa.model.*;
import com.sgpa.model.enums.MotifEcart;
import com.sgpa.model.enums.StatutInventaire;
import com.sgpa.model.enums.TypeAction;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des inventaires.
 * <p>
 * Ce service gere les sessions d'inventaire, les comptages physiques
 * et les regularisations de stock.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class InventaireService {

    private static final Logger logger = LoggerFactory.getLogger(InventaireService.class);

    private final SessionInventaireDAO sessionDAO;
    private final ComptageInventaireDAO comptageDAO;
    private final RegularisationDAO regularisationDAO;
    private final LotDAO lotDAO;
    private final AuditService auditService;

    /**
     * Constructeur par defaut.
     */
    public InventaireService() {
        this.sessionDAO = new SessionInventaireDAOImpl();
        this.comptageDAO = new ComptageInventaireDAOImpl();
        this.regularisationDAO = new RegularisationDAOImpl();
        this.lotDAO = new LotDAOImpl();
        this.auditService = new AuditService();
    }

    /**
     * Constructeur avec injection des DAOs (pour tests).
     */
    public InventaireService(SessionInventaireDAO sessionDAO, ComptageInventaireDAO comptageDAO,
                              RegularisationDAO regularisationDAO, LotDAO lotDAO, AuditService auditService) {
        this.sessionDAO = sessionDAO;
        this.comptageDAO = comptageDAO;
        this.regularisationDAO = regularisationDAO;
        this.lotDAO = lotDAO;
        this.auditService = auditService;
    }

    // ==================== GESTION DES SESSIONS ====================

    /**
     * Cree une nouvelle session d'inventaire.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @param notes         les notes de la session
     * @return la session creee
     * @throws ServiceException si une session est deja en cours
     */
    public SessionInventaire creerSession(int idUtilisateur, String notes) throws ServiceException {
        logger.info("Creation d'une session d'inventaire par utilisateur {}", idUtilisateur);

        try {
            // Verifier qu'il n'y a pas de session en cours
            if (sessionDAO.hasSessionEnCours()) {
                throw new ServiceException("Une session d'inventaire est deja en cours", ErrorType.VALIDATION);
            }

            SessionInventaire session = new SessionInventaire(idUtilisateur, notes);
            session = sessionDAO.save(session);

            // Audit
            auditService.log(TypeAction.CREATION, "SessionInventaire", session.getIdSession(),
                    "Nouvelle session d'inventaire creee");

            logger.info("Session d'inventaire {} creee", session.getIdSession());
            return session;

        } catch (DAOException e) {
            logger.error("Erreur lors de la creation de la session", e);
            throw new ServiceException("Erreur lors de la creation de la session", e);
        }
    }

    /**
     * Recupere une session par son ID.
     *
     * @param idSession l'ID de la session
     * @return la session
     * @throws ServiceException si la session n'existe pas
     */
    public SessionInventaire getSessionById(int idSession) throws ServiceException {
        try {
            SessionInventaire session = sessionDAO.findById(idSession)
                    .orElseThrow(() -> new ServiceException(
                            "Session non trouvee: " + idSession, ErrorType.NOT_FOUND));

            // Charger les comptages
            List<ComptageInventaire> comptages = comptageDAO.findBySession(idSession);
            session.setComptages(comptages);

            return session;

        } catch (DAOException e) {
            logger.error("Erreur lors de la recherche de la session", e);
            throw new ServiceException("Erreur lors de la recherche de la session", e);
        }
    }

    /**
     * Recupere toutes les sessions.
     *
     * @return la liste des sessions
     * @throws ServiceException si une erreur survient
     */
    public List<SessionInventaire> getAllSessions() throws ServiceException {
        try {
            return sessionDAO.findAll();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des sessions", e);
        }
    }

    /**
     * Recupere les sessions par statut.
     *
     * @param statut le statut recherche
     * @return la liste des sessions
     * @throws ServiceException si une erreur survient
     */
    public List<SessionInventaire> getSessionsByStatut(StatutInventaire statut) throws ServiceException {
        try {
            return sessionDAO.findByStatut(statut);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche par statut", e);
        }
    }

    /**
     * Recupere la session en cours s'il y en a une.
     *
     * @return la session en cours ou Optional.empty()
     * @throws ServiceException si une erreur survient
     */
    public Optional<SessionInventaire> getSessionEnCours() throws ServiceException {
        try {
            List<SessionInventaire> sessions = sessionDAO.findByStatut(StatutInventaire.EN_COURS);
            if (!sessions.isEmpty()) {
                SessionInventaire session = sessions.get(0);
                // Charger les comptages
                session.setComptages(comptageDAO.findBySession(session.getIdSession()));
                return Optional.of(session);
            }
            return Optional.empty();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche de session en cours", e);
        }
    }

    /**
     * Termine une session d'inventaire.
     *
     * @param idSession l'ID de la session
     * @throws ServiceException si la session n'est pas en cours
     */
    public void terminerSession(int idSession) throws ServiceException {
        logger.info("Terminaison de la session {}", idSession);

        try {
            SessionInventaire session = getSessionById(idSession);

            if (!session.isEnCours()) {
                throw new ServiceException("La session n'est pas en cours", ErrorType.VALIDATION);
            }

            sessionDAO.terminerSession(idSession);

            // Audit
            int nbComptages = comptageDAO.countBySession(idSession);
            int nbEcarts = comptageDAO.countEcarts(idSession);
            auditService.log(TypeAction.MODIFICATION, "SessionInventaire", idSession,
                    String.format("Session terminee: %d comptages, %d ecarts", nbComptages, nbEcarts));

            logger.info("Session {} terminee", idSession);

        } catch (DAOException e) {
            logger.error("Erreur lors de la terminaison de la session", e);
            throw new ServiceException("Erreur lors de la terminaison de la session", e);
        }
    }

    /**
     * Annule une session d'inventaire.
     *
     * @param idSession l'ID de la session
     * @throws ServiceException si la session n'est pas en cours
     */
    public void annulerSession(int idSession) throws ServiceException {
        logger.info("Annulation de la session {}", idSession);

        try {
            SessionInventaire session = getSessionById(idSession);

            if (!session.isEnCours()) {
                throw new ServiceException("La session n'est pas en cours", ErrorType.VALIDATION);
            }

            sessionDAO.annulerSession(idSession);

            // Audit
            auditService.log(TypeAction.MODIFICATION, "SessionInventaire", idSession, "Session annulee");

            logger.info("Session {} annulee", idSession);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'annulation de la session", e);
            throw new ServiceException("Erreur lors de l'annulation de la session", e);
        }
    }

    // ==================== GESTION DES COMPTAGES ====================

    /**
     * Enregistre un comptage physique.
     *
     * @param idSession        l'ID de la session
     * @param idLot            l'ID du lot
     * @param quantitePhysique la quantite physique comptee
     * @param motif            le motif de l'ecart (si applicable)
     * @param commentaire      commentaire optionnel
     * @param idUtilisateur    l'ID de l'utilisateur
     * @return le comptage enregistre
     * @throws ServiceException si une erreur survient
     */
    public ComptageInventaire enregistrerComptage(int idSession, int idLot, int quantitePhysique,
                                                   MotifEcart motif, String commentaire, int idUtilisateur)
            throws ServiceException {

        logger.info("Enregistrement comptage: session={}, lot={}, qtePhysique={}",
                idSession, idLot, quantitePhysique);

        try {
            // Verifier que la session existe et est en cours
            SessionInventaire session = getSessionById(idSession);
            if (!session.isEnCours()) {
                throw new ServiceException("La session n'est pas en cours", ErrorType.VALIDATION);
            }

            // Verifier que le lot existe
            Lot lot = lotDAO.findById(idLot)
                    .orElseThrow(() -> new ServiceException("Lot non trouve: " + idLot, ErrorType.NOT_FOUND));

            // Verifier si un comptage existe deja pour ce lot dans cette session
            Optional<ComptageInventaire> existant = comptageDAO.findBySessionAndLot(idSession, idLot);

            ComptageInventaire comptage;
            if (existant.isPresent()) {
                // Mise a jour du comptage existant
                comptage = existant.get();
                comptage.setQuantitePhysique(quantitePhysique);
                comptage.setEcart(quantitePhysique - comptage.getQuantiteTheorique());
                comptage.setMotifEcart(motif);
                comptage.setCommentaire(commentaire);
                comptageDAO.update(comptage);
                logger.debug("Comptage mis a jour: {}", comptage.getIdComptage());
            } else {
                // Nouveau comptage
                int quantiteTheorique = lot.getQuantiteStock();
                comptage = new ComptageInventaire(idSession, idLot, quantiteTheorique,
                        quantitePhysique, idUtilisateur);
                comptage.setMotifEcart(motif);
                comptage.setCommentaire(commentaire);
                comptage.setLot(lot);
                comptage = comptageDAO.save(comptage);
                logger.debug("Nouveau comptage cree: {}", comptage.getIdComptage());
            }

            return comptage;

        } catch (DAOException e) {
            logger.error("Erreur lors de l'enregistrement du comptage", e);
            throw new ServiceException("Erreur lors de l'enregistrement du comptage", e);
        }
    }

    /**
     * Recupere les comptages d'une session.
     *
     * @param idSession l'ID de la session
     * @return la liste des comptages
     * @throws ServiceException si une erreur survient
     */
    public List<ComptageInventaire> getComptagesBySession(int idSession) throws ServiceException {
        try {
            return comptageDAO.findBySession(idSession);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des comptages", e);
        }
    }

    /**
     * Recupere les comptages avec ecart d'une session.
     *
     * @param idSession l'ID de la session
     * @return la liste des comptages avec ecart
     * @throws ServiceException si une erreur survient
     */
    public List<ComptageInventaire> getComptagesAvecEcart(int idSession) throws ServiceException {
        try {
            return comptageDAO.findWithEcart(idSession);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des ecarts", e);
        }
    }

    // ==================== GESTION DES REGULARISATIONS ====================

    /**
     * Applique les regularisations pour une session.
     * Met a jour les quantites de stock des lots concernes.
     *
     * @param idSession     l'ID de la session
     * @param idUtilisateur l'ID de l'utilisateur
     * @return le nombre de regularisations appliquees
     * @throws ServiceException si une erreur survient
     */
    public int appliquerRegularisations(int idSession, int idUtilisateur) throws ServiceException {
        logger.info("Application des regularisations pour la session {}", idSession);

        Connection conn = null;
        try {
            // Verifier que la session existe et est en cours
            SessionInventaire session = getSessionById(idSession);
            if (!session.isEnCours()) {
                throw new ServiceException("La session n'est pas en cours", ErrorType.VALIDATION);
            }

            // Recuperer les comptages avec ecart
            List<ComptageInventaire> comptagesAvecEcart = comptageDAO.findWithEcart(idSession);

            if (comptagesAvecEcart.isEmpty()) {
                logger.info("Aucun ecart a regulariser");
                return 0;
            }

            // Demarrer la transaction
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            int nbRegularisations = 0;

            for (ComptageInventaire comptage : comptagesAvecEcart) {
                // Verifier que le motif est renseigne
                if (comptage.getMotifEcart() == null) {
                    throw new ServiceException(
                            "Motif manquant pour le lot " + comptage.getNumeroLot(), ErrorType.VALIDATION);
                }

                // Creer la regularisation
                Regularisation regularisation = new Regularisation(
                        idSession,
                        comptage.getIdLot(),
                        comptage.getQuantiteTheorique(),
                        comptage.getQuantitePhysique(),
                        comptage.getMotifEcart(),
                        idUtilisateur
                );
                regularisation.setJustificatif(comptage.getCommentaire());
                regularisationDAO.save(regularisation);

                // Mettre a jour le stock du lot
                lotDAO.updateQuantite(comptage.getIdLot(), comptage.getQuantitePhysique());

                logger.debug("Regularisation appliquee: lot={}, {} -> {}",
                        comptage.getIdLot(), comptage.getQuantiteTheorique(), comptage.getQuantitePhysique());

                nbRegularisations++;
            }

            conn.commit();

            // Audit
            auditService.log(TypeAction.MODIFICATION, "SessionInventaire", idSession,
                    String.format("Regularisations appliquees: %d lots ajustes", nbRegularisations));

            logger.info("{} regularisations appliquees pour la session {}", nbRegularisations, idSession);
            return nbRegularisations;

        } catch (SQLException e) {
            rollback(conn);
            logger.error("Erreur SQL lors de l'application des regularisations", e);
            throw new ServiceException("Erreur lors de l'application des regularisations", e);

        } catch (DAOException e) {
            rollback(conn);
            logger.error("Erreur DAO lors de l'application des regularisations", e);
            throw new ServiceException("Erreur lors de l'application des regularisations", e);

        } catch (ServiceException e) {
            rollback(conn);
            throw e;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Recupere les regularisations d'une session.
     *
     * @param idSession l'ID de la session
     * @return la liste des regularisations
     * @throws ServiceException si une erreur survient
     */
    public List<Regularisation> getRegularisationsBySession(int idSession) throws ServiceException {
        try {
            return regularisationDAO.findBySession(idSession);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des regularisations", e);
        }
    }

    // ==================== STATISTIQUES ====================

    /**
     * Compte le nombre de comptages pour une session.
     *
     * @param idSession l'ID de la session
     * @return le nombre de comptages
     * @throws ServiceException si une erreur survient
     */
    public int countComptages(int idSession) throws ServiceException {
        try {
            return comptageDAO.countBySession(idSession);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du comptage", e);
        }
    }

    /**
     * Compte le nombre d'ecarts pour une session.
     *
     * @param idSession l'ID de la session
     * @return le nombre d'ecarts
     * @throws ServiceException si une erreur survient
     */
    public int countEcarts(int idSession) throws ServiceException {
        try {
            return comptageDAO.countEcarts(idSession);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du comptage des ecarts", e);
        }
    }

    /**
     * Calcule le total des ecarts pour une session.
     *
     * @param idSession l'ID de la session
     * @return le total des ecarts (positif ou negatif)
     * @throws ServiceException si une erreur survient
     */
    public int getTotalEcart(int idSession) throws ServiceException {
        try {
            return comptageDAO.getTotalEcart(idSession);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du calcul des ecarts", e);
        }
    }

    /**
     * Recupere tous les lots pour le comptage.
     *
     * @return la liste des lots avec medicaments
     * @throws ServiceException si une erreur survient
     */
    public List<Lot> getAllLotsForComptage() throws ServiceException {
        try {
            List<Lot> allLots = lotDAO.findAll();
            // Exclure les lots perimes a stock 0 (inutiles pour l'inventaire)
            LocalDate today = LocalDate.now();
            allLots.removeIf(lot ->
                    lot.getQuantiteStock() == 0 &&
                    lot.getDatePeremption() != null &&
                    lot.getDatePeremption().isBefore(today));
            return allLots;
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des lots", e);
        }
    }

    /**
     * Effectue un rollback de la transaction.
     */
    private void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
                logger.warn("Transaction annulee (rollback)");
            } catch (SQLException e) {
                logger.error("Erreur lors du rollback", e);
            }
        }
    }
}
