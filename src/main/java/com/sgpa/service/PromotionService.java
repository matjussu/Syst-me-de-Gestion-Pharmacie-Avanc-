package com.sgpa.service;

import com.sgpa.dao.PromotionDAO;
import com.sgpa.dao.impl.PromotionDAOImpl;
import com.sgpa.dto.PromotionApplicationDTO;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.model.Promotion;
import com.sgpa.model.enums.TypePromotion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des promotions et remises.
 * <p>
 * Gere la creation, modification et application des promotions sur les ventes.
 * Supporte les types : pourcentage, montant fixe, offre groupee, prix special.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public class PromotionService {

    private static final Logger logger = LoggerFactory.getLogger(PromotionService.class);

    private final PromotionDAO promotionDAO;

    /**
     * Constructeur par defaut.
     */
    public PromotionService() {
        this.promotionDAO = new PromotionDAOImpl();
    }

    /**
     * Constructeur avec injection du DAO (pour tests).
     *
     * @param promotionDAO le DAO promotion
     */
    public PromotionService(PromotionDAO promotionDAO) {
        this.promotionDAO = promotionDAO;
    }

    // ==================== CRUD ====================

    /**
     * Cree une nouvelle promotion.
     *
     * @param promotion la promotion a creer
     * @return la promotion creee avec son ID
     * @throws ServiceException si la validation echoue ou erreur de persistence
     */
    public Promotion creerPromotion(Promotion promotion) throws ServiceException {
        logger.debug("Creation promotion: {}", promotion.getNom());

        validerPromotion(promotion);

        try {
            Promotion saved = promotionDAO.save(promotion);
            logger.info("Promotion creee: {} (ID: {})", saved.getNom(), saved.getIdPromotion());
            return saved;
        } catch (DAOException e) {
            logger.error("Erreur lors de la creation de la promotion", e);
            throw new ServiceException("Erreur lors de la creation de la promotion", e);
        }
    }

    /**
     * Met a jour une promotion existante.
     *
     * @param promotion la promotion a mettre a jour
     * @throws ServiceException si la validation echoue ou erreur de persistence
     */
    public void modifierPromotion(Promotion promotion) throws ServiceException {
        logger.debug("Modification promotion: {}", promotion.getIdPromotion());

        if (promotion.getIdPromotion() == null) {
            throw new ServiceException("L'ID de la promotion est requis pour la mise a jour");
        }

        validerPromotion(promotion);

        try {
            promotionDAO.update(promotion);
            logger.info("Promotion modifiee: {}", promotion.getIdPromotion());
        } catch (DAOException e) {
            logger.error("Erreur lors de la modification de la promotion", e);
            throw new ServiceException("Erreur lors de la modification de la promotion", e);
        }
    }

    /**
     * Supprime une promotion.
     *
     * @param idPromotion l'ID de la promotion a supprimer
     * @throws ServiceException si erreur de persistence
     */
    public void supprimerPromotion(int idPromotion) throws ServiceException {
        logger.debug("Suppression promotion: {}", idPromotion);

        try {
            promotionDAO.delete(idPromotion);
            logger.info("Promotion supprimee: {}", idPromotion);
        } catch (DAOException e) {
            logger.error("Erreur lors de la suppression de la promotion", e);
            throw new ServiceException("Erreur lors de la suppression de la promotion", e);
        }
    }

    /**
     * Active ou desactive une promotion.
     *
     * @param idPromotion l'ID de la promotion
     * @param actif       true pour activer, false pour desactiver
     * @throws ServiceException si erreur de persistence
     */
    public void setActif(int idPromotion, boolean actif) throws ServiceException {
        try {
            promotionDAO.setActif(idPromotion, actif);
            logger.info("Promotion {} {}", idPromotion, actif ? "activee" : "desactivee");
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du changement de statut", e);
        }
    }

    // ==================== RECHERCHE ====================

    /**
     * Recupere toutes les promotions.
     *
     * @return la liste de toutes les promotions
     * @throws ServiceException si erreur de persistence
     */
    public List<Promotion> getToutesPromotions() throws ServiceException {
        try {
            return promotionDAO.findAll();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des promotions", e);
        }
    }

    /**
     * Recupere les promotions actuellement actives et valides.
     *
     * @return la liste des promotions actives dans la periode de validite
     * @throws ServiceException si erreur de persistence
     */
    public List<Promotion> getPromotionsActives() throws ServiceException {
        try {
            return promotionDAO.findActives();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation des promotions actives", e);
        }
    }

    /**
     * Recupere une promotion par son ID.
     *
     * @param idPromotion l'ID de la promotion
     * @return la promotion ou Optional vide
     * @throws ServiceException si erreur de persistence
     */
    public Optional<Promotion> getPromotion(int idPromotion) throws ServiceException {
        try {
            return promotionDAO.findById(idPromotion);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recuperation de la promotion", e);
        }
    }

    /**
     * Recherche une promotion par son code promo.
     *
     * @param codePromo le code promotionnel
     * @return la promotion ou Optional vide
     * @throws ServiceException si erreur de persistence
     */
    public Optional<Promotion> getPromotionParCode(String codePromo) throws ServiceException {
        try {
            return promotionDAO.findByCode(codePromo);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche par code promo", e);
        }
    }

    /**
     * Recupere les promotions applicables a un medicament specifique.
     *
     * @param idMedicament l'ID du medicament
     * @return la liste des promotions pour ce medicament
     * @throws ServiceException si erreur de persistence
     */
    public List<Promotion> getPromotionsPourMedicament(int idMedicament) throws ServiceException {
        try {
            return promotionDAO.findByMedicament(idMedicament);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche des promotions", e);
        }
    }

    // ==================== APPLICATION ====================

    /**
     * Applique automatiquement la meilleure promotion disponible pour un medicament.
     *
     * @param idMedicament l'ID du medicament
     * @param quantite     la quantite achetee
     * @param prixUnitaire le prix unitaire du medicament
     * @return le DTO avec le resultat de l'application
     * @throws ServiceException si erreur de calcul
     */
    public PromotionApplicationDTO appliquerPromotion(int idMedicament, int quantite,
                                                       BigDecimal prixUnitaire) throws ServiceException {
        logger.debug("Application promo auto pour medicament {}, qte {}", idMedicament, quantite);

        try {
            List<Promotion> promotions = promotionDAO.findByMedicament(idMedicament);

            if (promotions.isEmpty()) {
                return PromotionApplicationDTO.aucunePromotion();
            }

            // Trouver la meilleure promotion (celle qui donne la plus grande remise)
            PromotionApplicationDTO meilleureRemise = null;

            for (Promotion promo : promotions) {
                if (!promo.estValide()) continue;

                PromotionApplicationDTO resultat = calculerRemise(promo, quantite, prixUnitaire);
                if (resultat.isAppliquee()) {
                    if (meilleureRemise == null ||
                        resultat.getMontantRemise().compareTo(meilleureRemise.getMontantRemise()) > 0) {
                        meilleureRemise = resultat;
                    }
                }
            }

            return meilleureRemise != null ? meilleureRemise : PromotionApplicationDTO.aucunePromotion();

        } catch (DAOException e) {
            logger.error("Erreur lors de l'application de la promotion", e);
            throw new ServiceException("Erreur lors de l'application de la promotion", e);
        }
    }

    /**
     * Applique un code promotionnel specifique.
     *
     * @param codePromo    le code promotionnel saisi
     * @param idMedicament l'ID du medicament (peut etre null si promo globale)
     * @param quantite     la quantite achetee
     * @param prixUnitaire le prix unitaire
     * @return le DTO avec le resultat de l'application
     * @throws ServiceException si erreur de calcul
     */
    public PromotionApplicationDTO appliquerCodePromo(String codePromo, Integer idMedicament,
                                                       int quantite, BigDecimal prixUnitaire)
            throws ServiceException {
        logger.debug("Application code promo: {}", codePromo);

        if (codePromo == null || codePromo.isBlank()) {
            return PromotionApplicationDTO.nonApplicable("Code promotionnel vide");
        }

        try {
            Optional<Promotion> promoOpt = promotionDAO.findByCode(codePromo.trim().toUpperCase());

            if (promoOpt.isEmpty()) {
                return PromotionApplicationDTO.promotionIntrouvable();
            }

            Promotion promo = promoOpt.get();

            // Verifier la validite
            if (!promo.estValide()) {
                if (promo.estExpiree()) {
                    return PromotionApplicationDTO.nonApplicable("Cette promotion a expire");
                } else if (promo.estFuture()) {
                    return PromotionApplicationDTO.nonApplicable(
                            "Cette promotion sera active a partir du " + promo.getDateDebut());
                } else {
                    return PromotionApplicationDTO.nonApplicable("Cette promotion n'est pas active");
                }
            }

            // Verifier si le medicament est concerne
            if (idMedicament != null && !promo.concerneMedicament(idMedicament)) {
                return PromotionApplicationDTO.nonApplicable(
                        "Cette promotion ne s'applique pas a ce produit");
            }

            PromotionApplicationDTO resultat = calculerRemise(promo, quantite, prixUnitaire);
            resultat.setCodePromo(codePromo);
            return resultat;

        } catch (DAOException e) {
            logger.error("Erreur lors de l'application du code promo", e);
            throw new ServiceException("Erreur lors de l'application du code promo", e);
        }
    }

    /**
     * Calcule la remise pour une promotion donnee.
     *
     * @param promotion    la promotion a appliquer
     * @param quantite     la quantite achetee
     * @param prixUnitaire le prix unitaire
     * @return le DTO avec le montant de la remise calculee
     */
    public PromotionApplicationDTO calculerRemise(Promotion promotion, int quantite,
                                                   BigDecimal prixUnitaire) {
        if (promotion == null || quantite <= 0 || prixUnitaire == null) {
            return PromotionApplicationDTO.aucunePromotion();
        }

        BigDecimal montantRemise;
        int quantiteGratuite = 0;

        switch (promotion.getTypePromotion()) {
            case POURCENTAGE -> {
                // remise = prix * qte * (valeur / 100)
                BigDecimal pourcentage = promotion.getValeur().divide(
                        BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                montantRemise = prixUnitaire.multiply(BigDecimal.valueOf(quantite))
                        .multiply(pourcentage)
                        .setScale(2, RoundingMode.HALF_UP);
            }
            case MONTANT_FIXE -> {
                // remise = valeur * qte
                montantRemise = promotion.getValeur().multiply(BigDecimal.valueOf(quantite))
                        .setScale(2, RoundingMode.HALF_UP);
                // Ne pas depasser le prix total
                BigDecimal prixTotal = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
                if (montantRemise.compareTo(prixTotal) > 0) {
                    montantRemise = prixTotal;
                }
            }
            case OFFRE_GROUPEE -> {
                // N achetes = M gratuits
                int qteRequise = promotion.getQuantiteRequise();
                int qteOfferte = promotion.getQuantiteOfferte();
                if (qteRequise <= 0) qteRequise = 1;

                int nbLots = quantite / (qteRequise + qteOfferte);
                quantiteGratuite = nbLots * qteOfferte;
                montantRemise = prixUnitaire.multiply(BigDecimal.valueOf(quantiteGratuite))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            case PRIX_SPECIAL -> {
                // remise = (prixNormal - prixSpecial) * qte
                BigDecimal economie = prixUnitaire.subtract(promotion.getValeur());
                if (economie.compareTo(BigDecimal.ZERO) > 0) {
                    montantRemise = economie.multiply(BigDecimal.valueOf(quantite))
                            .setScale(2, RoundingMode.HALF_UP);
                } else {
                    montantRemise = BigDecimal.ZERO;
                }
            }
            default -> {
                return PromotionApplicationDTO.aucunePromotion();
            }
        }

        PromotionApplicationDTO dto = new PromotionApplicationDTO(
                promotion.getIdPromotion(),
                promotion.getNom(),
                promotion.getTypePromotion(),
                montantRemise
        );
        dto.setQuantiteGratuite(quantiteGratuite);

        return dto;
    }

    /**
     * Enregistre l'utilisation d'une promotion lors d'une vente.
     *
     * @param idPromotion    l'ID de la promotion
     * @param idVente        l'ID de la vente
     * @param idLigneVente   l'ID de la ligne de vente (peut etre null)
     * @param montantReduction le montant de la reduction
     * @throws ServiceException si erreur de persistence
     */
    public void enregistrerUtilisation(int idPromotion, int idVente, Integer idLigneVente,
                                        BigDecimal montantReduction) throws ServiceException {
        try {
            promotionDAO.enregistrerUtilisation(idPromotion, idVente, idLigneVente, montantReduction);
            logger.debug("Utilisation promo {} enregistree pour vente {}", idPromotion, idVente);
        } catch (DAOException e) {
            logger.error("Erreur lors de l'enregistrement de l'utilisation", e);
            throw new ServiceException("Erreur lors de l'enregistrement de l'utilisation", e);
        }
    }

    // ==================== VALIDATION ====================

    /**
     * Valide les donnees d'une promotion avant sauvegarde.
     *
     * @param promotion la promotion a valider
     * @throws ServiceException si la validation echoue
     */
    private void validerPromotion(Promotion promotion) throws ServiceException {
        if (promotion.getNom() == null || promotion.getNom().isBlank()) {
            throw new ServiceException("Le nom de la promotion est obligatoire");
        }

        if (promotion.getTypePromotion() == null) {
            throw new ServiceException("Le type de promotion est obligatoire");
        }

        if (promotion.getValeur() == null || promotion.getValeur().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("La valeur de la promotion doit etre positive");
        }

        if (promotion.getTypePromotion() == TypePromotion.POURCENTAGE &&
            promotion.getValeur().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ServiceException("Le pourcentage ne peut pas depasser 100%");
        }

        if (promotion.getDateDebut() == null) {
            throw new ServiceException("La date de debut est obligatoire");
        }

        if (promotion.getDateFin() == null) {
            throw new ServiceException("La date de fin est obligatoire");
        }

        if (promotion.getDateFin().isBefore(promotion.getDateDebut())) {
            throw new ServiceException("La date de fin doit etre apres la date de debut");
        }

        if (promotion.getTypePromotion() == TypePromotion.OFFRE_GROUPEE) {
            if (promotion.getQuantiteRequise() <= 0) {
                throw new ServiceException("La quantite requise doit etre positive pour une offre groupee");
            }
            if (promotion.getQuantiteOfferte() <= 0) {
                throw new ServiceException("La quantite offerte doit etre positive pour une offre groupee");
            }
        }
    }

    /**
     * Compte le nombre d'utilisations d'une promotion.
     *
     * @param idPromotion l'ID de la promotion
     * @return le nombre d'utilisations
     * @throws ServiceException si erreur de persistence
     */
    public int getNombreUtilisations(int idPromotion) throws ServiceException {
        try {
            return promotionDAO.countUtilisations(idPromotion);
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors du comptage des utilisations", e);
        }
    }

    /**
     * Recupere les promotions expirees.
     *
     * @return la liste des promotions expirees
     * @throws ServiceException si erreur de persistence
     */
    public List<Promotion> getPromotionsExpirees() throws ServiceException {
        try {
            return promotionDAO.findExpirees();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche des promotions expirees", e);
        }
    }

    /**
     * Recupere les promotions futures (pas encore actives).
     *
     * @return la liste des promotions futures
     * @throws ServiceException si erreur de persistence
     */
    public List<Promotion> getPromotionsFutures() throws ServiceException {
        try {
            return promotionDAO.findFutures();
        } catch (DAOException e) {
            throw new ServiceException("Erreur lors de la recherche des promotions futures", e);
        }
    }
}
