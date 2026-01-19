package com.sgpa.model.enums;

/**
 * Statuts possibles d'une session d'inventaire.
 *
 * @author SGPA Team
 * @version 1.0
 */
public enum StatutInventaire {

    EN_COURS("En cours"),
    TERMINEE("Terminee"),
    ANNULEE("Annulee");

    private final String libelle;

    StatutInventaire(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Convertit une chaine en StatutInventaire.
     *
     * @param value la valeur a convertir
     * @return le statut correspondant
     */
    public static StatutInventaire fromString(String value) {
        if (value == null) {
            return EN_COURS;
        }
        for (StatutInventaire statut : values()) {
            if (statut.name().equalsIgnoreCase(value)) {
                return statut;
            }
        }
        return EN_COURS;
    }
}
