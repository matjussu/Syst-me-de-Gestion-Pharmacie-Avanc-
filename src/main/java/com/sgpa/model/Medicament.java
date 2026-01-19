package com.sgpa.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entite representant un medicament (fiche produit).
 * <p>
 * Correspond a la table {@code medicaments} de la base de donnees.
 * Cette classe represente la fiche produit du medicament, sans le stock physique
 * qui est gere par la classe {@link Lot}.
 * </p>
 * <p>
 * <b>Important :</b> Le prix est stocke en {@link BigDecimal} pour eviter
 * les erreurs d'arrondi des types flottants.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 * @see Lot
 */
public class Medicament {

    /** Identifiant unique du medicament */
    private Integer idMedicament;

    /** Nom commercial du medicament */
    private String nomCommercial;

    /** Principe actif (molecule) */
    private String principeActif;

    /** Forme galenique (comprime, sirop, etc.) */
    private String formeGalenique;

    /** Dosage (ex: 500mg, 1g) */
    private String dosage;

    /** Prix public TTC (utiliser BigDecimal pour les montants) */
    private BigDecimal prixPublic;

    /** Indique si une ordonnance est necessaire */
    private boolean necessiteOrdonnance;

    /** Seuil minimum de stock pour alerte */
    private int seuilMin;

    /** Description du medicament */
    private String description;

    /** Indicateur si le medicament est actif dans le catalogue */
    private boolean actif;

    /** Date de creation de la fiche */
    private LocalDateTime dateCreation;

    /** Date de derniere modification */
    private LocalDateTime dateModification;

    /**
     * Constructeur par defaut.
     */
    public Medicament() {
        this.actif = true;
        this.seuilMin = 10;
        this.necessiteOrdonnance = false;
    }

    /**
     * Constructeur complet.
     *
     * @param idMedicament        l'identifiant unique
     * @param nomCommercial       le nom commercial
     * @param principeActif       le principe actif
     * @param formeGalenique      la forme galenique
     * @param dosage              le dosage
     * @param prixPublic          le prix public TTC
     * @param necessiteOrdonnance si ordonnance requise
     * @param seuilMin            le seuil minimum de stock
     * @param description         la description
     * @param actif               si le medicament est actif
     * @param dateCreation        la date de creation
     * @param dateModification    la date de modification
     */
    public Medicament(Integer idMedicament, String nomCommercial, String principeActif,
                      String formeGalenique, String dosage, BigDecimal prixPublic,
                      boolean necessiteOrdonnance, int seuilMin, String description,
                      boolean actif, LocalDateTime dateCreation, LocalDateTime dateModification) {
        this.idMedicament = idMedicament;
        this.nomCommercial = nomCommercial;
        this.principeActif = principeActif;
        this.formeGalenique = formeGalenique;
        this.dosage = dosage;
        this.prixPublic = prixPublic;
        this.necessiteOrdonnance = necessiteOrdonnance;
        this.seuilMin = seuilMin;
        this.description = description;
        this.actif = actif;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

    /**
     * Constructeur pour creation d'un nouveau medicament.
     *
     * @param nomCommercial       le nom commercial
     * @param principeActif       le principe actif
     * @param formeGalenique      la forme galenique
     * @param dosage              le dosage
     * @param prixPublic          le prix public TTC
     * @param necessiteOrdonnance si ordonnance requise
     * @param seuilMin            le seuil minimum de stock
     */
    public Medicament(String nomCommercial, String principeActif, String formeGalenique,
                      String dosage, BigDecimal prixPublic, boolean necessiteOrdonnance, int seuilMin) {
        this.nomCommercial = nomCommercial;
        this.principeActif = principeActif;
        this.formeGalenique = formeGalenique;
        this.dosage = dosage;
        this.prixPublic = prixPublic;
        this.necessiteOrdonnance = necessiteOrdonnance;
        this.seuilMin = seuilMin;
        this.actif = true;
    }

    // Getters et Setters

    public Integer getIdMedicament() {
        return idMedicament;
    }

    public void setIdMedicament(Integer idMedicament) {
        this.idMedicament = idMedicament;
    }

    public String getNomCommercial() {
        return nomCommercial;
    }

    public void setNomCommercial(String nomCommercial) {
        this.nomCommercial = nomCommercial;
    }

    public String getPrincipeActif() {
        return principeActif;
    }

    public void setPrincipeActif(String principeActif) {
        this.principeActif = principeActif;
    }

    public String getFormeGalenique() {
        return formeGalenique;
    }

    public void setFormeGalenique(String formeGalenique) {
        this.formeGalenique = formeGalenique;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public BigDecimal getPrixPublic() {
        return prixPublic;
    }

    public void setPrixPublic(BigDecimal prixPublic) {
        this.prixPublic = prixPublic;
    }

    public boolean isNecessiteOrdonnance() {
        return necessiteOrdonnance;
    }

    public void setNecessiteOrdonnance(boolean necessiteOrdonnance) {
        this.necessiteOrdonnance = necessiteOrdonnance;
    }

    public int getSeuilMin() {
        return seuilMin;
    }

    public void setSeuilMin(int seuilMin) {
        this.seuilMin = seuilMin;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }

    /**
     * Retourne le nom complet du medicament (nom + dosage + forme).
     *
     * @return le nom complet formate
     */
    public String getNomComplet() {
        StringBuilder sb = new StringBuilder(nomCommercial);
        if (dosage != null && !dosage.isEmpty()) {
            sb.append(" ").append(dosage);
        }
        if (formeGalenique != null && !formeGalenique.isEmpty()) {
            sb.append(" - ").append(formeGalenique);
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medicament that = (Medicament) o;
        return Objects.equals(idMedicament, that.idMedicament) &&
               Objects.equals(nomCommercial, that.nomCommercial);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idMedicament, nomCommercial);
    }

    @Override
    public String toString() {
        return "Medicament{" +
               "idMedicament=" + idMedicament +
               ", nomCommercial='" + nomCommercial + '\'' +
               ", principeActif='" + principeActif + '\'' +
               ", dosage='" + dosage + '\'' +
               ", prixPublic=" + prixPublic +
               ", necessiteOrdonnance=" + necessiteOrdonnance +
               ", seuilMin=" + seuilMin +
               '}';
    }
}
