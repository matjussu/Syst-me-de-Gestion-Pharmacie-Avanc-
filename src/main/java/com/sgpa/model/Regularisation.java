package com.sgpa.model;

import com.sgpa.model.enums.MotifEcart;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entite representant une regularisation de stock suite a un inventaire.
 * <p>
 * Une regularisation enregistre l'ajustement effectue sur un lot
 * pour aligner le stock theorique avec le stock physique.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class Regularisation {

    private Integer idRegularisation;
    private Integer idSession;
    private Integer idLot;
    private int quantiteAncienne;
    private int quantiteNouvelle;
    private MotifEcart raison;
    private String justificatif;
    private LocalDateTime dateRegularisation;
    private Integer idUtilisateur;

    // Relations
    private SessionInventaire session;
    private Lot lot;
    private Utilisateur utilisateur;

    /**
     * Constructeur par defaut.
     */
    public Regularisation() {
        this.dateRegularisation = LocalDateTime.now();
    }

    /**
     * Constructeur avec parametres principaux.
     *
     * @param idSession         l'ID de la session
     * @param idLot             l'ID du lot
     * @param quantiteAncienne  la quantite avant regularisation
     * @param quantiteNouvelle  la quantite apres regularisation
     * @param raison            le motif de la regularisation
     * @param idUtilisateur     l'ID de l'utilisateur
     */
    public Regularisation(Integer idSession, Integer idLot, int quantiteAncienne,
                           int quantiteNouvelle, MotifEcart raison, Integer idUtilisateur) {
        this();
        this.idSession = idSession;
        this.idLot = idLot;
        this.quantiteAncienne = quantiteAncienne;
        this.quantiteNouvelle = quantiteNouvelle;
        this.raison = raison;
        this.idUtilisateur = idUtilisateur;
    }

    // Getters et Setters

    public Integer getIdRegularisation() {
        return idRegularisation;
    }

    public void setIdRegularisation(Integer idRegularisation) {
        this.idRegularisation = idRegularisation;
    }

    public Integer getIdSession() {
        return idSession;
    }

    public void setIdSession(Integer idSession) {
        this.idSession = idSession;
    }

    public Integer getIdLot() {
        return idLot;
    }

    public void setIdLot(Integer idLot) {
        this.idLot = idLot;
    }

    public int getQuantiteAncienne() {
        return quantiteAncienne;
    }

    public void setQuantiteAncienne(int quantiteAncienne) {
        this.quantiteAncienne = quantiteAncienne;
    }

    public int getQuantiteNouvelle() {
        return quantiteNouvelle;
    }

    public void setQuantiteNouvelle(int quantiteNouvelle) {
        this.quantiteNouvelle = quantiteNouvelle;
    }

    public MotifEcart getRaison() {
        return raison;
    }

    public void setRaison(MotifEcart raison) {
        this.raison = raison;
    }

    public String getJustificatif() {
        return justificatif;
    }

    public void setJustificatif(String justificatif) {
        this.justificatif = justificatif;
    }

    public LocalDateTime getDateRegularisation() {
        return dateRegularisation;
    }

    public void setDateRegularisation(LocalDateTime dateRegularisation) {
        this.dateRegularisation = dateRegularisation;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public SessionInventaire getSession() {
        return session;
    }

    public void setSession(SessionInventaire session) {
        this.session = session;
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    // Methodes utilitaires

    /**
     * Retourne la difference de quantite.
     *
     * @return la difference (nouvelle - ancienne)
     */
    public int getDifference() {
        return quantiteNouvelle - quantiteAncienne;
    }

    /**
     * Retourne le nom du medicament via le lot.
     *
     * @return le nom du medicament
     */
    public String getNomMedicament() {
        if (lot != null && lot.getMedicament() != null) {
            return lot.getMedicament().getNomCommercial();
        }
        return "Lot #" + idLot;
    }

    /**
     * Retourne le numero de lot.
     *
     * @return le numero de lot
     */
    public String getNumeroLot() {
        if (lot != null) {
            return lot.getNumeroLot();
        }
        return "-";
    }

    /**
     * Retourne le libelle de la raison.
     *
     * @return le libelle
     */
    public String getRaisonLibelle() {
        return raison != null ? raison.getLibelle() : "-";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Regularisation that = (Regularisation) o;
        return Objects.equals(idRegularisation, that.idRegularisation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRegularisation);
    }

    @Override
    public String toString() {
        return "Regularisation{" +
                "idRegularisation=" + idRegularisation +
                ", idLot=" + idLot +
                ", qteAncienne=" + quantiteAncienne +
                ", qteNouvelle=" + quantiteNouvelle +
                ", raison=" + raison +
                '}';
    }
}
