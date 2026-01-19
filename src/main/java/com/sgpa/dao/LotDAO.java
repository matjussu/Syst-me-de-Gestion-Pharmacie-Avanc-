package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Lot;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface DAO pour les operations sur les lots.
 * <p>
 * Etend {@link GenericDAO} avec des methodes specifiques
 * pour la gestion des lots et l'algorithme FEFO.
 * </p>
 * <p>
 * <b>FEFO (First Expired, First Out):</b> Les methodes de cette interface
 * permettent de recuperer les lots tries par date de peremption pour
 * implementer la logique de vente FEFO.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface LotDAO extends GenericDAO<Lot, Integer> {

    /**
     * Recherche les lots d'un medicament tries par date de peremption (FEFO).
     * <p>
     * <b>Methode cle pour l'algorithme FEFO</b> : retourne les lots
     * avec les dates de peremption les plus proches en premier.
     * Seuls les lots avec du stock disponible sont retournes.
     * </p>
     *
     * @param medicamentId l'identifiant du medicament
     * @return la liste des lots tries par date de peremption croissante
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findByMedicamentIdSortedByExpiration(int medicamentId) throws DAOException;

    /**
     * Recherche les lots dont la date de peremption est avant une date donnee.
     * <p>
     * Utilise pour les alertes de peremption proche.
     * </p>
     *
     * @param date la date limite de peremption
     * @return la liste des lots expirant avant cette date
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findExpiringBefore(LocalDate date) throws DAOException;

    /**
     * Recherche les lots perimes (date de peremption depassee).
     *
     * @return la liste des lots perimes
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findExpired() throws DAOException;

    /**
     * Calcule le stock total pour un medicament (somme des quantites de tous les lots).
     *
     * @param medicamentId l'identifiant du medicament
     * @return le stock total
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int getTotalStockByMedicament(int medicamentId) throws DAOException;

    /**
     * Recherche les lots avec un stock bas (medicaments en dessous du seuil).
     * <p>
     * Effectue une jointure avec la table medicaments pour comparer
     * le stock total au seuil minimum.
     * </p>
     *
     * @return la liste des lots de medicaments en stock bas
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findWithLowStock() throws DAOException;

    /**
     * Recherche les lots d'un fournisseur specifique.
     *
     * @param fournisseurId l'identifiant du fournisseur
     * @return la liste des lots du fournisseur
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findByFournisseur(int fournisseurId) throws DAOException;

    /**
     * Recherche un lot par son numero de lot fabricant.
     *
     * @param numeroLot le numero de lot
     * @return le lot ou null si non trouve
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Lot findByNumeroLot(String numeroLot) throws DAOException;

    /**
     * Recherche les lots vendables d'un medicament (stock > 0 et non perimes).
     *
     * @param medicamentId l'identifiant du medicament
     * @return la liste des lots vendables tries par FEFO
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findVendableByMedicament(int medicamentId) throws DAOException;

    /**
     * Met a jour la quantite en stock d'un lot.
     *
     * @param idLot         l'identifiant du lot
     * @param nouvelleQuantite la nouvelle quantite
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void updateQuantite(int idLot, int nouvelleQuantite) throws DAOException;

    /**
     * Recherche les lots recus entre deux dates.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des lots recus dans l'intervalle
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Lot> findByDateReception(LocalDate dateDebut, LocalDate dateFin) throws DAOException;
}
