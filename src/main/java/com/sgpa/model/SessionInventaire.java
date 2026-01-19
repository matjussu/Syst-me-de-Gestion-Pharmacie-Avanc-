package com.sgpa.model;

import com.sgpa.model.enums.StatutInventaire;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entite representant une session d'inventaire.
 * <p>
 * Une session d'inventaire permet de realiser un comptage physique
 * des stocks et de comparer avec les quantites theoriques.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class SessionInventaire {

    private Integer idSession;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private StatutInventaire statut;
    private Integer idUtilisateur;
    private String notes;

    // Relations
    private Utilisateur utilisateur;
    private List<ComptageInventaire> comptages;

    /**
     * Constructeur par defaut.
     */
    public SessionInventaire() {
        this.statut = StatutInventaire.EN_COURS;
        this.dateDebut = LocalDateTime.now();
        this.comptages = new ArrayList<>();
    }

    /**
     * Constructeur avec utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur qui cree la session
     * @param notes         les notes de la session
     */
    public SessionInventaire(Integer idUtilisateur, String notes) {
        this();
        this.idUtilisateur = idUtilisateur;
        this.notes = notes;
    }

    // Getters et Setters

    public Integer getIdSession() {
        return idSession;
    }

    public void setIdSession(Integer idSession) {
        this.idSession = idSession;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime dateFin) {
        this.dateFin = dateFin;
    }

    public StatutInventaire getStatut() {
        return statut;
    }

    public void setStatut(StatutInventaire statut) {
        this.statut = statut;
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

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public List<ComptageInventaire> getComptages() {
        return comptages;
    }

    public void setComptages(List<ComptageInventaire> comptages) {
        this.comptages = comptages;
    }

    // Methodes utilitaires

    /**
     * Verifie si la session est en cours.
     *
     * @return true si la session est en cours
     */
    public boolean isEnCours() {
        return statut == StatutInventaire.EN_COURS;
    }

    /**
     * Verifie si la session est terminee.
     *
     * @return true si la session est terminee
     */
    public boolean isTerminee() {
        return statut == StatutInventaire.TERMINEE;
    }

    /**
     * Retourne le nombre de comptages effectues.
     *
     * @return le nombre de comptages
     */
    public int getNombreComptages() {
        return comptages != null ? comptages.size() : 0;
    }

    /**
     * Retourne le nombre de comptages avec ecart.
     *
     * @return le nombre d'ecarts
     */
    public int getNombreEcarts() {
        if (comptages == null) return 0;
        return (int) comptages.stream()
                .filter(c -> c.getEcart() != 0)
                .count();
    }

    /**
     * Retourne le nom de l'utilisateur.
     *
     * @return le nom de l'utilisateur ou "Utilisateur #ID"
     */
    public String getNomUtilisateur() {
        if (utilisateur != null && utilisateur.getNomComplet() != null) {
            return utilisateur.getNomComplet();
        }
        return "Utilisateur #" + idUtilisateur;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionInventaire that = (SessionInventaire) o;
        return Objects.equals(idSession, that.idSession);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSession);
    }

    @Override
    public String toString() {
        return "SessionInventaire{" +
                "idSession=" + idSession +
                ", dateDebut=" + dateDebut +
                ", statut=" + statut +
                ", comptages=" + getNombreComptages() +
                ", ecarts=" + getNombreEcarts() +
                '}';
    }
}
