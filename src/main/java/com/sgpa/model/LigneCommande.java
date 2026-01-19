package com.sgpa.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entite representant une ligne de commande fournisseur.
 * <p>
 * Correspond a la table {@code ligne_commandes} de la base de donnees.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 * @see Commande
 */
public class LigneCommande {

    /** Identifiant unique de la ligne */
    private Integer idLigneCmd;

    /** Reference a la commande parente */
    private Integer idCommande;

    /** Reference au medicament commande */
    private Integer idMedicament;

    /** Quantite commandee */
    private int quantiteCommandee;

    /** Quantite effectivement recue */
    private int quantiteRecue;

    /** Prix unitaire negocie */
    private BigDecimal prixUnitaire;

    /** Reference vers le medicament (pour jointures) */
    private Medicament medicament;

    /**
     * Constructeur par defaut.
     */
    public LigneCommande() {
        this.quantiteRecue = 0;
    }

    /**
     * Constructeur complet.
     *
     * @param idLigneCmd        l'identifiant unique
     * @param idCommande        l'ID de la commande
     * @param idMedicament      l'ID du medicament
     * @param quantiteCommandee la quantite commandee
     * @param quantiteRecue     la quantite recue
     * @param prixUnitaire      le prix unitaire
     */
    public LigneCommande(Integer idLigneCmd, Integer idCommande, Integer idMedicament,
                         int quantiteCommandee, int quantiteRecue, BigDecimal prixUnitaire) {
        this.idLigneCmd = idLigneCmd;
        this.idCommande = idCommande;
        this.idMedicament = idMedicament;
        this.quantiteCommandee = quantiteCommandee;
        this.quantiteRecue = quantiteRecue;
        this.prixUnitaire = prixUnitaire;
    }

    /**
     * Constructeur pour nouvelle ligne de commande.
     *
     * @param idMedicament      l'ID du medicament
     * @param quantiteCommandee la quantite a commander
     * @param prixUnitaire      le prix unitaire negocie
     */
    public LigneCommande(Integer idMedicament, int quantiteCommandee, BigDecimal prixUnitaire) {
        this.idMedicament = idMedicament;
        this.quantiteCommandee = quantiteCommandee;
        this.prixUnitaire = prixUnitaire;
        this.quantiteRecue = 0;
    }

    // Getters et Setters

    public Integer getIdLigneCmd() {
        return idLigneCmd;
    }

    public void setIdLigneCmd(Integer idLigneCmd) {
        this.idLigneCmd = idLigneCmd;
    }

    public Integer getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(Integer idCommande) {
        this.idCommande = idCommande;
    }

    public Integer getIdMedicament() {
        return idMedicament;
    }

    public void setIdMedicament(Integer idMedicament) {
        this.idMedicament = idMedicament;
    }

    public int getQuantiteCommandee() {
        return quantiteCommandee;
    }

    public void setQuantiteCommandee(int quantiteCommandee) {
        this.quantiteCommandee = quantiteCommandee;
    }

    public int getQuantiteRecue() {
        return quantiteRecue;
    }

    public void setQuantiteRecue(int quantiteRecue) {
        this.quantiteRecue = quantiteRecue;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public Medicament getMedicament() {
        return medicament;
    }

    public void setMedicament(Medicament medicament) {
        this.medicament = medicament;
    }

    // Methodes metier

    /**
     * Calcule le montant de la ligne (quantite commandee x prix unitaire).
     *
     * @return le montant de la ligne
     */
    public BigDecimal getMontantLigne() {
        if (prixUnitaire == null) {
            return BigDecimal.ZERO;
        }
        return prixUnitaire.multiply(BigDecimal.valueOf(quantiteCommandee));
    }

    /**
     * Verifie si la ligne est completement recue.
     *
     * @return true si quantite recue >= quantite commandee
     */
    public boolean isCompletelyReceived() {
        return quantiteRecue >= quantiteCommandee;
    }

    /**
     * Retourne la quantite en attente de reception.
     *
     * @return la quantite restante a recevoir
     */
    public int getQuantiteEnAttente() {
        return Math.max(0, quantiteCommandee - quantiteRecue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LigneCommande that = (LigneCommande) o;
        return Objects.equals(idLigneCmd, that.idLigneCmd);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idLigneCmd);
    }

    @Override
    public String toString() {
        return "LigneCommande{" +
               "idLigneCmd=" + idLigneCmd +
               ", idCommande=" + idCommande +
               ", idMedicament=" + idMedicament +
               ", quantiteCommandee=" + quantiteCommandee +
               ", quantiteRecue=" + quantiteRecue +
               ", prixUnitaire=" + prixUnitaire +
               '}';
    }
}
