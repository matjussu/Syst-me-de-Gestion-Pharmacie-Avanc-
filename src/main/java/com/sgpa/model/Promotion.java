package com.sgpa.model;

import com.sgpa.model.enums.TypePromotion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entite representant une promotion ou remise applicable aux medicaments.
 * <p>
 * Correspond a la table {@code promotions} de la base de donnees.
 * Les promotions peuvent etre de differents types : pourcentage, montant fixe,
 * offre groupee (N+M) ou prix special.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class Promotion {

    /** Identifiant unique de la promotion */
    private Integer idPromotion;

    /** Code promotionnel optionnel (pour saisie manuelle) */
    private String codePromo;

    /** Nom de la promotion */
    private String nom;

    /** Description detaillee */
    private String description;

    /** Type de promotion */
    private TypePromotion typePromotion;

    /** Valeur de la reduction (%, EUR ou prix special) */
    private BigDecimal valeur;

    /** Quantite requise pour offre groupee */
    private int quantiteRequise;

    /** Quantite offerte pour offre groupee */
    private int quantiteOfferte;

    /** Date de debut de validite */
    private LocalDate dateDebut;

    /** Date de fin de validite */
    private LocalDate dateFin;

    /** Indicateur si la promotion est active */
    private boolean actif;

    /** Si true, utilisable une seule fois par client */
    private boolean usageUnique;

    /** Si true, peut se cumuler avec d'autres promotions */
    private boolean cumulable;

    /** Date de creation */
    private LocalDateTime dateCreation;

    /** ID de l'utilisateur qui a cree la promotion */
    private Integer creePar;

    /** Liste des IDs de medicaments concernes par la promotion */
    private List<Integer> medicamentIds;

    /**
     * Constructeur par defaut.
     */
    public Promotion() {
        this.actif = true;
        this.quantiteRequise = 1;
        this.quantiteOfferte = 0;
        this.usageUnique = false;
        this.cumulable = false;
        this.medicamentIds = new ArrayList<>();
    }

    /**
     * Constructeur avec les champs obligatoires.
     *
     * @param nom           le nom de la promotion
     * @param typePromotion le type de promotion
     * @param valeur        la valeur de la reduction
     * @param dateDebut     la date de debut
     * @param dateFin       la date de fin
     */
    public Promotion(String nom, TypePromotion typePromotion, BigDecimal valeur,
                     LocalDate dateDebut, LocalDate dateFin) {
        this();
        this.nom = nom;
        this.typePromotion = typePromotion;
        this.valeur = valeur;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters et Setters

    public Integer getIdPromotion() {
        return idPromotion;
    }

    public void setIdPromotion(Integer idPromotion) {
        this.idPromotion = idPromotion;
    }

    public String getCodePromo() {
        return codePromo;
    }

    public void setCodePromo(String codePromo) {
        this.codePromo = codePromo;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TypePromotion getTypePromotion() {
        return typePromotion;
    }

    public void setTypePromotion(TypePromotion typePromotion) {
        this.typePromotion = typePromotion;
    }

    public BigDecimal getValeur() {
        return valeur;
    }

    public void setValeur(BigDecimal valeur) {
        this.valeur = valeur;
    }

    public int getQuantiteRequise() {
        return quantiteRequise;
    }

    public void setQuantiteRequise(int quantiteRequise) {
        this.quantiteRequise = quantiteRequise;
    }

    public int getQuantiteOfferte() {
        return quantiteOfferte;
    }

    public void setQuantiteOfferte(int quantiteOfferte) {
        this.quantiteOfferte = quantiteOfferte;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public boolean isUsageUnique() {
        return usageUnique;
    }

    public void setUsageUnique(boolean usageUnique) {
        this.usageUnique = usageUnique;
    }

    public boolean isCumulable() {
        return cumulable;
    }

    public void setCumulable(boolean cumulable) {
        this.cumulable = cumulable;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Integer getCreePar() {
        return creePar;
    }

    public void setCreePar(Integer creePar) {
        this.creePar = creePar;
    }

    public List<Integer> getMedicamentIds() {
        return medicamentIds;
    }

    public void setMedicamentIds(List<Integer> medicamentIds) {
        this.medicamentIds = medicamentIds != null ? medicamentIds : new ArrayList<>();
    }

    // Methodes metier

    /**
     * Verifie si la promotion est actuellement valide (active et dans les dates).
     *
     * @return true si la promotion est valide aujourd'hui
     */
    public boolean estValide() {
        if (!actif) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(dateDebut) && !today.isAfter(dateFin);
    }

    /**
     * Verifie si la promotion est expiree.
     *
     * @return true si la date de fin est passee
     */
    public boolean estExpiree() {
        return LocalDate.now().isAfter(dateFin);
    }

    /**
     * Verifie si la promotion n'a pas encore commence.
     *
     * @return true si la date de debut n'est pas encore atteinte
     */
    public boolean estFuture() {
        return LocalDate.now().isBefore(dateDebut);
    }

    /**
     * Verifie si un medicament est concerne par cette promotion.
     *
     * @param idMedicament l'ID du medicament a verifier
     * @return true si le medicament est dans la liste ou si la promotion s'applique a tous
     */
    public boolean concerneMedicament(Integer idMedicament) {
        // Si aucun medicament specifie, la promotion s'applique a tous
        if (medicamentIds == null || medicamentIds.isEmpty()) {
            return true;
        }
        return medicamentIds.contains(idMedicament);
    }

    /**
     * Retourne une description formatee de la promotion.
     *
     * @return la description formatee
     */
    public String getDescriptionFormatee() {
        return switch (typePromotion) {
            case POURCENTAGE -> String.format("-%.0f%%", valeur);
            case MONTANT_FIXE -> String.format("-%.2f EUR", valeur);
            case OFFRE_GROUPEE -> String.format("%d+%d gratuit", quantiteRequise, quantiteOfferte);
            case PRIX_SPECIAL -> String.format("Prix: %.2f EUR", valeur);
        };
    }

    /**
     * Retourne le statut textuel de la promotion.
     *
     * @return le statut (Active, Expiree, Future, Inactive)
     */
    public String getStatut() {
        if (!actif) return "Inactive";
        if (estExpiree()) return "Expiree";
        if (estFuture()) return "Future";
        return "Active";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Promotion promotion = (Promotion) o;
        return Objects.equals(idPromotion, promotion.idPromotion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPromotion);
    }

    @Override
    public String toString() {
        return "Promotion{" +
               "idPromotion=" + idPromotion +
               ", nom='" + nom + '\'' +
               ", type=" + typePromotion +
               ", valeur=" + valeur +
               ", actif=" + actif +
               ", statut=" + getStatut() +
               '}';
    }
}
