package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Promotion;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour les operations sur les promotions.
 * <p>
 * Etend {@link GenericDAO} avec des methodes specifiques
 * pour la gestion des promotions et remises.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface PromotionDAO extends GenericDAO<Promotion, Integer> {

    /**
     * Recherche une promotion par son code promotionnel.
     *
     * @param codePromo le code promotionnel
     * @return un Optional contenant la promotion si trouvee
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Optional<Promotion> findByCode(String codePromo) throws DAOException;

    /**
     * Recherche les promotions actuellement actives et valides.
     *
     * @return la liste des promotions actives dans la periode de validite
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Promotion> findActives() throws DAOException;

    /**
     * Recherche les promotions applicables a un medicament specifique.
     *
     * @param idMedicament l'identifiant du medicament
     * @return la liste des promotions pour ce medicament
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Promotion> findByMedicament(int idMedicament) throws DAOException;

    /**
     * Recherche les promotions expirees.
     *
     * @return la liste des promotions dont la date de fin est passee
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Promotion> findExpirees() throws DAOException;

    /**
     * Recherche les promotions futures (pas encore commencees).
     *
     * @return la liste des promotions dont la date de debut n'est pas atteinte
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Promotion> findFutures() throws DAOException;

    /**
     * Ajoute un medicament a une promotion.
     *
     * @param idPromotion  l'identifiant de la promotion
     * @param idMedicament l'identifiant du medicament
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void addMedicament(int idPromotion, int idMedicament) throws DAOException;

    /**
     * Retire un medicament d'une promotion.
     *
     * @param idPromotion  l'identifiant de la promotion
     * @param idMedicament l'identifiant du medicament
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void removeMedicament(int idPromotion, int idMedicament) throws DAOException;

    /**
     * Supprime tous les medicaments associes a une promotion.
     *
     * @param idPromotion l'identifiant de la promotion
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void removeAllMedicaments(int idPromotion) throws DAOException;

    /**
     * Recupere les IDs des medicaments associes a une promotion.
     *
     * @param idPromotion l'identifiant de la promotion
     * @return la liste des IDs de medicaments
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Integer> getMedicamentIds(int idPromotion) throws DAOException;

    /**
     * Enregistre l'utilisation d'une promotion lors d'une vente.
     *
     * @param idPromotion    l'identifiant de la promotion
     * @param idVente        l'identifiant de la vente
     * @param idLigneVente   l'identifiant de la ligne de vente (peut etre null)
     * @param montantReduction le montant de la reduction appliquee
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void enregistrerUtilisation(int idPromotion, int idVente, Integer idLigneVente,
                                 java.math.BigDecimal montantReduction) throws DAOException;

    /**
     * Compte le nombre d'utilisations d'une promotion.
     *
     * @param idPromotion l'identifiant de la promotion
     * @return le nombre d'utilisations
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int countUtilisations(int idPromotion) throws DAOException;

    /**
     * Active ou desactive une promotion.
     *
     * @param idPromotion l'identifiant de la promotion
     * @param actif       true pour activer, false pour desactiver
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void setActif(int idPromotion, boolean actif) throws DAOException;
}
