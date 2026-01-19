package com.sgpa.model.enums;

/**
 * Enumeration des statuts possibles pour une commande fournisseur.
 * <p>
 * Cycle de vie d'une commande :
 * <ol>
 *   <li>{@link #EN_ATTENTE} : Commande creee, en attente de reception</li>
 *   <li>{@link #RECUE} : Commande recue et stock mis a jour</li>
 *   <li>{@link #ANNULEE} : Commande annulee</li>
 * </ol>
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public enum StatutCommande {

    /**
     * Commande en attente de reception.
     */
    EN_ATTENTE("En attente", "Commande en cours de traitement"),

    /**
     * Commande recue - stock mis a jour.
     */
    RECUE("Recue", "Commande recue et enregistree"),

    /**
     * Commande annulee.
     */
    ANNULEE("Annulee", "Commande annulee");

    private final String libelle;
    private final String description;

    /**
     * Constructeur de l'enumeration StatutCommande.
     *
     * @param libelle     le libelle affichable du statut
     * @param description la description du statut
     */
    StatutCommande(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
    }

    /**
     * Retourne le libelle du statut.
     *
     * @return le libelle affichable
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Retourne la description du statut.
     *
     * @return la description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Verifie si la commande peut etre modifiee.
     *
     * @return true si le statut est EN_ATTENTE
     */
    public boolean isModifiable() {
        return this == EN_ATTENTE;
    }

    /**
     * Verifie si la commande est finalisee (recue ou annulee).
     *
     * @return true si le statut est RECUE ou ANNULEE
     */
    public boolean isFinalisee() {
        return this == RECUE || this == ANNULEE;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
