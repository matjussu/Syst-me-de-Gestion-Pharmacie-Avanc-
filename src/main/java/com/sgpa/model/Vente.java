package com.sgpa.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entite representant une vente (en-tete).
 * <p>
 * Correspond a la table {@code ventes} de la base de donnees.
 * Une vente contient une ou plusieurs lignes de vente ({@link LigneVente}).
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 * @see LigneVente
 */
public class Vente {

    /** Identifiant unique de la vente */
    private Integer idVente;

    /** Date et heure de la vente */
    private LocalDateTime dateVente;

    /** Montant total TTC (BigDecimal pour precision) */
    private BigDecimal montantTotal;

    /** Indique si la vente est sur ordonnance */
    private boolean estSurOrdonnance;

    /** Numero d'ordonnance (si applicable) */
    private String numeroOrdonnance;

    /** Reference a l'utilisateur qui a effectue la vente */
    private Integer idUtilisateur;

    /** Notes complementaires */
    private String notes;

    /** Liste des lignes de vente */
    private List<LigneVente> lignesVente;

    /** Reference vers l'utilisateur (pour jointures) */
    private Utilisateur utilisateur;

    /** Cache pour le nombre d'articles (rempli par les requetes avec sous-requete SQL) */
    private int nombreArticlesCache = -1;

    /**
     * Constructeur par defaut.
     */
    public Vente() {
        this.lignesVente = new ArrayList<>();
        this.montantTotal = BigDecimal.ZERO;
        this.estSurOrdonnance = false;
    }

    /**
     * Constructeur complet.
     *
     * @param idVente          l'identifiant unique
     * @param dateVente        la date de vente
     * @param montantTotal     le montant total
     * @param estSurOrdonnance si vente sur ordonnance
     * @param numeroOrdonnance le numero d'ordonnance
     * @param idUtilisateur    l'ID de l'utilisateur
     * @param notes            les notes
     */
    public Vente(Integer idVente, LocalDateTime dateVente, BigDecimal montantTotal,
                 boolean estSurOrdonnance, String numeroOrdonnance, Integer idUtilisateur, String notes) {
        this.idVente = idVente;
        this.dateVente = dateVente;
        this.montantTotal = montantTotal;
        this.estSurOrdonnance = estSurOrdonnance;
        this.numeroOrdonnance = numeroOrdonnance;
        this.idUtilisateur = idUtilisateur;
        this.notes = notes;
        this.lignesVente = new ArrayList<>();
    }

    /**
     * Constructeur pour nouvelle vente.
     *
     * @param idUtilisateur    l'ID de l'utilisateur
     * @param estSurOrdonnance si vente sur ordonnance
     */
    public Vente(Integer idUtilisateur, boolean estSurOrdonnance) {
        this.idUtilisateur = idUtilisateur;
        this.estSurOrdonnance = estSurOrdonnance;
        this.dateVente = LocalDateTime.now();
        this.montantTotal = BigDecimal.ZERO;
        this.lignesVente = new ArrayList<>();
    }

    // Getters et Setters

    public Integer getIdVente() {
        return idVente;
    }

    public void setIdVente(Integer idVente) {
        this.idVente = idVente;
    }

    public LocalDateTime getDateVente() {
        return dateVente;
    }

    public void setDateVente(LocalDateTime dateVente) {
        this.dateVente = dateVente;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public boolean isEstSurOrdonnance() {
        return estSurOrdonnance;
    }

    public void setEstSurOrdonnance(boolean estSurOrdonnance) {
        this.estSurOrdonnance = estSurOrdonnance;
    }

    public String getNumeroOrdonnance() {
        return numeroOrdonnance;
    }

    public void setNumeroOrdonnance(String numeroOrdonnance) {
        this.numeroOrdonnance = numeroOrdonnance;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<LigneVente> getLignesVente() {
        return lignesVente;
    }

    public void setLignesVente(List<LigneVente> lignesVente) {
        this.lignesVente = lignesVente;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    // Methodes metier

    /**
     * Ajoute une ligne de vente.
     *
     * @param ligne la ligne a ajouter
     */
    public void addLigneVente(LigneVente ligne) {
        if (lignesVente == null) {
            lignesVente = new ArrayList<>();
        }
        lignesVente.add(ligne);
        recalculerMontantTotal();
    }

    /**
     * Supprime une ligne de vente.
     *
     * @param ligne la ligne a supprimer
     */
    public void removeLigneVente(LigneVente ligne) {
        if (lignesVente != null) {
            lignesVente.remove(ligne);
            recalculerMontantTotal();
        }
    }

    /**
     * Recalcule le montant total de la vente.
     */
    public void recalculerMontantTotal() {
        if (lignesVente == null || lignesVente.isEmpty()) {
            this.montantTotal = BigDecimal.ZERO;
            return;
        }
        this.montantTotal = lignesVente.stream()
                .map(LigneVente::getMontantLigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retourne le nombre d'articles vendus.
     *
     * @return le nombre total d'articles
     */
    public int getNombreArticles() {
        if (nombreArticlesCache >= 0) return nombreArticlesCache;
        if (lignesVente == null) return 0;
        return lignesVente.stream()
                .mapToInt(LigneVente::getQuantite)
                .sum();
    }

    public void setNombreArticlesCache(int nombreArticlesCache) {
        this.nombreArticlesCache = nombreArticlesCache;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vente vente = (Vente) o;
        return Objects.equals(idVente, vente.idVente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idVente);
    }

    @Override
    public String toString() {
        return "Vente{" +
               "idVente=" + idVente +
               ", dateVente=" + dateVente +
               ", montantTotal=" + montantTotal +
               ", estSurOrdonnance=" + estSurOrdonnance +
               ", nbLignes=" + (lignesVente != null ? lignesVente.size() : 0) +
               '}';
    }
}
