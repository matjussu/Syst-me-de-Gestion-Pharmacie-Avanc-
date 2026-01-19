package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.LigneVente;
import com.sgpa.model.Vente;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface DAO pour les operations sur les ventes.
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface VenteDAO extends GenericDAO<Vente, Integer> {

    /**
     * Sauvegarde une ligne de vente.
     *
     * @param ligneVente la ligne de vente a sauvegarder
     * @return la ligne sauvegardee avec son ID
     * @throws DAOException si une erreur survient
     */
    LigneVente saveLigneVente(LigneVente ligneVente) throws DAOException;

    /**
     * Recupere les lignes d'une vente.
     *
     * @param idVente l'ID de la vente
     * @return la liste des lignes de vente
     * @throws DAOException si une erreur survient
     */
    List<LigneVente> findLignesByVenteId(int idVente) throws DAOException;

    /**
     * Recherche les ventes par date.
     *
     * @param date la date de vente
     * @return la liste des ventes de ce jour
     * @throws DAOException si une erreur survient
     */
    List<Vente> findByDate(LocalDate date) throws DAOException;

    /**
     * Recherche les ventes entre deux dates.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des ventes dans la periode
     * @throws DAOException si une erreur survient
     */
    List<Vente> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException;

    /**
     * Recherche les ventes d'un utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @return la liste des ventes de cet utilisateur
     * @throws DAOException si une erreur survient
     */
    List<Vente> findByUtilisateur(int idUtilisateur) throws DAOException;

    /**
     * Recherche les ventes sur ordonnance.
     *
     * @return la liste des ventes sur ordonnance
     * @throws DAOException si une erreur survient
     */
    List<Vente> findVentesSurOrdonnance() throws DAOException;
}
