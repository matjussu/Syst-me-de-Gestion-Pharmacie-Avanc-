package com.sgpa.dto;

/**
 * DTO representant une alerte de stock bas.
 * Utilise pour notifier quand un medicament est en dessous de son seuil minimum.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AlerteStock {

    private int idMedicament;
    private String nomMedicament;
    private int stockActuel;
    private int seuilMin;

    public AlerteStock() {
    }

    public AlerteStock(int idMedicament, String nomMedicament, int stockActuel, int seuilMin) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.stockActuel = stockActuel;
        this.seuilMin = seuilMin;
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

    public int getStockActuel() {
        return stockActuel;
    }

    public void setStockActuel(int stockActuel) {
        this.stockActuel = stockActuel;
    }

    public int getSeuilMin() {
        return seuilMin;
    }

    public void setSeuilMin(int seuilMin) {
        this.seuilMin = seuilMin;
    }

    /**
     * Calcule le deficit de stock.
     *
     * @return la quantite manquante pour atteindre le seuil
     */
    public int getDeficit() {
        return Math.max(0, seuilMin - stockActuel);
    }

    /**
     * Retourne le niveau de criticite (pourcentage du seuil).
     *
     * @return pourcentage de remplissage (0-100)
     */
    public int getNiveauCriticite() {
        if (seuilMin == 0) return 100;
        return (stockActuel * 100) / seuilMin;
    }

    @Override
    public String toString() {
        return String.format("ALERTE STOCK: %s - Stock: %d/%d (deficit: %d)",
                nomMedicament, stockActuel, seuilMin, getDeficit());
    }
}
