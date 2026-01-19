package com.sgpa.service;

import com.sgpa.dao.ConsommationDAO;
import com.sgpa.dao.ConsommationDAO.StatConsommation;
import com.sgpa.dao.impl.ConsommationDAOImpl;
import com.sgpa.dto.PredictionReapprovisionnement;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de predictions de reapprovisionnement.
 * <p>
 * Analyse l'historique des ventes pour predire les ruptures de stock
 * et suggerer des quantites a commander.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class PredictionService {

    private static final Logger logger = LoggerFactory.getLogger(PredictionService.class);

    private final ConsommationDAO consommationDAO;
    private final ConfigService configService;

    /**
     * Constructeur par defaut.
     */
    public PredictionService() {
        this.consommationDAO = new ConsommationDAOImpl();
        this.configService = new ConfigService();
    }

    /**
     * Constructeur avec injection des dependances (pour tests).
     */
    public PredictionService(ConsommationDAO consommationDAO, ConfigService configService) {
        this.consommationDAO = consommationDAO;
        this.configService = configService;
    }

    /**
     * Genere les predictions pour tous les medicaments actifs.
     *
     * @return la liste des predictions de reapprovisionnement
     * @throws ServiceException si une erreur survient
     */
    public List<PredictionReapprovisionnement> genererPredictions() throws ServiceException {
        return genererPredictions(configService.getPredictionJoursAnalyse());
    }

    /**
     * Genere les predictions pour tous les medicaments avec une periode d'analyse specifique.
     *
     * @param nbJoursAnalyse le nombre de jours d'historique a analyser
     * @return la liste des predictions de reapprovisionnement
     * @throws ServiceException si une erreur survient
     */
    public List<PredictionReapprovisionnement> genererPredictions(int nbJoursAnalyse) throws ServiceException {
        logger.info("Generation des predictions sur {} jours d'historique", nbJoursAnalyse);
        List<PredictionReapprovisionnement> predictions = new ArrayList<>();

        try {
            List<StatConsommation> stats = consommationDAO.getStatistiquesConsommation(nbJoursAnalyse);

            for (StatConsommation stat : stats) {
                PredictionReapprovisionnement prediction = creerPrediction(stat);
                predictions.add(prediction);
            }

            logger.info("{} predictions generees", predictions.size());
            return predictions;

        } catch (DAOException e) {
            logger.error("Erreur lors de la generation des predictions", e);
            throw new ServiceException("Erreur lors de la generation des predictions", e);
        }
    }

    /**
     * Genere la prediction pour un medicament specifique.
     *
     * @param idMedicament l'identifiant du medicament
     * @return la prediction de reapprovisionnement
     * @throws ServiceException si une erreur survient
     */
    public PredictionReapprovisionnement genererPrediction(int idMedicament) throws ServiceException {
        return genererPrediction(idMedicament, configService.getPredictionJoursAnalyse());
    }

    /**
     * Genere la prediction pour un medicament avec une periode d'analyse specifique.
     *
     * @param idMedicament   l'identifiant du medicament
     * @param nbJoursAnalyse le nombre de jours d'historique a analyser
     * @return la prediction de reapprovisionnement
     * @throws ServiceException si une erreur survient
     */
    public PredictionReapprovisionnement genererPrediction(int idMedicament, int nbJoursAnalyse)
            throws ServiceException {
        logger.debug("Generation de la prediction pour medicament {} sur {} jours", idMedicament, nbJoursAnalyse);

        try {
            List<StatConsommation> stats = consommationDAO.getStatistiquesConsommation(nbJoursAnalyse);

            for (StatConsommation stat : stats) {
                if (stat.getIdMedicament() == idMedicament) {
                    return creerPrediction(stat);
                }
            }

            throw new ServiceException("Medicament non trouve: " + idMedicament);

        } catch (DAOException e) {
            logger.error("Erreur lors de la generation de la prediction", e);
            throw new ServiceException("Erreur lors de la generation de la prediction", e);
        }
    }

    /**
     * Retourne les predictions critiques (rupture imminente).
     *
     * @return la liste des predictions critiques
     * @throws ServiceException si une erreur survient
     */
    public List<PredictionReapprovisionnement> getPredictionsCritiques() throws ServiceException {
        int seuilCritique = configService.getPredictionSeuilCritiqueJours();
        return genererPredictions().stream()
                .filter(p -> p.getJoursAvantRupture() <= seuilCritique)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les predictions urgentes.
     *
     * @return la liste des predictions urgentes
     * @throws ServiceException si une erreur survient
     */
    public List<PredictionReapprovisionnement> getPredictionsUrgentes() throws ServiceException {
        int seuilUrgent = configService.getPredictionSeuilUrgentJours();
        return genererPredictions().stream()
                .filter(p -> p.getJoursAvantRupture() <= seuilUrgent)
                .collect(Collectors.toList());
    }

    /**
     * Retourne les predictions necessitant attention.
     *
     * @return la liste des predictions a surveiller
     * @throws ServiceException si une erreur survient
     */
    public List<PredictionReapprovisionnement> getPredictionsAttention() throws ServiceException {
        return genererPredictions().stream()
                .filter(p -> !PredictionReapprovisionnement.NIVEAU_OK.equals(p.getNiveauUrgence()))
                .collect(Collectors.toList());
    }

    /**
     * Calcule le nombre de jours avant rupture de stock.
     *
     * @param stockActuel             le stock actuel
     * @param consommationJournaliere la consommation moyenne journaliere
     * @return le nombre de jours avant rupture
     */
    public int calculerJoursAvantRupture(int stockActuel, double consommationJournaliere) {
        if (consommationJournaliere <= 0) {
            return Integer.MAX_VALUE;
        }
        return (int) Math.floor(stockActuel / consommationJournaliere);
    }

    /**
     * Calcule la quantite optimale a commander.
     *
     * @param stockActuel             le stock actuel
     * @param consommationJournaliere la consommation moyenne journaliere
     * @param delaiLivraison          le delai de livraison en jours
     * @return la quantite suggeree a commander
     */
    public int calculerQuantiteSuggeree(int stockActuel, double consommationJournaliere, int delaiLivraison) {
        if (consommationJournaliere <= 0) {
            return 0;
        }

        int margeSecurite = configService.getPredictionMargeSecuriteJours();
        int stockCible = configService.getPredictionStockCibleJours();

        // Stock necessaire = consommation * (delai + marge + stock cible)
        int joursACouvrir = delaiLivraison + margeSecurite + stockCible;
        int stockNecessaire = (int) Math.ceil(consommationJournaliere * joursACouvrir);

        // Quantite a commander = stock necessaire - stock actuel
        int quantite = stockNecessaire - stockActuel;

        return Math.max(0, quantite);
    }

    /**
     * Retourne l'historique de consommation pour un medicament.
     *
     * @param idMedicament l'identifiant du medicament
     * @param nbJours      le nombre de jours d'historique
     * @return une map avec la date comme cle et la quantite vendue comme valeur
     * @throws ServiceException si une erreur survient
     */
    public Map<LocalDate, Integer> getHistoriqueConsommation(int idMedicament, int nbJours)
            throws ServiceException {
        try {
            return consommationDAO.getHistoriqueConsommation(idMedicament, nbJours);
        } catch (DAOException e) {
            logger.error("Erreur lors de la recuperation de l'historique", e);
            throw new ServiceException("Erreur lors de la recuperation de l'historique", e);
        }
    }

    /**
     * Determine le niveau d'urgence en fonction des jours avant rupture.
     *
     * @param joursAvantRupture le nombre de jours avant rupture
     * @return le niveau d'urgence (RUPTURE, CRITIQUE, URGENT, ATTENTION, OK)
     */
    public String determinerNiveauUrgence(int joursAvantRupture) {
        int seuilCritique = configService.getPredictionSeuilCritiqueJours();
        int seuilUrgent = configService.getPredictionSeuilUrgentJours();

        if (joursAvantRupture <= 0) {
            return PredictionReapprovisionnement.NIVEAU_RUPTURE;
        } else if (joursAvantRupture <= seuilCritique) {
            return PredictionReapprovisionnement.NIVEAU_CRITIQUE;
        } else if (joursAvantRupture <= seuilUrgent) {
            return PredictionReapprovisionnement.NIVEAU_URGENT;
        } else if (joursAvantRupture <= seuilUrgent * 2) {
            return PredictionReapprovisionnement.NIVEAU_ATTENTION;
        }
        return PredictionReapprovisionnement.NIVEAU_OK;
    }

    /**
     * Retourne le nombre total de predictions par niveau d'urgence.
     *
     * @return un tableau [rupture, critique, urgent, attention, ok]
     * @throws ServiceException si une erreur survient
     */
    public int[] getCompteurParUrgence() throws ServiceException {
        int[] compteurs = new int[5]; // rupture, critique, urgent, attention, ok
        List<PredictionReapprovisionnement> predictions = genererPredictions();

        for (PredictionReapprovisionnement p : predictions) {
            switch (p.getNiveauUrgence()) {
                case PredictionReapprovisionnement.NIVEAU_RUPTURE -> compteurs[0]++;
                case PredictionReapprovisionnement.NIVEAU_CRITIQUE -> compteurs[1]++;
                case PredictionReapprovisionnement.NIVEAU_URGENT -> compteurs[2]++;
                case PredictionReapprovisionnement.NIVEAU_ATTENTION -> compteurs[3]++;
                default -> compteurs[4]++;
            }
        }

        return compteurs;
    }

    /**
     * Cree une prediction a partir des statistiques de consommation.
     */
    private PredictionReapprovisionnement creerPrediction(StatConsommation stat) {
        PredictionReapprovisionnement prediction = new PredictionReapprovisionnement(
                stat.getIdMedicament(),
                stat.getNomMedicament(),
                stat.getStockTotal(),
                stat.getStockVendable(),
                stat.getConsommationMoyenneJour(),
                stat.getSeuilMin()
        );

        // Calculer la quantite suggeree
        int delaiLivraison = configService.getPredictionDelaiLivraisonDefaut();
        int quantiteSuggeree = calculerQuantiteSuggeree(
                stat.getStockVendable(),
                stat.getConsommationMoyenneJour(),
                delaiLivraison
        );
        prediction.setQuantiteSuggeree(quantiteSuggeree);

        // Mettre a jour le niveau d'urgence avec les seuils configurables
        prediction.setNiveauUrgence(determinerNiveauUrgence(prediction.getJoursAvantRupture()));

        return prediction;
    }
}
