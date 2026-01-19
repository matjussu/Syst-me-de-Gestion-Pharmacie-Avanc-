package com.sgpa.service;

import com.sgpa.dao.CommandeDAO;
import com.sgpa.dao.LotDAO;
import com.sgpa.dao.impl.CommandeDAOImpl;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.exception.ServiceException.ErrorType;
import com.sgpa.model.Commande;
import com.sgpa.model.LigneCommande;
import com.sgpa.model.Lot;
import com.sgpa.model.enums.StatutCommande;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service de gestion des commandes fournisseurs.
 * <p>
 * Gere le cycle de vie des commandes :
 * <ul>
 *   <li>Creation de commandes</li>
 *   <li>Reception et creation automatique des lots</li>
 *   <li>Annulation</li>
 * </ul>
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class CommandeService {

    private static final Logger logger = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeDAO commandeDAO;
    private final LotDAO lotDAO;

    /**
     * Constructeur par defaut.
     */
    public CommandeService() {
        this.commandeDAO = new CommandeDAOImpl();
        this.lotDAO = new LotDAOImpl();
    }

    /**
     * Constructeur avec injection des DAOs (pour tests).
     *
     * @param commandeDAO le DAO commande
     * @param lotDAO      le DAO lot
     */
    public CommandeService(CommandeDAO commandeDAO, LotDAO lotDAO) {
        this.commandeDAO = commandeDAO;
        this.lotDAO = lotDAO;
    }

    /**
     * Cree une nouvelle commande fournisseur.
     *
     * @param idFournisseur l'ID du fournisseur
     * @param lignes        les lignes de commande
     * @return la commande creee
     * @throws ServiceException si une erreur survient
     */
    public Commande creerCommande(int idFournisseur, List<LigneCommande> lignes) throws ServiceException {
        if (lignes == null || lignes.isEmpty()) {
            throw new ServiceException("La commande doit contenir au moins une ligne", ErrorType.VALIDATION);
        }

        logger.info("Creation d'une commande pour le fournisseur {}", idFournisseur);

        try {
            Commande commande = new Commande();
            commande.setIdFournisseur(idFournisseur);
            commande.setDateCreation(LocalDateTime.now());
            commande.setStatut(StatutCommande.EN_ATTENTE);

            commande = commandeDAO.save(commande);

            // Sauvegarder les lignes
            for (LigneCommande ligne : lignes) {
                ligne.setIdCommande(commande.getIdCommande());
                commandeDAO.saveLigneCommande(ligne);
            }

            commande.setLignesCommande(lignes);
            logger.info("Commande {} creee avec {} ligne(s)", commande.getIdCommande(), lignes.size());

            return commande;

        } catch (DAOException e) {
            logger.error("Erreur lors de la creation de la commande", e);
            throw new ServiceException("Erreur lors de la creation de la commande", e);
        }
    }

    /**
     * Recoit une commande et cree les lots correspondants.
     *
     * @param idCommande     l'ID de la commande
     * @param receptions     les informations de reception (quantites recues, dates peremption)
     * @throws ServiceException si une erreur survient
     */
    public void recevoirCommande(int idCommande, List<ReceptionInfo> receptions) throws ServiceException {
        logger.info("Reception de la commande {}", idCommande);

        try {
            Commande commande = commandeDAO.findById(idCommande)
                    .orElseThrow(() -> new ServiceException(
                            "Commande non trouvee: " + idCommande, ErrorType.NOT_FOUND));

            if (commande.getStatut() != StatutCommande.EN_ATTENTE) {
                throw new ServiceException(
                        "La commande ne peut pas etre recue (statut: " + commande.getStatut() + ")",
                        ErrorType.VALIDATION);
            }

            // Creer les lots pour chaque reception
            for (ReceptionInfo reception : receptions) {
                Lot lot = new Lot();
                lot.setIdMedicament(reception.idMedicament);
                lot.setIdFournisseur(commande.getIdFournisseur());
                lot.setNumeroLot(reception.numeroLot);
                lot.setDatePeremption(reception.datePeremption);
                lot.setDateFabrication(reception.dateFabrication);
                lot.setQuantiteStock(reception.quantiteRecue);
                lot.setPrixAchat(reception.prixAchat);

                lotDAO.save(lot);
                logger.debug("Lot cree: {} ({} unites)", reception.numeroLot, reception.quantiteRecue);
            }

            // Mettre a jour le statut de la commande
            commande.setStatut(StatutCommande.RECUE);
            commande.setDateReception(LocalDateTime.now());
            commandeDAO.update(commande);

            logger.info("Commande {} recue avec {} lot(s) cree(s)", idCommande, receptions.size());

        } catch (DAOException e) {
            logger.error("Erreur lors de la reception de la commande", e);
            throw new ServiceException("Erreur lors de la reception de la commande", e);
        }
    }

    /**
     * Annule une commande.
     *
     * @param idCommande l'ID de la commande
     * @throws ServiceException si une erreur survient
     */
    public void annulerCommande(int idCommande) throws ServiceException {
        logger.info("Annulation de la commande {}", idCommande);

        try {
            Commande commande = commandeDAO.findById(idCommande)
                    .orElseThrow(() -> new ServiceException(
                            "Commande non trouvee: " + idCommande, ErrorType.NOT_FOUND));

            if (commande.getStatut() == StatutCommande.RECUE) {
                throw new ServiceException(
                        "Une commande deja recue ne peut pas etre annulee",
                        ErrorType.VALIDATION);
            }

            commande.setStatut(StatutCommande.ANNULEE);
            commandeDAO.update(commande);

            logger.info("Commande {} annulee", idCommande);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'annulation de la commande", e);
            throw new ServiceException("Erreur lors de l'annulation de la commande", e);
        }
    }

    /**
     * Retourne les commandes en attente.
     *
     * @return la liste des commandes en attente
     * @throws ServiceException si une erreur survient
     */
    public List<Commande> getCommandesEnAttente() throws ServiceException {
        try {
            return commandeDAO.findByStatut(StatutCommande.EN_ATTENTE);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des commandes", e);
        }
    }

    /**
     * Retourne toutes les commandes.
     *
     * @return la liste de toutes les commandes
     * @throws ServiceException si une erreur survient
     */
    public List<Commande> getAllCommandes() throws ServiceException {
        try {
            return commandeDAO.findAll();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des commandes", e);
        }
    }

    /**
     * Informations pour la reception d'un article.
     */
    public static class ReceptionInfo {
        public int idMedicament;
        public String numeroLot;
        public LocalDate datePeremption;
        public LocalDate dateFabrication;
        public int quantiteRecue;
        public BigDecimal prixAchat;

        public ReceptionInfo(int idMedicament, String numeroLot, LocalDate datePeremption,
                             int quantiteRecue, BigDecimal prixAchat) {
            this.idMedicament = idMedicament;
            this.numeroLot = numeroLot;
            this.datePeremption = datePeremption;
            this.quantiteRecue = quantiteRecue;
            this.prixAchat = prixAchat;
        }
    }
}
