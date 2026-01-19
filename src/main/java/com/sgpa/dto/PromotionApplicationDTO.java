package com.sgpa.dto;

import com.sgpa.model.enums.TypePromotion;

import java.math.BigDecimal;

/**
 * DTO representant le resultat de l'application d'une promotion sur une ligne de vente.
 * Contient les informations de la reduction appliquee.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class PromotionApplicationDTO {

    /** ID de la promotion appliquee */
    private Integer idPromotion;

    /** Nom de la promotion */
    private String nomPromotion;

    /** Code promo utilise (si applicable) */
    private String codePromo;

    /** Type de promotion */
    private TypePromotion typePromotion;

    /** Montant de la remise calculee */
    private BigDecimal montantRemise;

    /** Quantite gratuite (pour offres groupees) */
    private int quantiteGratuite;

    /** Message descriptif de l'application */
    private String messageApplication;

    /** Indique si la promotion a ete appliquee avec succes */
    private boolean appliquee;

    /** Message d'erreur si la promotion n'a pas pu etre appliquee */
    private String messageErreur;

    public PromotionApplicationDTO() {
        this.montantRemise = BigDecimal.ZERO;
        this.quantiteGratuite = 0;
        this.appliquee = false;
    }

    /**
     * Constructeur pour une application reussie.
     *
     * @param idPromotion    l'ID de la promotion
     * @param nomPromotion   le nom de la promotion
     * @param typePromotion  le type de promotion
     * @param montantRemise  le montant de la remise
     */
    public PromotionApplicationDTO(Integer idPromotion, String nomPromotion,
                                    TypePromotion typePromotion, BigDecimal montantRemise) {
        this();
        this.idPromotion = idPromotion;
        this.nomPromotion = nomPromotion;
        this.typePromotion = typePromotion;
        this.montantRemise = montantRemise;
        this.appliquee = true;
        genererMessage();
    }

    /**
     * Cree un DTO pour une promotion non applicable.
     *
     * @param messageErreur le message d'erreur
     * @return un DTO avec l'erreur
     */
    public static PromotionApplicationDTO nonApplicable(String messageErreur) {
        PromotionApplicationDTO dto = new PromotionApplicationDTO();
        dto.setAppliquee(false);
        dto.setMessageErreur(messageErreur);
        return dto;
    }

    /**
     * Cree un DTO pour une promotion introuvable.
     *
     * @return un DTO avec le message d'erreur
     */
    public static PromotionApplicationDTO promotionIntrouvable() {
        return nonApplicable("Code promotionnel invalide ou expire");
    }

    /**
     * Cree un DTO pour aucune promotion disponible.
     *
     * @return un DTO vide
     */
    public static PromotionApplicationDTO aucunePromotion() {
        PromotionApplicationDTO dto = new PromotionApplicationDTO();
        dto.setAppliquee(false);
        dto.setMessageApplication("Aucune promotion disponible");
        return dto;
    }

    /**
     * Genere le message d'application basee sur le type.
     */
    private void genererMessage() {
        if (typePromotion == null) return;

        this.messageApplication = switch (typePromotion) {
            case POURCENTAGE -> String.format("Remise de %.2f EUR appliquee", montantRemise);
            case MONTANT_FIXE -> String.format("Reduction de %.2f EUR appliquee", montantRemise);
            case OFFRE_GROUPEE -> quantiteGratuite > 0
                ? String.format("%d unite(s) gratuite(s) (valeur: %.2f EUR)", quantiteGratuite, montantRemise)
                : "Offre groupee appliquee";
            case PRIX_SPECIAL -> String.format("Prix special applique, economie: %.2f EUR", montantRemise);
        };
    }

    // Getters et Setters

    public Integer getIdPromotion() {
        return idPromotion;
    }

    public void setIdPromotion(Integer idPromotion) {
        this.idPromotion = idPromotion;
    }

    public String getNomPromotion() {
        return nomPromotion;
    }

    public void setNomPromotion(String nomPromotion) {
        this.nomPromotion = nomPromotion;
    }

    public String getCodePromo() {
        return codePromo;
    }

    public void setCodePromo(String codePromo) {
        this.codePromo = codePromo;
    }

    public TypePromotion getTypePromotion() {
        return typePromotion;
    }

    public void setTypePromotion(TypePromotion typePromotion) {
        this.typePromotion = typePromotion;
    }

    public BigDecimal getMontantRemise() {
        return montantRemise;
    }

    public void setMontantRemise(BigDecimal montantRemise) {
        this.montantRemise = montantRemise != null ? montantRemise : BigDecimal.ZERO;
        genererMessage();
    }

    public int getQuantiteGratuite() {
        return quantiteGratuite;
    }

    public void setQuantiteGratuite(int quantiteGratuite) {
        this.quantiteGratuite = quantiteGratuite;
        genererMessage();
    }

    public String getMessageApplication() {
        return messageApplication;
    }

    public void setMessageApplication(String messageApplication) {
        this.messageApplication = messageApplication;
    }

    public boolean isAppliquee() {
        return appliquee;
    }

    public void setAppliquee(boolean appliquee) {
        this.appliquee = appliquee;
    }

    public String getMessageErreur() {
        return messageErreur;
    }

    public void setMessageErreur(String messageErreur) {
        this.messageErreur = messageErreur;
    }

    /**
     * Retourne le message a afficher (application ou erreur).
     */
    public String getMessage() {
        if (appliquee) {
            return messageApplication;
        } else {
            return messageErreur != null ? messageErreur : "Promotion non appliquee";
        }
    }

    /**
     * Verifie si une remise a ete calculee.
     */
    public boolean aRemise() {
        return appliquee && montantRemise != null && montantRemise.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        if (appliquee) {
            return String.format("PromotionApplication[%s: remise=%.2f EUR, %s]",
                    nomPromotion, montantRemise, messageApplication);
        } else {
            return String.format("PromotionApplication[non appliquee: %s]", messageErreur);
        }
    }
}
