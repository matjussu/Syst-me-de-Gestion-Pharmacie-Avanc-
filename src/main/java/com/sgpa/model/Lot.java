package com.sgpa.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Entite representant un lot de medicaments avec sa date de peremption.
 * <p>
 * Correspond a la table {@code lots} de la base de donnees.
 * Un lot represente le stock physique d'un medicament avec une date de peremption specifique.
 * Cette separation permet la gestion FEFO (First Expired, First Out).
 * </p>
 * <p>
 * <b>Algorithme FEFO :</b> Lors d'une vente, les lots avec les dates de peremption
 * les plus proches sont utilises en premier.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 * @see Medicament
 */
public class Lot {

    /** Nombre de jours pour l'alerte de peremption proche */
    private static final int JOURS_ALERTE_PEREMPTION = 90;

    /** Identifiant unique du lot */
    private Integer idLot;

    /** Reference au medicament */
    private Integer idMedicament;

    /** Reference au fournisseur */
    private Integer idFournisseur;

    /** Numero de lot du fabricant (pour tracabilite) */
    private String numeroLot;

    /** Date de peremption (critique pour FEFO) */
    private LocalDate datePeremption;

    /** Date de fabrication */
    private LocalDate dateFabrication;

    /** Date de reception du lot */
    private LocalDateTime dateReception;

    /** Quantite en stock */
    private int quantiteStock;

    /** Prix d'achat unitaire */
    private BigDecimal prixAchat;

    /** Reference vers l'objet Medicament (pour jointures) */
    private Medicament medicament;

    /** Reference vers l'objet Fournisseur (pour jointures) */
    private Fournisseur fournisseur;

    /**
     * Constructeur par defaut.
     */
    public Lot() {
        this.quantiteStock = 0;
    }

    /**
     * Constructeur complet.
     *
     * @param idLot          l'identifiant unique
     * @param idMedicament   l'ID du medicament
     * @param idFournisseur  l'ID du fournisseur
     * @param numeroLot      le numero de lot fabricant
     * @param datePeremption la date de peremption
     * @param dateFabrication la date de fabrication
     * @param dateReception  la date de reception
     * @param quantiteStock  la quantite en stock
     * @param prixAchat      le prix d'achat unitaire
     */
    public Lot(Integer idLot, Integer idMedicament, Integer idFournisseur,
               String numeroLot, LocalDate datePeremption, LocalDate dateFabrication,
               LocalDateTime dateReception, int quantiteStock, BigDecimal prixAchat) {
        this.idLot = idLot;
        this.idMedicament = idMedicament;
        this.idFournisseur = idFournisseur;
        this.numeroLot = numeroLot;
        this.datePeremption = datePeremption;
        this.dateFabrication = dateFabrication;
        this.dateReception = dateReception;
        this.quantiteStock = quantiteStock;
        this.prixAchat = prixAchat;
    }

    /**
     * Constructeur pour creation d'un nouveau lot.
     *
     * @param idMedicament   l'ID du medicament
     * @param idFournisseur  l'ID du fournisseur
     * @param numeroLot      le numero de lot fabricant
     * @param datePeremption la date de peremption
     * @param quantiteStock  la quantite initiale
     * @param prixAchat      le prix d'achat unitaire
     */
    public Lot(Integer idMedicament, Integer idFournisseur, String numeroLot,
               LocalDate datePeremption, int quantiteStock, BigDecimal prixAchat) {
        this.idMedicament = idMedicament;
        this.idFournisseur = idFournisseur;
        this.numeroLot = numeroLot;
        this.datePeremption = datePeremption;
        this.quantiteStock = quantiteStock;
        this.prixAchat = prixAchat;
    }

    // Getters et Setters

    public Integer getIdLot() {
        return idLot;
    }

    public void setIdLot(Integer idLot) {
        this.idLot = idLot;
    }

    public Integer getIdMedicament() {
        return idMedicament;
    }

    public void setIdMedicament(Integer idMedicament) {
        this.idMedicament = idMedicament;
    }

    public Integer getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(Integer idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public String getNumeroLot() {
        return numeroLot;
    }

    public void setNumeroLot(String numeroLot) {
        this.numeroLot = numeroLot;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }

    public LocalDate getDateFabrication() {
        return dateFabrication;
    }

    public void setDateFabrication(LocalDate dateFabrication) {
        this.dateFabrication = dateFabrication;
    }

    public LocalDateTime getDateReception() {
        return dateReception;
    }

    public void setDateReception(LocalDateTime dateReception) {
        this.dateReception = dateReception;
    }

    public int getQuantiteStock() {
        return quantiteStock;
    }

    public void setQuantiteStock(int quantiteStock) {
        this.quantiteStock = quantiteStock;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }

    public Medicament getMedicament() {
        return medicament;
    }

    public void setMedicament(Medicament medicament) {
        this.medicament = medicament;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    // Methodes metier

    /**
     * Verifie si le lot est perime.
     *
     * @return true si la date de peremption est depassee
     */
    public boolean isPerime() {
        return datePeremption != null && datePeremption.isBefore(LocalDate.now());
    }

    /**
     * Verifie si le lot a une peremption proche (moins de 3 mois).
     *
     * @return true si la peremption est dans moins de 90 jours
     */
    public boolean isPeremptionProche() {
        if (datePeremption == null) return false;
        long joursRestants = ChronoUnit.DAYS.between(LocalDate.now(), datePeremption);
        return joursRestants >= 0 && joursRestants <= JOURS_ALERTE_PEREMPTION;
    }

    /**
     * Retourne le nombre de jours avant peremption.
     *
     * @return le nombre de jours (negatif si perime)
     */
    public long getJoursAvantPeremption() {
        if (datePeremption == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), datePeremption);
    }

    /**
     * Verifie si le lot a du stock disponible.
     *
     * @return true si le stock est superieur a 0
     */
    public boolean hasStock() {
        return quantiteStock > 0;
    }

    /**
     * Verifie si le lot est vendable (stock > 0 et non perime).
     *
     * @return true si le lot peut etre vendu
     */
    public boolean isVendable() {
        return hasStock() && !isPerime();
    }

    /**
     * Deduit une quantite du stock.
     *
     * @param quantite la quantite a deduire
     * @throws IllegalArgumentException si la quantite est superieure au stock
     */
    public void deduireStock(int quantite) {
        if (quantite > quantiteStock) {
            throw new IllegalArgumentException(
                    "Quantite demandee (" + quantite + ") superieure au stock disponible (" + quantiteStock + ")");
        }
        this.quantiteStock -= quantite;
    }

    /**
     * Ajoute une quantite au stock.
     *
     * @param quantite la quantite a ajouter
     * @throws IllegalArgumentException si la quantite est negative
     */
    public void ajouterStock(int quantite) {
        if (quantite < 0) {
            throw new IllegalArgumentException("La quantite a ajouter ne peut pas etre negative");
        }
        this.quantiteStock += quantite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lot lot = (Lot) o;
        return Objects.equals(idLot, lot.idLot) &&
               Objects.equals(numeroLot, lot.numeroLot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLot, numeroLot);
    }

    @Override
    public String toString() {
        return "Lot{" +
               "idLot=" + idLot +
               ", idMedicament=" + idMedicament +
               ", numeroLot='" + numeroLot + '\'' +
               ", datePeremption=" + datePeremption +
               ", quantiteStock=" + quantiteStock +
               ", perime=" + isPerime() +
               ", peremptionProche=" + isPeremptionProche() +
               '}';
    }
}
