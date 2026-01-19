package com.sgpa.service;

import com.sgpa.dao.LotDAO;
import com.sgpa.dao.RetourDAO;
import com.sgpa.dao.VenteDAO;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.dao.impl.RetourDAOImpl;
import com.sgpa.dao.impl.VenteDAOImpl;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.exception.ServiceException.ErrorType;
import com.sgpa.model.LigneVente;
import com.sgpa.model.Lot;
import com.sgpa.model.Retour;
import com.sgpa.model.Vente;
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
 * Service de gestion des retours produits.
 * <p>
 * Ce service gere les retours de produits vendus avec possibilite
 * de reintegration au stock si le lot n'est pas perime.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class RetourService {

    private static final Logger logger = LoggerFactory.getLogger(RetourService.class);

    private final RetourDAO retourDAO;
    private final VenteDAO venteDAO;
    private final LotDAO lotDAO;
    private final AuditService auditService;

    /**
     * Constructeur par defaut.
     */
    public RetourService() {
        this.retourDAO = new RetourDAOImpl();
        this.venteDAO = new VenteDAOImpl();
        this.lotDAO = new LotDAOImpl();
        this.auditService = new AuditService();
    }

    /**
     * Constructeur avec injection des DAOs (pour tests).
     *
     * @param retourDAO    le DAO retour
     * @param venteDAO     le DAO vente
     * @param lotDAO       le DAO lot
     * @param auditService le service d'audit
     */
    public RetourService(RetourDAO retourDAO, VenteDAO venteDAO, LotDAO lotDAO, AuditService auditService) {
        this.retourDAO = retourDAO;
        this.venteDAO = venteDAO;
        this.lotDAO = lotDAO;
        this.auditService = auditService;
    }

    /**
     * Enregistre un retour de produit.
     * <p>
     * Si le parametre reintegrer est true et que le lot n'est pas perime,
     * la quantite est reintegree au stock.
     * </p>
     *
     * @param idVente       l'ID de la vente d'origine
     * @param idLot         l'ID du lot retourne
     * @param quantite      la quantite retournee
     * @param motif         le motif du retour
     * @param reintegrer    true pour reintegrer au stock
     * @param commentaire   commentaire optionnel
     * @param idUtilisateur l'ID de l'utilisateur effectuant le retour
     * @return le retour cree
     * @throws ServiceException si une erreur survient
     */
    public Retour enregistrerRetour(int idVente, int idLot, int quantite, String motif,
                                     boolean reintegrer, String commentaire, int idUtilisateur)
            throws ServiceException {

        logger.info("Enregistrement retour: vente={}, lot={}, qte={}, reintegrer={}",
                idVente, idLot, quantite, reintegrer);

        // Validations
        if (quantite <= 0) {
            throw new ServiceException("La quantite doit etre superieure a 0", ErrorType.VALIDATION);
        }
        if (motif == null || motif.trim().isEmpty()) {
            throw new ServiceException("Le motif est obligatoire", ErrorType.VALIDATION);
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);

            // Verifier que la vente existe
            Vente vente = venteDAO.findById(idVente)
                    .orElseThrow(() -> new ServiceException(
                            "Vente non trouvee: " + idVente, ErrorType.NOT_FOUND));

            // Verifier que le lot existe
            Lot lot = lotDAO.findById(idLot)
                    .orElseThrow(() -> new ServiceException(
                            "Lot non trouve: " + idLot, ErrorType.NOT_FOUND));

            // Verifier que le lot fait partie de cette vente
            List<LigneVente> lignesVente = venteDAO.findLignesByVenteId(idVente);
            int quantiteVendueTotal = lignesVente.stream()
                    .filter(l -> l.getIdLot() == idLot)
                    .mapToInt(LigneVente::getQuantite)
                    .sum();

            if (quantiteVendueTotal == 0) {
                throw new ServiceException(
                        "Ce lot n'a pas ete vendu dans cette vente", ErrorType.VALIDATION);
            }

            // Verifier que la quantite retournee ne depasse pas la quantite vendue
            // moins les retours deja effectues
            List<Retour> retoursExistants = retourDAO.findByVente(idVente);
            int quantiteDejaRetournee = retoursExistants.stream()
                    .filter(r -> r.getIdLot() == idLot)
                    .mapToInt(Retour::getQuantite)
                    .sum();

            int quantiteRetournableRestante = quantiteVendueTotal - quantiteDejaRetournee;
            if (quantite > quantiteRetournableRestante) {
                throw new ServiceException(
                        String.format("Quantite retournable depassee. Maximum: %d, Demande: %d",
                                quantiteRetournableRestante, quantite),
                        ErrorType.VALIDATION);
            }

            // Determiner si on peut reintegrer (lot non perime)
            boolean reintegrationEffective = reintegrer && !lot.isPerime();

            if (reintegrer && lot.isPerime()) {
                logger.warn("Reintegration impossible: lot {} perime", lot.getNumeroLot());
            }

            // Creer le retour
            Retour retour = new Retour(idVente, idLot, idUtilisateur, quantite, motif);
            retour.setReintegre(reintegrationEffective);
            retour.setCommentaire(commentaire);
            retour.setDateRetour(LocalDateTime.now());
            retour.setVente(vente);
            retour.setLot(lot);

            // Sauvegarder le retour
            retour = retourDAO.save(retour);

            // Reintegrer au stock si applicable
            if (reintegrationEffective) {
                int nouvelleQuantite = lot.getQuantiteStock() + quantite;
                lotDAO.updateQuantite(idLot, nouvelleQuantite);
                logger.info("Stock reintegre: lot={}, nouvelle qte={}", lot.getNumeroLot(), nouvelleQuantite);
            }

            // Audit
            String details = String.format("Retour #%d: Vente #%d, Lot %s, Qte: %d, Reintegre: %s",
                    retour.getIdRetour(), idVente, lot.getNumeroLot(), quantite, reintegrationEffective);
            auditService.log(TypeAction.CREATION, "Retour", retour.getIdRetour(), details);

            conn.commit();
            logger.info("Retour {} enregistre avec succes", retour.getIdRetour());

            return retour;

        } catch (SQLException e) {
            rollback(conn);
            logger.error("Erreur SQL lors de l'enregistrement du retour", e);
            throw new ServiceException("Erreur lors de l'enregistrement du retour", e);

        } catch (DAOException e) {
            rollback(conn);
            logger.error("Erreur DAO lors de l'enregistrement du retour", e);
            throw new ServiceException("Erreur lors de l'enregistrement du retour", e);

        } catch (ServiceException e) {
            rollback(conn);
            throw e;

        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Recupere tous les retours.
     *
     * @return la liste de tous les retours
     * @throws ServiceException si une erreur survient
     */
    public List<Retour> getAllRetours() throws ServiceException {
        try {
            return retourDAO.findAll();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des retours", e);
        }
    }

    /**
     * Recupere les retours pour une vente donnee.
     *
     * @param idVente l'ID de la vente
     * @return la liste des retours
     * @throws ServiceException si une erreur survient
     */
    public List<Retour> getRetoursByVente(int idVente) throws ServiceException {
        try {
            return retourDAO.findByVente(idVente);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des retours", e);
        }
    }

    /**
     * Recupere les retours dans une periode donnee.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des retours
     * @throws ServiceException si une erreur survient
     */
    public List<Retour> getRetoursByPeriode(LocalDate dateDebut, LocalDate dateFin) throws ServiceException {
        try {
            return retourDAO.findByDateRange(dateDebut, dateFin);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des retours", e);
        }
    }

    /**
     * Recupere les retours effectues par un utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @return la liste des retours
     * @throws ServiceException si une erreur survient
     */
    public List<Retour> getRetoursByUtilisateur(int idUtilisateur) throws ServiceException {
        try {
            return retourDAO.findByUtilisateur(idUtilisateur);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des retours", e);
        }
    }

    /**
     * Recupere les retours reintegres ou non.
     *
     * @param reintegre true pour les retours reintegres
     * @return la liste des retours
     * @throws ServiceException si une erreur survient
     */
    public List<Retour> getRetoursByReintegre(boolean reintegre) throws ServiceException {
        try {
            return retourDAO.findByReintegre(reintegre);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des retours", e);
        }
    }

    /**
     * Recupere un retour par son ID.
     *
     * @param idRetour l'ID du retour
     * @return le retour
     * @throws ServiceException si le retour n'existe pas
     */
    public Retour getRetourById(int idRetour) throws ServiceException {
        try {
            return retourDAO.findById(idRetour)
                    .orElseThrow(() -> new ServiceException(
                            "Retour non trouve: " + idRetour, ErrorType.NOT_FOUND));
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation du retour", e);
        }
    }

    /**
     * Recherche une vente par son ID pour le formulaire de retour.
     *
     * @param idVente l'ID de la vente
     * @return la vente avec ses lignes
     * @throws ServiceException si la vente n'existe pas
     */
    public Vente rechercherVente(int idVente) throws ServiceException {
        try {
            Vente vente = venteDAO.findById(idVente)
                    .orElseThrow(() -> new ServiceException(
                            "Vente non trouvee: " + idVente, ErrorType.NOT_FOUND));

            // Charger les lignes de vente
            List<LigneVente> lignes = venteDAO.findLignesByVenteId(idVente);
            vente.setLignesVente(lignes);

            return vente;
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche de la vente", e);
        }
    }

    /**
     * Compte le nombre total de retours.
     *
     * @return le nombre de retours
     * @throws ServiceException si une erreur survient
     */
    public long countRetours() throws ServiceException {
        try {
            return retourDAO.count();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du comptage des retours", e);
        }
    }

    /**
     * Effectue un rollback de la transaction.
     *
     * @param conn la connexion
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
