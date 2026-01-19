package com.sgpa.service;

import com.sgpa.dao.LotDAO;
import com.sgpa.dao.MedicamentDAO;
import com.sgpa.dao.VenteDAO;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.dao.impl.MedicamentDAOImpl;
import com.sgpa.dao.impl.VenteDAOImpl;
import com.sgpa.dto.LigneVenteDTO;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.exception.ServiceException.ErrorType;
import com.sgpa.model.LigneVente;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import com.sgpa.model.Vente;
import com.sgpa.utils.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des ventes avec algorithme FEFO.
 * <p>
 * Ce service est le coeur du systeme de gestion des ventes.
 * Il implemente l'algorithme <b>FEFO (First Expired, First Out)</b> qui deduit
 * automatiquement les quantites des lots dont la date de peremption est la plus proche.
 * </p>
 * <p>
 * <b>Gestion transactionnelle :</b> Les ventes sont effectuees dans une transaction
 * pour garantir la coherence des donnees. En cas d'erreur, un rollback est effectue.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class VenteService {

    private static final Logger logger = LoggerFactory.getLogger(VenteService.class);

    private final VenteDAO venteDAO;
    private final MedicamentDAO medicamentDAO;
    private final LotDAO lotDAO;

    /**
     * Constructeur par defaut.
     */
    public VenteService() {
        this.venteDAO = new VenteDAOImpl();
        this.medicamentDAO = new MedicamentDAOImpl();
        this.lotDAO = new LotDAOImpl();
    }

    /**
     * Constructeur avec injection des DAOs (pour tests).
     *
     * @param venteDAO       le DAO vente
     * @param medicamentDAO  le DAO medicament
     * @param lotDAO         le DAO lot
     */
    public VenteService(VenteDAO venteDAO, MedicamentDAO medicamentDAO, LotDAO lotDAO) {
        this.venteDAO = venteDAO;
        this.medicamentDAO = medicamentDAO;
        this.lotDAO = lotDAO;
    }

    /**
     * Cree une nouvelle vente en appliquant l'algorithme FEFO.
     * <p>
     * Cette methode :
     * <ol>
     *   <li>Valide la disponibilite du stock pour chaque ligne</li>
     *   <li>Applique l'algorithme FEFO pour deduire les quantites</li>
     *   <li>Cree la vente et les lignes de vente avec tracabilite des lots</li>
     *   <li>Effectue le tout dans une transaction</li>
     * </ol>
     * </p>
     *
     * @param lignes        les lignes de vente (medicament + quantite)
     * @param idUtilisateur l'ID de l'utilisateur effectuant la vente
     * @param surOrdonnance true si la vente est sur ordonnance
     * @return la vente creee
     * @throws ServiceException si une erreur survient ou si le stock est insuffisant
     */
    public Vente creerVente(List<LigneVenteDTO> lignes, int idUtilisateur, boolean surOrdonnance)
            throws ServiceException {

        if (lignes == null || lignes.isEmpty()) {
            throw new ServiceException("La vente doit contenir au moins une ligne", ErrorType.VALIDATION);
        }

        logger.info("Creation d'une vente avec {} ligne(s) par utilisateur {}",
                lignes.size(), idUtilisateur);

        Connection conn = null;
        try {
            // Demarrer la transaction
            conn = DatabaseConnection.getInstance().getConnection();
            conn.setAutoCommit(false);
            logger.debug("Transaction demarree");

            // 1. Valider toutes les lignes avant de commencer
            for (LigneVenteDTO ligne : lignes) {
                validerLigneVente(ligne, surOrdonnance);
            }

            // 2. Calculer le montant total
            BigDecimal montantTotal = BigDecimal.ZERO;
            List<LigneVenteInfo> lignesInfo = new ArrayList<>();

            for (LigneVenteDTO ligne : lignes) {
                Medicament med = medicamentDAO.findById(ligne.getIdMedicament())
                        .orElseThrow(() -> new ServiceException(
                                "Medicament non trouve: " + ligne.getIdMedicament(),
                                ErrorType.NOT_FOUND));

                BigDecimal prixUnitaire = ligne.getPrixUnitaire() != null
                        ? ligne.getPrixUnitaire()
                        : med.getPrixPublic();

                BigDecimal montantLigne = prixUnitaire.multiply(BigDecimal.valueOf(ligne.getQuantite()));
                montantTotal = montantTotal.add(montantLigne);

                lignesInfo.add(new LigneVenteInfo(med, ligne.getQuantite(), prixUnitaire));
            }

            // 3. Creer l'entete de vente
            Vente vente = new Vente();
            vente.setDateVente(LocalDateTime.now());
            vente.setMontantTotal(montantTotal);
            vente.setEstSurOrdonnance(surOrdonnance);
            vente.setIdUtilisateur(idUtilisateur);

            vente = venteDAO.save(vente);
            logger.debug("Vente creee avec ID: {}", vente.getIdVente());

            // 4. Appliquer FEFO et creer les lignes de vente
            List<LigneVente> lignesVente = new ArrayList<>();

            for (LigneVenteInfo info : lignesInfo) {
                List<LotUtilise> lotsUtilises = deduireStockFEFO(info.medicament.getIdMedicament(),
                        info.quantite, conn);

                for (LotUtilise lotUtilise : lotsUtilises) {
                    LigneVente ligneVente = new LigneVente();
                    ligneVente.setIdVente(vente.getIdVente());
                    ligneVente.setIdLot(lotUtilise.lot.getIdLot());
                    ligneVente.setQuantite(lotUtilise.quantitePrelevee);
                    ligneVente.setPrixUnitaireApplique(info.prixUnitaire);

                    // Assigner le lot avec le medicament pour la generation de rapports/tickets
                    Lot lotComplet = lotUtilise.lot;
                    lotComplet.setMedicament(info.medicament);
                    ligneVente.setLot(lotComplet);

                    venteDAO.saveLigneVente(ligneVente);
                    lignesVente.add(ligneVente);

                    logger.debug("Ligne de vente creee: Lot={}, Qte={}, Prix={}",
                            lotUtilise.lot.getNumeroLot(), lotUtilise.quantitePrelevee, info.prixUnitaire);
                }
            }

            vente.setLignesVente(lignesVente);

            // 5. Commit de la transaction
            conn.commit();
            logger.info("Vente {} creee avec succes. Montant total: {}", vente.getIdVente(), montantTotal);

            return vente;

        } catch (SQLException e) {
            // Rollback en cas d'erreur SQL
            rollback(conn);
            logger.error("Erreur SQL lors de la creation de la vente", e);
            throw new ServiceException("Erreur lors de la creation de la vente", e);

        } catch (DAOException e) {
            // Rollback en cas d'erreur DAO
            rollback(conn);
            logger.error("Erreur DAO lors de la creation de la vente", e);
            throw new ServiceException("Erreur lors de la creation de la vente", e);

        } catch (ServiceException e) {
            // Rollback en cas d'erreur metier
            rollback(conn);
            throw e;

        } finally {
            // Restaurer l'auto-commit et liberer la connexion
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * Applique l'algorithme FEFO pour deduire le stock.
     * <p>
     * <b>FEFO (First Expired, First Out)</b> : Les lots avec la date de peremption
     * la plus proche sont utilises en premier.
     * </p>
     *
     * @param idMedicament     l'ID du medicament
     * @param quantiteDemandee la quantite a deduire
     * @param conn             la connexion pour la transaction
     * @return la liste des lots utilises avec les quantites prelevees
     * @throws ServiceException si le stock est insuffisant
     * @throws DAOException     si une erreur d'acces aux donnees survient
     */
    private List<LotUtilise> deduireStockFEFO(int idMedicament, int quantiteDemandee, Connection conn)
            throws ServiceException, DAOException {

        logger.debug("Application FEFO pour medicament {}: {} unites demandees",
                idMedicament, quantiteDemandee);

        List<LotUtilise> lotsUtilises = new ArrayList<>();

        // Recuperer les lots tries par date de peremption (FEFO)
        List<Lot> lots = lotDAO.findVendableByMedicament(idMedicament);

        int quantiteRestante = quantiteDemandee;

        for (Lot lot : lots) {
            if (quantiteRestante <= 0) break;

            // Ignorer les lots perimes
            if (lot.isPerime()) {
                logger.debug("Lot {} ignore (perime)", lot.getNumeroLot());
                continue;
            }

            // Ignorer les lots sans stock
            if (lot.getQuantiteStock() <= 0) {
                continue;
            }

            // Calculer la quantite a prendre de ce lot
            int aDeduire = Math.min(lot.getQuantiteStock(), quantiteRestante);

            // Mettre a jour le lot
            int nouvelleQuantite = lot.getQuantiteStock() - aDeduire;
            lotDAO.updateQuantite(lot.getIdLot(), nouvelleQuantite);

            logger.debug("Lot {} (expire {}): -{} unites (reste {})",
                    lot.getNumeroLot(), lot.getDatePeremption(), aDeduire, nouvelleQuantite);

            lotsUtilises.add(new LotUtilise(lot, aDeduire));
            quantiteRestante -= aDeduire;
        }

        // Verifier que tout a ete deduit
        if (quantiteRestante > 0) {
            throw new ServiceException(
                    String.format("Stock insuffisant pour le medicament %d. Manque %d unites.",
                            idMedicament, quantiteRestante),
                    ErrorType.STOCK_INSUFFISANT);
        }

        logger.debug("FEFO applique: {} lot(s) utilise(s)", lotsUtilises.size());
        return lotsUtilises;
    }

    /**
     * Valide une ligne de vente avant traitement.
     *
     * @param ligne         la ligne a valider
     * @param surOrdonnance true si la vente est sur ordonnance
     * @throws ServiceException si la validation echoue
     */
    private void validerLigneVente(LigneVenteDTO ligne, boolean surOrdonnance) throws ServiceException {
        if (ligne.getIdMedicament() <= 0) {
            throw new ServiceException("ID medicament invalide", ErrorType.VALIDATION);
        }
        if (ligne.getQuantite() <= 0) {
            throw new ServiceException("La quantite doit etre superieure a 0", ErrorType.VALIDATION);
        }

        try {
            // Verifier que le medicament existe
            Medicament med = medicamentDAO.findById(ligne.getIdMedicament())
                    .orElseThrow(() -> new ServiceException(
                            "Medicament non trouve: " + ligne.getIdMedicament(),
                            ErrorType.NOT_FOUND));

            // Verifier si ordonnance requise
            if (med.isNecessiteOrdonnance() && !surOrdonnance) {
                throw new ServiceException(
                        "Le medicament " + med.getNomCommercial() + " necessite une ordonnance",
                        ErrorType.ORDONNANCE_REQUISE);
            }

            // Verifier le stock disponible
            int stockVendable = lotDAO.findVendableByMedicament(ligne.getIdMedicament())
                    .stream()
                    .mapToInt(Lot::getQuantiteStock)
                    .sum();

            if (stockVendable < ligne.getQuantite()) {
                throw new ServiceException(
                        String.format("Stock insuffisant pour %s. Disponible: %d, Demande: %d",
                                med.getNomCommercial(), stockVendable, ligne.getQuantite()),
                        ErrorType.STOCK_INSUFFISANT);
            }

        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la validation", e);
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

    /**
     * Recupere l'historique des ventes.
     *
     * @return la liste de toutes les ventes
     * @throws ServiceException si une erreur survient
     */
    public List<Vente> getHistoriqueVentes() throws ServiceException {
        try {
            return venteDAO.findAll();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation de l'historique", e);
        }
    }

    /**
     * Recupere une vente par son ID.
     *
     * @param idVente l'ID de la vente
     * @return la vente
     * @throws ServiceException si la vente n'existe pas
     */
    public Vente getVenteById(int idVente) throws ServiceException {
        try {
            return venteDAO.findById(idVente)
                    .orElseThrow(() -> new ServiceException(
                            "Vente non trouvee: " + idVente,
                            ErrorType.NOT_FOUND));
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation de la vente", e);
        }
    }

    // Classes internes pour le traitement

    /**
     * Information sur une ligne de vente en cours de traitement.
     */
    private static class LigneVenteInfo {
        final Medicament medicament;
        final int quantite;
        final BigDecimal prixUnitaire;

        LigneVenteInfo(Medicament medicament, int quantite, BigDecimal prixUnitaire) {
            this.medicament = medicament;
            this.quantite = quantite;
            this.prixUnitaire = prixUnitaire;
        }
    }

    /**
     * Information sur un lot utilise lors d'une vente.
     */
    private static class LotUtilise {
        final Lot lot;
        final int quantitePrelevee;

        LotUtilise(Lot lot, int quantitePrelevee) {
            this.lot = lot;
            this.quantitePrelevee = quantitePrelevee;
        }
    }
}
