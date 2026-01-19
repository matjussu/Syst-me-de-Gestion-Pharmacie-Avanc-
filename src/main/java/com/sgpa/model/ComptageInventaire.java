package com.sgpa.model;

import com.sgpa.model.enums.MotifEcart;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entite representant un comptage physique lors d'un inventaire.
 * <p>
 * Chaque comptage enregistre la quantite theorique (stock systeme)
 * et la quantite physique (comptage reel), ainsi que l'ecart calcule.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ComptageInventaire {

    private Integer idComptage;
    private Integer idSession;
    private Integer idLot;
    private int quantiteTheorique;
    private int quantitePhysique;
    private int ecart;
    private MotifEcart motifEcart;
    private String commentaire;
    private LocalDateTime dateComptage;
    private Integer idUtilisateur;

    // Relations
    private SessionInventaire session;
    private Lot lot;
    private Utilisateur utilisateur;

    /**
     * Constructeur par defaut.
     */
    public ComptageInventaire() {
        this.dateComptage = LocalDateTime.now();
    }

    /**
     * Constructeur avec parametres principaux.
     *
     * @param idSession         l'ID de la session
     * @param idLot             l'ID du lot
     * @param quantiteTheorique la quantite theorique (stock systeme)
     * @param quantitePhysique  la quantite physique (comptage reel)
     * @param idUtilisateur     l'ID de l'utilisateur
     */
    public ComptageInventaire(Integer idSession, Integer idLot, int quantiteTheorique,
                               int quantitePhysique, Integer idUtilisateur) {
        this();
        this.idSession = idSession;
        this.idLot = idLot;
        this.quantiteTheorique = quantiteTheorique;
        this.quantitePhysique = quantitePhysique;
        this.ecart = quantitePhysique - quantiteTheorique;
        this.idUtilisateur = idUtilisateur;
    }

    // Getters et Setters

    public Integer getIdComptage() {
        return idComptage;
    }

    public void setIdComptage(Integer idComptage) {
        this.idComptage = idComptage;
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

    public int getQuantiteTheorique() {
        return quantiteTheorique;
    }

    public void setQuantiteTheorique(int quantiteTheorique) {
        this.quantiteTheorique = quantiteTheorique;
        recalculerEcart();
    }

    public int getQuantitePhysique() {
        return quantitePhysique;
    }

    public void setQuantitePhysique(int quantitePhysique) {
        this.quantitePhysique = quantitePhysique;
        recalculerEcart();
    }

    public int getEcart() {
        return ecart;
    }

    public void setEcart(int ecart) {
        this.ecart = ecart;
    }

    public MotifEcart getMotifEcart() {
        return motifEcart;
    }

    public void setMotifEcart(MotifEcart motifEcart) {
        this.motifEcart = motifEcart;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public LocalDateTime getDateComptage() {
        return dateComptage;
    }

    public void setDateComptage(LocalDateTime dateComptage) {
        this.dateComptage = dateComptage;
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
     * Recalcule l'ecart entre quantite physique et theorique.
     */
    private void recalculerEcart() {
        this.ecart = this.quantitePhysique - this.quantiteTheorique;
    }

    /**
     * Verifie s'il y a un ecart.
     *
     * @return true s'il y a un ecart
     */
    public boolean hasEcart() {
        return ecart != 0;
    }

    /**
     * Verifie si l'ecart est positif (surplus).
     *
     * @return true si surplus
     */
    public boolean isSurplus() {
        return ecart > 0;
    }

    /**
     * Verifie si l'ecart est negatif (manque).
     *
     * @return true si manque
     */
    public boolean isManque() {
        return ecart < 0;
    }

    /**
     * Retourne le pourcentage d'ecart.
     *
     * @return le pourcentage d'ecart
     */
    public double getEcartPourcentage() {
        if (quantiteTheorique == 0) {
            return ecart != 0 ? 100.0 : 0.0;
        }
        return (double) ecart / quantiteTheorique * 100.0;
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
     * Retourne le libelle du motif d'ecart.
     *
     * @return le libelle du motif
     */
    public String getMotifLibelle() {
        return motifEcart != null ? motifEcart.getLibelle() : "-";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComptageInventaire that = (ComptageInventaire) o;
        return Objects.equals(idComptage, that.idComptage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idComptage);
    }

    @Override
    public String toString() {
        return "ComptageInventaire{" +
                "idComptage=" + idComptage +
                ", idLot=" + idLot +
                ", qteTheorique=" + quantiteTheorique +
                ", qtePhysique=" + quantitePhysique +
                ", ecart=" + ecart +
                ", motif=" + motifEcart +
                '}';
    }
}
