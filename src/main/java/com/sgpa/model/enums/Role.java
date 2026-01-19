package com.sgpa.model.enums;

/**
 * Enumeration des roles utilisateurs dans le systeme SGPA.
 * <p>
 * Definit les differents niveaux d'acces dans l'application :
 * <ul>
 *   <li>{@link #PHARMACIEN} : Acces complet a toutes les fonctionnalites</li>
 *   <li>{@link #PREPARATEUR} : Acces limite aux ventes uniquement</li>
 * </ul>
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public enum Role {

    /**
     * Role Pharmacien - Administrateur du systeme.
     * Acces complet : gestion des stocks, ventes, commandes, utilisateurs, rapports.
     */
    PHARMACIEN("Pharmacien", "Acces complet au systeme"),

    /**
     * Role Preparateur - Utilisateur limite.
     * Acces restreint : ventes uniquement.
     */
    PREPARATEUR("Preparateur", "Acces aux ventes uniquement");

    private final String libelle;
    private final String description;

    /**
     * Constructeur de l'enumeration Role.
     *
     * @param libelle     le libelle affichable du role
     * @param description la description des droits associes
     */
    Role(String libelle, String description) {
        this.libelle = libelle;
        this.description = description;
    }

    /**
     * Retourne le libelle du role.
     *
     * @return le libelle affichable
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Retourne la description du role.
     *
     * @return la description des droits
     */
    public String getDescription() {
        return description;
    }

    /**
     * Verifie si ce role a les droits d'administration.
     *
     * @return true si le role est PHARMACIEN
     */
    public boolean isAdmin() {
        return this == PHARMACIEN;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
