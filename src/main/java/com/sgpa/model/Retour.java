package com.sgpa.model;

import java.time.LocalDateTime;

/**
 * Entite representant un retour de produit.
 * <p>
 * Un retour est effectue lorsqu'un client rapporte un medicament
 * precedemment achete. Le stock peut etre reintegre ou non
 * selon l'etat du produit et sa date de peremption.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class Retour {

    private Integer idRetour;
    private Integer idVente;
    private Integer idLot;
    private Integer idUtilisateur;
    private int quantite;
    private String motif;
    private LocalDateTime dateRetour;
    private boolean reintegre;
    private String commentaire;

    // Relations
    private Vente vente;
    private Lot lot;
    private Utilisateur utilisateur;

    /**
     * Constructeur par defaut.
     */
    public Retour() {
        this.dateRetour = LocalDateTime.now();
        this.reintegre = false;
    }

    /**
     * Constructeur avec parametres essentiels.
     *
     * @param idVente       l'identifiant de la vente d'origine
     * @param idLot         l'identifiant du lot retourne
     * @param idUtilisateur l'identifiant de l'utilisateur qui enregistre le retour
     * @param quantite      la quantite retournee
     * @param motif         le motif du retour
     */
    public Retour(Integer idVente, Integer idLot, Integer idUtilisateur, int quantite, String motif) {
        this();
        this.idVente = idVente;
        this.idLot = idLot;
        this.idUtilisateur = idUtilisateur;
        this.quantite = quantite;
        this.motif = motif;
    }

    // Getters et Setters

    public Integer getIdRetour() {
        return idRetour;
    }

    public void setIdRetour(Integer idRetour) {
        this.idRetour = idRetour;
    }

    public Integer getIdVente() {
        return idVente;
    }

    public void setIdVente(Integer idVente) {
        this.idVente = idVente;
    }

    public Integer getIdLot() {
        return idLot;
    }

    public void setIdLot(Integer idLot) {
        this.idLot = idLot;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public LocalDateTime getDateRetour() {
        return dateRetour;
    }

    public void setDateRetour(LocalDateTime dateRetour) {
        this.dateRetour = dateRetour;
    }

    public boolean isReintegre() {
        return reintegre;
    }

    public void setReintegre(boolean reintegre) {
        this.reintegre = reintegre;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public Vente getVente() {
        return vente;
    }

    public void setVente(Vente vente) {
        this.vente = vente;
        if (vente != null) {
            this.idVente = vente.getIdVente();
        }
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
        if (lot != null) {
            this.idLot = lot.getIdLot();
        }
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        if (utilisateur != null) {
            this.idUtilisateur = utilisateur.getIdUtilisateur();
        }
    }

    /**
     * Retourne le nom du medicament via le lot.
     *
     * @return le nom du medicament ou "Inconnu" si non disponible
     */
    public String getNomMedicament() {
        if (lot != null && lot.getMedicament() != null) {
            return lot.getMedicament().getNomCommercial();
        }
        return "Medicament #" + idLot;
    }

    /**
     * Retourne le numero du lot.
     *
     * @return le numero du lot ou "-" si non disponible
     */
    public String getNumeroLot() {
        return lot != null ? lot.getNumeroLot() : "-";
    }

    /**
     * Retourne le nom de l'utilisateur.
     *
     * @return le nom complet ou "Utilisateur #ID" si non disponible
     */
    public String getNomUtilisateur() {
        if (utilisateur != null) {
            return utilisateur.getNomComplet();
        }
        return "Utilisateur #" + idUtilisateur;
    }

    @Override
    public String toString() {
        return "Retour{" +
                "idRetour=" + idRetour +
                ", idVente=" + idVente +
                ", quantite=" + quantite +
                ", motif='" + motif + '\'' +
                ", reintegre=" + reintegre +
                '}';
    }
}
