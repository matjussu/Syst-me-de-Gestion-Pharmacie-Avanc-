package com.sgpa.service;

import com.sgpa.dao.LotDAO;
import com.sgpa.dao.MedicamentDAO;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.dao.impl.MedicamentDAOImpl;
import com.sgpa.dto.AlertePeremption;
import com.sgpa.dto.AlerteStock;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des alertes.
 * <p>
 * Detecte et remonte les alertes concernant :
 * <ul>
 *   <li>Stock bas (en dessous du seuil minimum)</li>
 *   <li>Peremption proche (moins de 3 mois)</li>
 *   <li>Produits perimes</li>
 * </ul>
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AlerteService {

    private static final Logger logger = LoggerFactory.getLogger(AlerteService.class);

    /** Nombre de jours par defaut pour l'alerte de peremption (90 jours = 3 mois) */
    private static final int JOURS_ALERTE_PEREMPTION_DEFAUT = 90;

    private final MedicamentDAO medicamentDAO;
    private final LotDAO lotDAO;

    /**
     * Constructeur par defaut.
     */
    public AlerteService() {
        this.medicamentDAO = new MedicamentDAOImpl();
        this.lotDAO = new LotDAOImpl();
    }

    /**
     * Constructeur avec injection des DAOs (pour tests).
     *
     * @param medicamentDAO le DAO medicament
     * @param lotDAO        le DAO lot
     */
    public AlerteService(MedicamentDAO medicamentDAO, LotDAO lotDAO) {
        this.medicamentDAO = medicamentDAO;
        this.lotDAO = lotDAO;
    }

    /**
     * Recupere toutes les alertes de stock bas.
     * <p>
     * Un medicament est en stock bas si la somme des quantites de tous ses lots
     * est inferieure a son seuil minimum.
     * </p>
     *
     * @return la liste des alertes de stock bas
     * @throws ServiceException si une erreur survient
     */
    public List<AlerteStock> getAlertesStockBas() throws ServiceException {
        logger.debug("Recherche des alertes de stock bas");
        List<AlerteStock> alertes = new ArrayList<>();

        try {
            List<Medicament> medicaments = medicamentDAO.findBelowThreshold();

            for (Medicament med : medicaments) {
                int stockTotal = lotDAO.getTotalStockByMedicament(med.getIdMedicament());
                AlerteStock alerte = new AlerteStock(
                        med.getIdMedicament(),
                        med.getNomCommercial(),
                        stockTotal,
                        med.getSeuilMin()
                );
                alertes.add(alerte);
            }

            logger.info("{} alerte(s) de stock bas detectee(s)", alertes.size());
            return alertes;

        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des alertes de stock bas", e);
            throw new ServiceException("Erreur lors de la recuperation des alertes de stock", e);
        }
    }

    /**
     * Recupere les alertes de peremption proche.
     * <p>
     * Retourne les lots dont la date de peremption est dans moins de 3 mois (90 jours).
     * </p>
     *
     * @return la liste des alertes de peremption
     * @throws ServiceException si une erreur survient
     */
    public List<AlertePeremption> getAlertesPeremption() throws ServiceException {
        return getAlertesPeremption(JOURS_ALERTE_PEREMPTION_DEFAUT);
    }

    /**
     * Recupere les alertes de peremption proche avec un delai personnalise.
     *
     * @param joursAvant nombre de jours avant peremption pour l'alerte
     * @return la liste des alertes de peremption
     * @throws ServiceException si une erreur survient
     */
    public List<AlertePeremption> getAlertesPeremption(int joursAvant) throws ServiceException {
        logger.debug("Recherche des alertes de peremption (< {} jours)", joursAvant);
        List<AlertePeremption> alertes = new ArrayList<>();

        try {
            LocalDate dateLimite = LocalDate.now().plusDays(joursAvant);
            List<Lot> lots = lotDAO.findExpiringBefore(dateLimite);

            for (Lot lot : lots) {
                if (lot.getQuantiteStock() <= 0) continue;

                Medicament med = medicamentDAO.findById(lot.getIdMedicament())
                        .orElse(null);
                String nomMedicament = med != null ? med.getNomCommercial() : "Inconnu";

                long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), lot.getDatePeremption());

                AlertePeremption alerte = new AlertePeremption(
                        lot.getIdLot(),
                        lot.getNumeroLot(),
                        lot.getIdMedicament(),
                        nomMedicament,
                        lot.getDatePeremption(),
                        joursRestants,
                        lot.getQuantiteStock()
                );
                alertes.add(alerte);
            }

            logger.info("{} alerte(s) de peremption detectee(s)", alertes.size());
            return alertes;

        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des alertes de peremption", e);
            throw new ServiceException("Erreur lors de la recuperation des alertes de peremption", e);
        }
    }

    /**
     * Recupere la liste des lots perimes.
     *
     * @return la liste des lots perimes avec stock > 0
     * @throws ServiceException si une erreur survient
     */
    public List<Lot> getLotsPerimes() throws ServiceException {
        logger.debug("Recherche des lots perimes");

        try {
            List<Lot> lotsPerimes = lotDAO.findExpired();
            logger.info("{} lot(s) perime(s) detecte(s)", lotsPerimes.size());
            return lotsPerimes;

        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation des lots perimes", e);
            throw new ServiceException("Erreur lors de la recuperation des lots perimes", e);
        }
    }

    /**
     * Retourne le nombre total d'alertes actives.
     * <p>
     * Utile pour afficher un badge sur le dashboard.
     * </p>
     *
     * @return le nombre total d'alertes
     * @throws ServiceException si une erreur survient
     */
    public int getNombreAlertes() throws ServiceException {
        int nbStockBas = getAlertesStockBas().size();
        int nbPeremption = getAlertesPeremption().size();
        int nbPerimes = getLotsPerimes().size();

        int total = nbStockBas + nbPeremption + nbPerimes;
        logger.debug("Total alertes: {} (stock bas: {}, peremption: {}, perimes: {})",
                total, nbStockBas, nbPeremption, nbPerimes);

        return total;
    }

    /**
     * Recupere les alertes critiques (peremption < 30 jours ou perimes).
     *
     * @return la liste des alertes critiques
     * @throws ServiceException si une erreur survient
     */
    public List<AlertePeremption> getAlertesCritiques() throws ServiceException {
        List<AlertePeremption> alertes = getAlertesPeremption(30);
        // Filtrer pour ne garder que les critiques
        return alertes.stream()
                .filter(a -> a.getJoursRestants() <= 30)
                .toList();
    }

    /**
     * Genere un resume des alertes pour le dashboard.
     *
     * @return un resume textuel des alertes
     * @throws ServiceException si une erreur survient
     */
    public String getResumeAlertes() throws ServiceException {
        StringBuilder sb = new StringBuilder();

        List<AlerteStock> stockBas = getAlertesStockBas();
        List<AlertePeremption> peremption = getAlertesPeremption();
        List<Lot> perimes = getLotsPerimes();

        sb.append("=== RESUME DES ALERTES ===\n");

        if (!stockBas.isEmpty()) {
            sb.append("\nSTOCK BAS (").append(stockBas.size()).append("):\n");
            for (AlerteStock a : stockBas) {
                sb.append("  - ").append(a.toString()).append("\n");
            }
        }

        if (!perimes.isEmpty()) {
            sb.append("\nPRODUITS PERIMES (").append(perimes.size()).append("):\n");
            for (Lot l : perimes) {
                sb.append("  - Lot ").append(l.getNumeroLot())
                  .append(" (").append(l.getQuantiteStock()).append(" unites)\n");
            }
        }

        if (!peremption.isEmpty()) {
            sb.append("\nPEREMPTION PROCHE (").append(peremption.size()).append("):\n");
            for (AlertePeremption a : peremption) {
                if (!a.isPerime()) {
                    sb.append("  - ").append(a.toString()).append("\n");
                }
            }
        }

        if (stockBas.isEmpty() && peremption.isEmpty() && perimes.isEmpty()) {
            sb.append("\nAucune alerte active.\n");
        }

        return sb.toString();
    }
}
