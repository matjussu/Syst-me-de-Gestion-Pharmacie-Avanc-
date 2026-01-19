package com.sgpa.model.enums;

/**
 * Motifs possibles pour les ecarts d'inventaire.
 *
 * @author SGPA Team
 * @version 1.0
 */
public enum MotifEcart {

    PERTE("Perte"),
    CASSE("Casse/Deterioration"),
    VOL("Vol"),
    PEREMPTION("Peremption"),
    AJUSTEMENT("Ajustement"),
    ERREUR_SAISIE("Erreur de saisie"),
    AUTRE("Autre");

    private final String libelle;

    MotifEcart(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    /**
     * Convertit une chaine en MotifEcart.
     *
     * @param value la valeur a convertir
     * @return le motif correspondant
     */
    public static MotifEcart fromString(String value) {
        if (value == null) {
            return AUTRE;
        }
        for (MotifEcart motif : values()) {
            if (motif.name().equalsIgnoreCase(value)) {
                return motif;
            }
        }
        return AUTRE;
    }
}
