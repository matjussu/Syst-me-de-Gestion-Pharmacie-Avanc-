package com.sgpa.dto;

import java.time.LocalDate;

/**
 * DTO representant une prediction de reapprovisionnement pour un medicament.
 * Contient les informations de stock, consommation et suggestions de commande.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class PredictionReapprovisionnement {

    /** Niveaux d'urgence pour le reapprovisionnement */
    public static final String NIVEAU_RUPTURE = "RUPTURE";
    public static final String NIVEAU_CRITIQUE = "CRITIQUE";
    public static final String NIVEAU_URGENT = "URGENT";
    public static final String NIVEAU_ATTENTION = "ATTENTION";
    public static final String NIVEAU_OK = "OK";

    private int idMedicament;
    private String nomMedicament;
    private int stockActuel;
    private int stockVendable;
    private double consommationJournaliere;
    private double consommationHebdomadaire;
    private double consommationMensuelle;
    private int joursAvantRupture;
    private LocalDate dateRupturePrevue;
    private int quantiteSuggeree;
    private int seuilMin;
    private String niveauUrgence;

    public PredictionReapprovisionnement() {
    }

    public PredictionReapprovisionnement(int idMedicament, String nomMedicament, int stockActuel,
                                          int stockVendable, double consommationJournaliere,
                                          int seuilMin) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.stockActuel = stockActuel;
        this.stockVendable = stockVendable;
        this.consommationJournaliere = consommationJournaliere;
        this.seuilMin = seuilMin;

        // Calculer les autres valeurs
        this.consommationHebdomadaire = consommationJournaliere * 7;
        this.consommationMensuelle = consommationJournaliere * 30;
        calculerPredictions();
    }

    /**
     * Calcule les predictions basees sur la consommation journaliere.
     */
    private void calculerPredictions() {
        if (consommationJournaliere > 0) {
            this.joursAvantRupture = (int) Math.floor(stockVendable / consommationJournaliere);
            this.dateRupturePrevue = LocalDate.now().plusDays(joursAvantRupture);
        } else {
            this.joursAvantRupture = Integer.MAX_VALUE;
            this.dateRupturePrevue = null;
        }
        determinerNiveauUrgence();
    }

    /**
     * Determine le niveau d'urgence en fonction des jours avant rupture.
     */
    private void determinerNiveauUrgence() {
        if (joursAvantRupture <= 0) {
            this.niveauUrgence = NIVEAU_RUPTURE;
        } else if (joursAvantRupture <= 7) {
            this.niveauUrgence = NIVEAU_CRITIQUE;
        } else if (joursAvantRupture <= 14) {
            this.niveauUrgence = NIVEAU_URGENT;
        } else if (joursAvantRupture <= 28) {
            this.niveauUrgence = NIVEAU_ATTENTION;
        } else {
            this.niveauUrgence = NIVEAU_OK;
        }
    }

    // Getters et Setters

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

    public int getStockActuel() {
        return stockActuel;
    }

    public void setStockActuel(int stockActuel) {
        this.stockActuel = stockActuel;
    }

    public int getStockVendable() {
        return stockVendable;
    }

    public void setStockVendable(int stockVendable) {
        this.stockVendable = stockVendable;
    }

    public double getConsommationJournaliere() {
        return consommationJournaliere;
    }

    public void setConsommationJournaliere(double consommationJournaliere) {
        this.consommationJournaliere = consommationJournaliere;
        this.consommationHebdomadaire = consommationJournaliere * 7;
        this.consommationMensuelle = consommationJournaliere * 30;
        calculerPredictions();
    }

    public double getConsommationHebdomadaire() {
        return consommationHebdomadaire;
    }

    public double getConsommationMensuelle() {
        return consommationMensuelle;
    }

    public int getJoursAvantRupture() {
        return joursAvantRupture;
    }

    public void setJoursAvantRupture(int joursAvantRupture) {
        this.joursAvantRupture = joursAvantRupture;
        determinerNiveauUrgence();
    }

    public LocalDate getDateRupturePrevue() {
        return dateRupturePrevue;
    }

    public void setDateRupturePrevue(LocalDate dateRupturePrevue) {
        this.dateRupturePrevue = dateRupturePrevue;
    }

    public int getQuantiteSuggeree() {
        return quantiteSuggeree;
    }

    public void setQuantiteSuggeree(int quantiteSuggeree) {
        this.quantiteSuggeree = quantiteSuggeree;
    }

    public int getSeuilMin() {
        return seuilMin;
    }

    public void setSeuilMin(int seuilMin) {
        this.seuilMin = seuilMin;
    }

    public String getNiveauUrgence() {
        return niveauUrgence;
    }

    public void setNiveauUrgence(String niveauUrgence) {
        this.niveauUrgence = niveauUrgence;
    }

    // Methodes utilitaires

    /**
     * Verifie si le medicament est en rupture de stock.
     */
    public boolean isRupture() {
        return NIVEAU_RUPTURE.equals(niveauUrgence);
    }

    /**
     * Verifie si le reapprovisionnement est critique.
     */
    public boolean isCritique() {
        return NIVEAU_CRITIQUE.equals(niveauUrgence) || NIVEAU_RUPTURE.equals(niveauUrgence);
    }

    /**
     * Verifie si le reapprovisionnement est urgent.
     */
    public boolean isUrgent() {
        return NIVEAU_URGENT.equals(niveauUrgence) || isCritique();
    }

    /**
     * Retourne le deficit par rapport au seuil minimum.
     */
    public int getDeficit() {
        return Math.max(0, seuilMin - stockActuel);
    }

    /**
     * Retourne le pourcentage de stock par rapport au seuil minimum.
     */
    public int getPourcentageStock() {
        if (seuilMin == 0) return 100;
        return Math.min(100, (stockActuel * 100) / seuilMin);
    }

    /**
     * Retourne un ordre numerique pour le tri par urgence.
     * Plus le nombre est petit, plus c'est urgent.
     */
    public int getOrdreUrgence() {
        return switch (niveauUrgence) {
            case NIVEAU_RUPTURE -> 0;
            case NIVEAU_CRITIQUE -> 1;
            case NIVEAU_URGENT -> 2;
            case NIVEAU_ATTENTION -> 3;
            default -> 4;
        };
    }

    /**
     * Retourne une description formatee des jours restants.
     */
    public String getJoursRestantsFormate() {
        if (consommationJournaliere <= 0) {
            return "Aucune vente";
        } else if (joursAvantRupture <= 0) {
            return "RUPTURE";
        } else if (joursAvantRupture == 1) {
            return "1 jour";
        } else {
            return joursAvantRupture + " jours";
        }
    }

    @Override
    public String toString() {
        return String.format("Prediction[%s: stock=%d, conso/j=%.1f, rupture=%s, urgence=%s]",
                nomMedicament, stockActuel, consommationJournaliere,
                getJoursRestantsFormate(), niveauUrgence);
    }
}
