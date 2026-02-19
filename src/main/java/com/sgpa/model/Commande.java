package com.sgpa.model;

import com.sgpa.model.enums.StatutCommande;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entite representant une commande fournisseur (en-tete).
 * <p>
 * Correspond a la table {@code commandes} de la base de donnees.
 * Une commande contient une ou plusieurs lignes de commande ({@link LigneCommande}).
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 * @see LigneCommande
 * @see StatutCommande
 */
public class Commande {

    /** Identifiant unique de la commande */
    private Integer idCommande;

    /** Date de creation de la commande */
    private LocalDateTime dateCreation;

    /** Date de reception de la commande */
    private LocalDateTime dateReception;

    /** Statut de la commande */
    private StatutCommande statut;

    /** Reference au fournisseur */
    private Integer idFournisseur;

    /** Notes complementaires */
    private String notes;

    /** Liste des lignes de commande */
    private List<LigneCommande> lignesCommande;

    /** Reference vers le fournisseur (pour jointures) */
    private Fournisseur fournisseur;

    /** Nombre total d'articles (pre-calcule par SQL, non persiste) */
    private int nbArticlesTotal = -1;

    /**
     * Constructeur par defaut.
     */
    public Commande() {
        this.lignesCommande = new ArrayList<>();
        this.statut = StatutCommande.EN_ATTENTE;
        this.dateCreation = LocalDateTime.now();
    }

    /**
     * Constructeur complet.
     *
     * @param idCommande    l'identifiant unique
     * @param dateCreation  la date de creation
     * @param dateReception la date de reception
     * @param statut        le statut
     * @param idFournisseur l'ID du fournisseur
     * @param notes         les notes
     */
    public Commande(Integer idCommande, LocalDateTime dateCreation, LocalDateTime dateReception,
                    StatutCommande statut, Integer idFournisseur, String notes) {
        this.idCommande = idCommande;
        this.dateCreation = dateCreation;
        this.dateReception = dateReception;
        this.statut = statut;
        this.idFournisseur = idFournisseur;
        this.notes = notes;
        this.lignesCommande = new ArrayList<>();
    }

    /**
     * Constructeur pour nouvelle commande.
     *
     * @param idFournisseur l'ID du fournisseur
     */
    public Commande(Integer idFournisseur) {
        this.idFournisseur = idFournisseur;
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutCommande.EN_ATTENTE;
        this.lignesCommande = new ArrayList<>();
    }

    // Getters et Setters

    public Integer getIdCommande() {
        return idCommande;
    }

    public void setIdCommande(Integer idCommande) {
        this.idCommande = idCommande;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateReception() {
        return dateReception;
    }

    public void setDateReception(LocalDateTime dateReception) {
        this.dateReception = dateReception;
    }

    public StatutCommande getStatut() {
        return statut;
    }

    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    public Integer getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(Integer idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<LigneCommande> getLignesCommande() {
        return lignesCommande;
    }

    public void setLignesCommande(List<LigneCommande> lignesCommande) {
        this.lignesCommande = lignesCommande;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }

    public int getNbArticlesTotal() {
        return nbArticlesTotal;
    }

    public void setNbArticlesTotal(int nbArticlesTotal) {
        this.nbArticlesTotal = nbArticlesTotal;
    }

    // Methodes metier

    /**
     * Ajoute une ligne de commande.
     *
     * @param ligne la ligne a ajouter
     */
    public void addLigneCommande(LigneCommande ligne) {
        if (lignesCommande == null) {
            lignesCommande = new ArrayList<>();
        }
        lignesCommande.add(ligne);
    }

    /**
     * Supprime une ligne de commande.
     *
     * @param ligne la ligne a supprimer
     */
    public void removeLigneCommande(LigneCommande ligne) {
        if (lignesCommande != null) {
            lignesCommande.remove(ligne);
        }
    }

    /**
     * Verifie si la commande peut etre modifiee.
     *
     * @return true si la commande est en attente
     */
    public boolean isModifiable() {
        return statut != null && statut.isModifiable();
    }

    /**
     * Verifie si la commande est finalisee.
     *
     * @return true si recue ou annulee
     */
    public boolean isFinalisee() {
        return statut != null && statut.isFinalisee();
    }

    /**
     * Marque la commande comme recue.
     */
    public void marquerRecue() {
        this.statut = StatutCommande.RECUE;
        this.dateReception = LocalDateTime.now();
    }

    /**
     * Annule la commande.
     */
    public void annuler() {
        this.statut = StatutCommande.ANNULEE;
    }

    /**
     * Retourne le nombre total d'articles commandes.
     *
     * @return le nombre total d'articles
     */
    public int getNombreArticlesCommandes() {
        if (lignesCommande != null && !lignesCommande.isEmpty()) {
            return lignesCommande.stream()
                    .mapToInt(LigneCommande::getQuantiteCommandee)
                    .sum();
        }
        return Math.max(nbArticlesTotal, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commande commande = (Commande) o;
        return Objects.equals(idCommande, commande.idCommande);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCommande);
    }

    @Override
    public String toString() {
        return "Commande{" +
               "idCommande=" + idCommande +
               ", dateCreation=" + dateCreation +
               ", statut=" + statut +
               ", idFournisseur=" + idFournisseur +
               ", nbLignes=" + (lignesCommande != null ? lignesCommande.size() : 0) +
               '}';
    }
}
