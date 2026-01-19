package com.sgpa.model;

import com.sgpa.model.enums.TypeAction;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entite representant une entree dans le journal d'audit.
 * <p>
 * Correspond a la table {@code audit_log} de la base de donnees.
 * Chaque action importante dans le systeme est enregistree dans ce journal
 * pour assurer la tracabilite et la conformite.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class AuditLog {

    /** Identifiant unique de l'entree d'audit */
    private Integer idAudit;

    /** Date et heure de l'action */
    private LocalDateTime dateAction;

    /** ID de l'utilisateur ayant effectue l'action */
    private Integer idUtilisateur;

    /** Nom de l'utilisateur (stocke pour historique meme si utilisateur supprime) */
    private String nomUtilisateur;

    /** Type d'action effectuee */
    private TypeAction typeAction;

    /** Type d'entite concernee (MEDICAMENT, LOT, UTILISATEUR, VENTE, etc.) */
    private String entite;

    /** ID de l'entite concernee */
    private Integer idEntite;

    /** Description detaillee de l'action */
    private String description;

    /** Details supplementaires en JSON */
    private String detailsJson;

    /** Adresse IP de l'utilisateur */
    private String adresseIp;

    /**
     * Constructeur par defaut.
     */
    public AuditLog() {
        this.dateAction = LocalDateTime.now();
    }

    /**
     * Constructeur avec parametres essentiels.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @param nomUtilisateur le nom de l'utilisateur
     * @param typeAction    le type d'action
     * @param entite        le type d'entite concernee
     * @param description   la description de l'action
     */
    public AuditLog(Integer idUtilisateur, String nomUtilisateur, TypeAction typeAction,
                    String entite, String description) {
        this.dateAction = LocalDateTime.now();
        this.idUtilisateur = idUtilisateur;
        this.nomUtilisateur = nomUtilisateur;
        this.typeAction = typeAction;
        this.entite = entite;
        this.description = description;
    }

    /**
     * Constructeur complet.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @param nomUtilisateur le nom de l'utilisateur
     * @param typeAction    le type d'action
     * @param entite        le type d'entite concernee
     * @param idEntite      l'ID de l'entite concernee
     * @param description   la description de l'action
     */
    public AuditLog(Integer idUtilisateur, String nomUtilisateur, TypeAction typeAction,
                    String entite, Integer idEntite, String description) {
        this(idUtilisateur, nomUtilisateur, typeAction, entite, description);
        this.idEntite = idEntite;
    }

    // Getters et Setters

    public Integer getIdAudit() {
        return idAudit;
    }

    public void setIdAudit(Integer idAudit) {
        this.idAudit = idAudit;
    }

    public LocalDateTime getDateAction() {
        return dateAction;
    }

    public void setDateAction(LocalDateTime dateAction) {
        this.dateAction = dateAction;
    }

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

    public TypeAction getTypeAction() {
        return typeAction;
    }

    public void setTypeAction(TypeAction typeAction) {
        this.typeAction = typeAction;
    }

    public String getEntite() {
        return entite;
    }

    public void setEntite(String entite) {
        this.entite = entite;
    }

    public Integer getIdEntite() {
        return idEntite;
    }

    public void setIdEntite(Integer idEntite) {
        this.idEntite = idEntite;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public void setDetailsJson(String detailsJson) {
        this.detailsJson = detailsJson;
    }

    public String getAdresseIp() {
        return adresseIp;
    }

    public void setAdresseIp(String adresseIp) {
        this.adresseIp = adresseIp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuditLog auditLog = (AuditLog) o;
        return Objects.equals(idAudit, auditLog.idAudit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAudit);
    }

    @Override
    public String toString() {
        return "AuditLog{" +
               "idAudit=" + idAudit +
               ", dateAction=" + dateAction +
               ", nomUtilisateur='" + nomUtilisateur + '\'' +
               ", typeAction=" + typeAction +
               ", entite='" + entite + '\'' +
               ", description='" + description + '\'' +
               '}';
    }
}
