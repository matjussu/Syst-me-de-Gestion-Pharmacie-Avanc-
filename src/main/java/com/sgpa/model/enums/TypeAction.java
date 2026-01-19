package com.sgpa.model.enums;

/**
 * Enumeration des types d'actions pour le journal d'audit.
 *
 * @author SGPA Team
 * @version 1.0
 */
public enum TypeAction {

    /** Connexion d'un utilisateur */
    CONNEXION("Connexion"),

    /** Deconnexion d'un utilisateur */
    DECONNEXION("Deconnexion"),

    /** Creation d'une entite */
    CREATION("Creation"),

    /** Modification d'une entite */
    MODIFICATION("Modification"),

    /** Suppression d'une entite */
    SUPPRESSION("Suppression"),

    /** Enregistrement d'une vente */
    VENTE("Vente"),

    /** Creation d'une commande fournisseur */
    COMMANDE("Commande"),

    /** Reception d'une commande */
    RECEPTION("Reception"),

    /** Action non categorisee */
    AUTRE("Autre");

    private final String libelle;

    TypeAction(String libelle) {
        this.libelle = libelle;
    }

    /**
     * Retourne le libelle du type d'action.
     *
     * @return le libelle
     */
    public String getLibelle() {
        return libelle;
    }
}
