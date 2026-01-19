package com.sgpa.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entite representant un fournisseur de medicaments.
 * <p>
 * Correspond a la table {@code fournisseurs} de la base de donnees.
 * Les fournisseurs approvisionnent la pharmacie en medicaments via les commandes.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class Fournisseur {

    /** Identifiant unique du fournisseur */
    private Integer idFournisseur;

    /** Nom du fournisseur */
    private String nom;

    /** Nom du contact chez le fournisseur */
    private String contact;

    /** Adresse du fournisseur */
    private String adresse;

    /** Numero de telephone */
    private String telephone;

    /** Adresse email */
    private String email;

    /** Indicateur si le fournisseur est actif */
    private boolean actif;

    /** Date de creation de la fiche */
    private LocalDateTime dateCreation;

    /**
     * Constructeur par defaut.
     */
    public Fournisseur() {
        this.actif = true;
    }

    /**
     * Constructeur complet.
     *
     * @param idFournisseur l'identifiant unique
     * @param nom           le nom du fournisseur
     * @param contact       le nom du contact
     * @param adresse       l'adresse
     * @param telephone     le telephone
     * @param email         l'email
     * @param actif         si le fournisseur est actif
     * @param dateCreation  la date de creation
     */
    public Fournisseur(Integer idFournisseur, String nom, String contact, String adresse,
                       String telephone, String email, boolean actif, LocalDateTime dateCreation) {
        this.idFournisseur = idFournisseur;
        this.nom = nom;
        this.contact = contact;
        this.adresse = adresse;
        this.telephone = telephone;
        this.email = email;
        this.actif = actif;
        this.dateCreation = dateCreation;
    }

    /**
     * Constructeur pour creation d'un nouveau fournisseur.
     *
     * @param nom       le nom du fournisseur
     * @param contact   le nom du contact
     * @param telephone le telephone
     * @param email     l'email
     */
    public Fournisseur(String nom, String contact, String telephone, String email) {
        this.nom = nom;
        this.contact = contact;
        this.telephone = telephone;
        this.email = email;
        this.actif = true;
    }

    // Getters et Setters

    public Integer getIdFournisseur() {
        return idFournisseur;
    }

    public void setIdFournisseur(Integer idFournisseur) {
        this.idFournisseur = idFournisseur;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Fournisseur that = (Fournisseur) o;
        return Objects.equals(idFournisseur, that.idFournisseur) &&
               Objects.equals(nom, that.nom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idFournisseur, nom);
    }

    @Override
    public String toString() {
        return "Fournisseur{" +
               "idFournisseur=" + idFournisseur +
               ", nom='" + nom + '\'' +
               ", contact='" + contact + '\'' +
               ", telephone='" + telephone + '\'' +
               ", actif=" + actif +
               '}';
    }
}
