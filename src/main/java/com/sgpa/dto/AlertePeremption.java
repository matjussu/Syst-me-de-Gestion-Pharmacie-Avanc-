package com.sgpa.dto;

import java.time.LocalDate;

/**
 * DTO representant une alerte de peremption proche.
 * Utilise pour notifier quand un lot approche de sa date de peremption.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AlertePeremption {

    private int idLot;
    private String numeroLot;
    private int idMedicament;
    private String nomMedicament;
    private LocalDate datePeremption;
    private long joursRestants;
    private int quantiteStock;

    public AlertePeremption() {
    }

    public AlertePeremption(int idLot, String numeroLot, int idMedicament, String nomMedicament,
                            LocalDate datePeremption, long joursRestants, int quantiteStock) {
        this.idLot = idLot;
        this.numeroLot = numeroLot;
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.datePeremption = datePeremption;
        this.joursRestants = joursRestants;
        this.quantiteStock = quantiteStock;
    }

    public int getIdLot() {
        return idLot;
    }

    public void setIdLot(int idLot) {
        this.idLot = idLot;
    }

    public String getNumeroLot() {
        return numeroLot;
    }

    public void setNumeroLot(String numeroLot) {
        this.numeroLot = numeroLot;
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

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }

    public long getJoursRestants() {
        return joursRestants;
    }

    public void setJoursRestants(long joursRestants) {
        this.joursRestants = joursRestants;
    }

    public int getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(int quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    /**
     * Verifie si le lot est perime.
     *
     * @return true si le lot est deja perime
     */
    public boolean isPerime() {
        return joursRestants < 0;
    }

    /**
     * Verifie si le lot est critique (moins de 30 jours).
     *
     * @return true si moins de 30 jours avant peremption
     */
    public boolean isCritique() {
        return joursRestants >= 0 && joursRestants <= 30;
    }

    /**
     * Retourne le niveau d'urgence.
     *
     * @return "PERIME", "CRITIQUE", "URGENT" ou "ATTENTION"
     */
    public String getNiveauUrgence() {
        if (joursRestants < 0) return "PERIME";
        if (joursRestants <= 30) return "CRITIQUE";
        if (joursRestants <= 60) return "URGENT";
        return "ATTENTION";
    }

    @Override
    public String toString() {
        return String.format("ALERTE PEREMPTION [%s]: %s (Lot %s) - Expire le %s (%d jours) - %d unites",
                getNiveauUrgence(), nomMedicament, numeroLot, datePeremption, joursRestants, quantiteStock);
    }
}
