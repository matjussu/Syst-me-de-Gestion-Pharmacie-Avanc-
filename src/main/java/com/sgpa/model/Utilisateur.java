package com.sgpa.model;

import com.sgpa.model.enums.Role;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entite representant un utilisateur du systeme SGPA.
 * <p>
 * Correspond a la table {@code utilisateurs} de la base de donnees.
 * Les utilisateurs peuvent avoir deux roles : PHARMACIEN (admin) ou PREPARATEUR (ventes).
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class Utilisateur {

    /** Identifiant unique de l'utilisateur */
    private Integer idUtilisateur;

    /** Nom d'utilisateur pour la connexion (unique) */
    private String nomUtilisateur;

    /** Mot de passe hashe (BCrypt) */
    private String motDePasse;

    /** Role de l'utilisateur */
    private Role role;

    /** Nom complet de l'utilisateur */
    private String nomComplet;

    /** Indicateur si le compte est actif */
    private boolean actif;

    /** Date de creation du compte */
    private LocalDateTime dateCreation;

    /** Date de derniere connexion */
    private LocalDateTime derniereConnexion;

    /**
     * Constructeur par defaut.
     */
    public Utilisateur() {
        this.actif = true;
    }

    /**
     * Constructeur complet.
     *
     * @param idUtilisateur     l'identifiant unique
     * @param nomUtilisateur    le nom d'utilisateur
     * @param motDePasse        le mot de passe hashe
     * @param role              le role
     * @param nomComplet        le nom complet
     * @param actif             si le compte est actif
     * @param dateCreation      la date de creation
     * @param derniereConnexion la derniere connexion
     */
    public Utilisateur(Integer idUtilisateur, String nomUtilisateur, String motDePasse,
                       Role role, String nomComplet, boolean actif,
                       LocalDateTime dateCreation, LocalDateTime derniereConnexion) {
        this.idUtilisateur = idUtilisateur;
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
        this.role = role;
        this.nomComplet = nomComplet;
        this.actif = actif;
        this.dateCreation = dateCreation;
        this.derniereConnexion = derniereConnexion;
    }

    /**
     * Constructeur pour creation d'un nouvel utilisateur.
     *
     * @param nomUtilisateur le nom d'utilisateur
     * @param motDePasse     le mot de passe hashe
     * @param role           le role
     * @param nomComplet     le nom complet
     */
    public Utilisateur(String nomUtilisateur, String motDePasse, Role role, String nomComplet) {
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
        this.role = role;
        this.nomComplet = nomComplet;
        this.actif = true;
    }

    // Getters et Setters

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getNomUtilisateur() {
        return nomUtilisateur;
    }

    public void setNomUtilisateur(String nomUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getNomComplet() {
        return nomComplet;
    }

    public void setNomComplet(String nomComplet) {
        this.nomComplet = nomComplet;
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

    public LocalDateTime getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    /**
     * Verifie si l'utilisateur a les droits d'administration.
     *
     * @return true si l'utilisateur est pharmacien
     */
    public boolean isAdmin() {
        return role != null && role.isAdmin();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Utilisateur that = (Utilisateur) o;
        return Objects.equals(idUtilisateur, that.idUtilisateur) &&
               Objects.equals(nomUtilisateur, that.nomUtilisateur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUtilisateur, nomUtilisateur);
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
               "idUtilisateur=" + idUtilisateur +
               ", nomUtilisateur='" + nomUtilisateur + '\'' +
               ", role=" + role +
               ", nomComplet='" + nomComplet + '\'' +
               ", actif=" + actif +
               '}';
    }
}
