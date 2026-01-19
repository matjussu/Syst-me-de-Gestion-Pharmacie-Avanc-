package com.sgpa.model.enums;

/**
 * Enumeration des types de promotions disponibles.
 * <p>
 * Types supportes :
 * <ul>
 *   <li>{@link #POURCENTAGE} : Reduction en pourcentage du prix</li>
 *   <li>{@link #MONTANT_FIXE} : Reduction d'un montant fixe</li>
 *   <li>{@link #OFFRE_GROUPEE} : N achetes = M offerts</li>
 *   <li>{@link #PRIX_SPECIAL} : Prix fixe temporaire</li>
 * </ul>
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public enum TypePromotion {

    /**
     * Reduction en pourcentage du prix unitaire.
     * Exemple: -20% sur le prix
     */
    POURCENTAGE("Pourcentage", "Reduction en % du prix", "%"),

    /**
     * Reduction d'un montant fixe par unite.
     * Exemple: -5 EUR par unite
     */
    MONTANT_FIXE("Montant fixe", "Reduction d'un montant fixe", "EUR"),

    /**
     * Offre groupee : N achetes = M offerts.
     * Exemple: 2 achetes = 1 offert
     */
    OFFRE_GROUPEE("Offre groupee", "N achetes = M offerts", ""),

    /**
     * Prix special temporaire.
     * Exemple: Prix special a 9.99 EUR
     */
    PRIX_SPECIAL("Prix special", "Prix fixe temporaire", "EUR");

    private final String libelle;
    private final String description;
    private final String unite;

    /**
     * Constructeur de l'enumeration TypePromotion.
     *
     * @param libelle     le libelle affichable du type
     * @param description la description du type
     * @param unite       l'unite de la valeur (%, EUR, etc.)
     */
    TypePromotion(String libelle, String description, String unite) {
        this.libelle = libelle;
        this.description = description;
        this.unite = unite;
    }

    /**
     * Retourne le libelle du type.
     *
     * @return le libelle affichable
     */
    public String getLibelle() {
        return libelle;
    }

    /**
     * Retourne la description du type.
     *
     * @return la description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Retourne l'unite de la valeur.
     *
     * @return l'unite (%, EUR, etc.)
     */
    public String getUnite() {
        return unite;
    }

    /**
     * Verifie si ce type necessite une quantite requise/offerte.
     *
     * @return true si le type est OFFRE_GROUPEE
     */
    public boolean necessiteQuantites() {
        return this == OFFRE_GROUPEE;
    }

    /**
     * Verifie si la valeur represente un pourcentage.
     *
     * @return true si le type est POURCENTAGE
     */
    public boolean estPourcentage() {
        return this == POURCENTAGE;
    }

    /**
     * Verifie si la valeur represente un montant monetaire.
     *
     * @return true si le type est MONTANT_FIXE ou PRIX_SPECIAL
     */
    public boolean estMontant() {
        return this == MONTANT_FIXE || this == PRIX_SPECIAL;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
