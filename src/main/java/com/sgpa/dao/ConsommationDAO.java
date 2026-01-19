package com.sgpa.dao;

import com.sgpa.exception.DAOException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface DAO pour les calculs de consommation et statistiques de ventes.
 * <p>
 * Fournit les methodes necessaires pour analyser l'historique des ventes
 * et calculer les statistiques de consommation pour les predictions
 * de reapprovisionnement.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface ConsommationDAO {

    /**
     * Calcule la consommation totale d'un medicament sur une periode.
     *
     * @param idMedicament l'identifiant du medicament
     * @param dateDebut    la date de debut de la periode
     * @param dateFin      la date de fin de la periode
     * @return la quantite totale vendue sur la periode
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int getConsommationByPeriode(int idMedicament, LocalDate dateDebut, LocalDate dateFin) throws DAOException;

    /**
     * Retourne l'historique des ventes journalieres d'un medicament.
     *
     * @param idMedicament l'identifiant du medicament
     * @param nbJours      le nombre de jours d'historique a recuperer
     * @return une map avec la date comme cle et la quantite vendue comme valeur
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Map<LocalDate, Integer> getHistoriqueConsommation(int idMedicament, int nbJours) throws DAOException;

    /**
     * Calcule la consommation moyenne journaliere sur les N derniers jours.
     *
     * @param idMedicament l'identifiant du medicament
     * @param nbJours      le nombre de jours pour le calcul de la moyenne
     * @return la consommation moyenne journaliere
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    double getConsommationMoyenneJournaliere(int idMedicament, int nbJours) throws DAOException;

    /**
     * Retourne les statistiques de consommation pour tous les medicaments actifs.
     *
     * @param nbJours le nombre de jours pour le calcul des statistiques
     * @return la liste des statistiques par medicament
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<StatConsommation> getStatistiquesConsommation(int nbJours) throws DAOException;

    /**
     * Retourne le stock total (tous les lots) d'un medicament.
     *
     * @param idMedicament l'identifiant du medicament
     * @return le stock total
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int getStockTotal(int idMedicament) throws DAOException;

    /**
     * Retourne le stock vendable (non perime) d'un medicament.
     *
     * @param idMedicament l'identifiant du medicament
     * @return le stock vendable
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int getStockVendable(int idMedicament) throws DAOException;

    /**
     * Classe interne pour les statistiques de consommation d'un medicament.
     */
    class StatConsommation {
        private int idMedicament;
        private String nomMedicament;
        private int seuilMin;
        private int stockTotal;
        private int stockVendable;
        private int consommationPeriode;
        private double consommationMoyenneJour;

        public StatConsommation() {
        }

        public StatConsommation(int idMedicament, String nomMedicament, int seuilMin,
                                int stockTotal, int stockVendable, int consommationPeriode,
                                double consommationMoyenneJour) {
            this.idMedicament = idMedicament;
            this.nomMedicament = nomMedicament;
            this.seuilMin = seuilMin;
            this.stockTotal = stockTotal;
            this.stockVendable = stockVendable;
            this.consommationPeriode = consommationPeriode;
            this.consommationMoyenneJour = consommationMoyenneJour;
        }

        public int getIdMedicament() {
            return idMedicament;
        }

        public void setIdMedicament(int idMedicament) {
            this.idMedicament = idMedicament;
        }

        public String getNomMedicament() {
            return nomMedicament;
        }

        public void setNomMedicament(String nomMedicament) {
            this.nomMedicament = nomMedicament;
        }

        public int getSeuilMin() {
            return seuilMin;
        }

        public void setSeuilMin(int seuilMin) {
            this.seuilMin = seuilMin;
        }

        public int getStockTotal() {
            return stockTotal;
        }

        public void setStockTotal(int stockTotal) {
            this.stockTotal = stockTotal;
        }

        public int getStockVendable() {
            return stockVendable;
        }

        public void setStockVendable(int stockVendable) {
            this.stockVendable = stockVendable;
        }

        public int getConsommationPeriode() {
            return consommationPeriode;
        }

        public void setConsommationPeriode(int consommationPeriode) {
            this.consommationPeriode = consommationPeriode;
        }

        public double getConsommationMoyenneJour() {
            return consommationMoyenneJour;
        }

        public void setConsommationMoyenneJour(double consommationMoyenneJour) {
            this.consommationMoyenneJour = consommationMoyenneJour;
        }
    }
}
