package com.sgpa.dto;

import java.math.BigDecimal;

/**
 * DTO pour la creation d'une ligne de vente.
 * Utilise pour passer les informations d'une ligne de vente au service.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class LigneVenteDTO {

    private int idMedicament;
    private int quantite;
    private BigDecimal prixUnitaire;
    private Integer idPromotion;
    private String nomPromotion;
    private BigDecimal montantRemise = BigDecimal.ZERO;

    public LigneVenteDTO() {
        this.montantRemise = BigDecimal.ZERO;
    }

    public LigneVenteDTO(int idMedicament, int quantite) {
        this.idMedicament = idMedicament;
        this.quantite = quantite;
    }

    public LigneVenteDTO(int idMedicament, int quantite, BigDecimal prixUnitaire) {
        this.idMedicament = idMedicament;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public int getIdMedicament() {
        return idMedicament;
    }

    public void setIdMedicament(int idMedicament) {
        this.idMedicament = idMedicament;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Integer getIdPromotion() {
        return idPromotion;
    }

    public void setIdPromotion(Integer idPromotion) {
        this.idPromotion = idPromotion;
    }

    public String getNomPromotion() {
        return nomPromotion;
    }

    public void setNomPromotion(String nomPromotion) {
        this.nomPromotion = nomPromotion;
    }

    public BigDecimal getMontantRemise() {
        return montantRemise != null ? montantRemise : BigDecimal.ZERO;
    }

    public void setMontantRemise(BigDecimal montantRemise) {
        this.montantRemise = montantRemise != null ? montantRemise : BigDecimal.ZERO;
    }

    /**
     * Calcule le montant net apres remise.
     */
    public BigDecimal getMontantNet() {
        if (prixUnitaire == null) return BigDecimal.ZERO;
        BigDecimal brut = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        return brut.subtract(getMontantRemise());
    }

    /**
     * Verifie si une promotion est appliquee.
     */
    public boolean aPromotion() {
        return idPromotion != null && getMontantRemise().compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        return "LigneVenteDTO{" +
                "idMedicament=" + idMedicament +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", montantRemise=" + montantRemise +
                (nomPromotion != null ? ", promo=" + nomPromotion : "") +
                '}';
    }
}
