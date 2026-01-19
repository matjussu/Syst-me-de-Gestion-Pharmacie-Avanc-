package com.sgpa.service;

import com.sgpa.dao.LotDAO;
import com.sgpa.dao.MedicamentDAO;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.dao.impl.MedicamentDAOImpl;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.exception.ServiceException.ErrorType;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service de gestion du stock.
 * <p>
 * Fournit les operations de consultation et de gestion du stock :
 * <ul>
 *   <li>Consultation du stock total par medicament</li>
 *   <li>Inventaire complet</li>
 *   <li>Ajout de stock (reception de lots)</li>
 *   <li>Detection des medicaments en stock bas</li>
 * </ul>
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class StockService {

    private static final Logger logger = LoggerFactory.getLogger(StockService.class);

    private final MedicamentDAO medicamentDAO;
    private final LotDAO lotDAO;

    /**
     * Constructeur par defaut.
     */
    public StockService() {
        this.medicamentDAO = new MedicamentDAOImpl();
        this.lotDAO = new LotDAOImpl();
    }

    /**
     * Constructeur avec injection des DAOs (pour tests).
     *
     * @param medicamentDAO le DAO medicament
     * @param lotDAO        le DAO lot
     */
    public StockService(MedicamentDAO medicamentDAO, LotDAO lotDAO) {
        this.medicamentDAO = medicamentDAO;
        this.lotDAO = lotDAO;
    }

    /**
     * Retourne le stock total d'un medicament (somme de tous les lots).
     *
     * @param idMedicament l'ID du medicament
     * @return le stock total
     * @throws ServiceException si une erreur survient
     */
    public int getStockTotal(int idMedicament) throws ServiceException {
        try {
            return lotDAO.getTotalStockByMedicament(idMedicament);
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation du stock total", e);
            throw new ServiceException("Erreur lors de la recuperation du stock", e);
        }
    }

    /**
     * Retourne le stock vendable d'un medicament (lots non perimes).
     *
     * @param idMedicament l'ID du medicament
     * @return le stock vendable
     * @throws ServiceException si une erreur survient
     */
    public int getStockVendable(int idMedicament) throws ServiceException {
        try {
            List<Lot> lotsVendables = lotDAO.findVendableByMedicament(idMedicament);
            return lotsVendables.stream()
                    .mapToInt(Lot::getQuantiteStock)
                    .sum();
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation du stock vendable", e);
            throw new ServiceException("Erreur lors de la recuperation du stock vendable", e);
        }
    }

    /**
     * Retourne l'inventaire complet (tous les medicaments avec leur stock).
     *
     * @return une map medicament -> stock total
     * @throws ServiceException si une erreur survient
     */
    public Map<Medicament, Integer> getInventaireComplet() throws ServiceException {
        logger.debug("Generation de l'inventaire complet");
        Map<Medicament, Integer> inventaire = new HashMap<>();

        try {
            List<Medicament> medicaments = medicamentDAO.findAllActive();

            for (Medicament med : medicaments) {
                int stock = lotDAO.getTotalStockByMedicament(med.getIdMedicament());
                inventaire.put(med, stock);
            }

            logger.info("Inventaire genere: {} medicaments", inventaire.size());
            return inventaire;

        } catch (DAOException e) {
            logger.error("Erreur lors de la generation de l'inventaire", e);
            throw new ServiceException("Erreur lors de la generation de l'inventaire", e);
        }
    }

    /**
     * Retourne les medicaments en stock bas (sous le seuil minimum).
     *
     * @return la liste des medicaments en stock bas
     * @throws ServiceException si une erreur survient
     */
    public List<Medicament> getMedicamentsEnStockBas() throws ServiceException {
        try {
            return medicamentDAO.findBelowThreshold();
        } catch (DAOException e) {
            logger.error("Erreur lors de la recherche des medicaments en stock bas", e);
            throw new ServiceException("Erreur lors de la recherche des medicaments en stock bas", e);
        }
    }

    /**
     * Ajoute un nouveau lot de stock pour un medicament.
     *
     * @param lot le lot a ajouter
     * @return le lot sauvegarde avec son ID
     * @throws ServiceException si une erreur survient
     */
    public Lot ajouterStock(Lot lot) throws ServiceException {
        logger.info("Ajout de stock: {} unites pour medicament ID {}",
                lot.getQuantiteStock(), lot.getIdMedicament());

        validateLot(lot);

        try {
            // Verifier que le medicament existe
            medicamentDAO.findById(lot.getIdMedicament())
                    .orElseThrow(() -> new ServiceException(
                            "Medicament non trouve: " + lot.getIdMedicament(),
                            ErrorType.NOT_FOUND));

            Lot savedLot = lotDAO.save(lot);
            logger.info("Lot ajoute avec succes: ID={}, Numero={}",
                    savedLot.getIdLot(), savedLot.getNumeroLot());

            return savedLot;

        } catch (DAOException e) {
            logger.error("Erreur lors de l'ajout du lot", e);
            throw new ServiceException("Erreur lors de l'ajout du stock", e);
        }
    }

    /**
     * Met a jour la quantite d'un lot existant.
     *
     * @param idLot           l'ID du lot
     * @param nouvelleQuantite la nouvelle quantite
     * @throws ServiceException si une erreur survient
     */
    public void updateQuantiteLot(int idLot, int nouvelleQuantite) throws ServiceException {
        if (nouvelleQuantite < 0) {
            throw new ServiceException("La quantite ne peut pas etre negative", ErrorType.VALIDATION);
        }

        try {
            lotDAO.updateQuantite(idLot, nouvelleQuantite);
            logger.info("Quantite du lot {} mise a jour: {}", idLot, nouvelleQuantite);
        } catch (DAOException e) {
            logger.error("Erreur lors de la mise a jour de la quantite", e);
            throw new ServiceException("Erreur lors de la mise a jour de la quantite", e);
        }
    }

    /**
     * Retourne les lots d'un medicament tries par date de peremption (FEFO).
     *
     * @param idMedicament l'ID du medicament
     * @return les lots tries par date de peremption croissante
     * @throws ServiceException si une erreur survient
     */
    public List<Lot> getLotsFEFO(int idMedicament) throws ServiceException {
        try {
            return lotDAO.findByMedicamentIdSortedByExpiration(idMedicament);
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des lots FEFO", e);
            throw new ServiceException("Erreur lors de la recuperation des lots", e);
        }
    }

    /**
     * Retourne les lots vendables d'un medicament (non perimes, avec stock).
     *
     * @param idMedicament l'ID du medicament
     * @return les lots vendables
     * @throws ServiceException si une erreur survient
     */
    public List<Lot> getLotsVendables(int idMedicament) throws ServiceException {
        try {
            return lotDAO.findVendableByMedicament(idMedicament);
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des lots vendables", e);
            throw new ServiceException("Erreur lors de la recuperation des lots vendables", e);
        }
    }

    /**
     * Verifie si un medicament est en stock.
     *
     * @param idMedicament l'ID du medicament
     * @return true si le stock est > 0
     * @throws ServiceException si une erreur survient
     */
    public boolean isEnStock(int idMedicament) throws ServiceException {
        return getStockVendable(idMedicament) > 0;
    }

    /**
     * Verifie si un medicament a suffisamment de stock pour une quantite donnee.
     *
     * @param idMedicament l'ID du medicament
     * @param quantite     la quantite requise
     * @return true si le stock est suffisant
     * @throws ServiceException si une erreur survient
     */
    public boolean hasStockSuffisant(int idMedicament, int quantite) throws ServiceException {
        return getStockVendable(idMedicament) >= quantite;
    }

    /**
     * Valide les donnees d'un lot avant sauvegarde.
     *
     * @param lot le lot a valider
     * @throws ServiceException si les donnees sont invalides
     */
    private void validateLot(Lot lot) throws ServiceException {
        if (lot.getIdMedicament() == null || lot.getIdMedicament() <= 0) {
            throw new ServiceException("L'ID du medicament est requis", ErrorType.VALIDATION);
        }
        if (lot.getDatePeremption() == null) {
            throw new ServiceException("La date de peremption est requise", ErrorType.VALIDATION);
        }
        if (lot.getQuantiteStock() < 0) {
            throw new ServiceException("La quantite ne peut pas etre negative", ErrorType.VALIDATION);
        }
    }
}
