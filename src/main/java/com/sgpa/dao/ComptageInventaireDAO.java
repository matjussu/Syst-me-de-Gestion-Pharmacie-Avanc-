package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.ComptageInventaire;
import com.sgpa.model.enums.MotifEcart;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour les operations sur les comptages d'inventaire.
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface ComptageInventaireDAO extends GenericDAO<ComptageInventaire, Integer> {

    /**
     * Recherche les comptages d'une session.
     *
     * @param idSession l'ID de la session
     * @return la liste des comptages
     * @throws DAOException si une erreur survient
     */
    List<ComptageInventaire> findBySession(int idSession) throws DAOException;

    /**
     * Recherche un comptage par session et lot.
     *
     * @param idSession l'ID de la session
     * @param idLot     l'ID du lot
     * @return le comptage s'il existe
     * @throws DAOException si une erreur survient
     */
    Optional<ComptageInventaire> findBySessionAndLot(int idSession, int idLot) throws DAOException;

    /**
     * Recherche les comptages d'un lot.
     *
     * @param idLot l'ID du lot
     * @return la liste des comptages
     * @throws DAOException si une erreur survient
     */
    List<ComptageInventaire> findByLot(int idLot) throws DAOException;

    /**
     * Recherche les comptages avec ecart pour une session.
     *
     * @param idSession l'ID de la session
     * @return la liste des comptages avec ecart
     * @throws DAOException si une erreur survient
     */
    List<ComptageInventaire> findWithEcart(int idSession) throws DAOException;

    /**
     * Recherche les comptages par motif d'ecart.
     *
     * @param idSession  l'ID de la session
     * @param motifEcart le motif recherche
     * @return la liste des comptages
     * @throws DAOException si une erreur survient
     */
    List<ComptageInventaire> findByMotif(int idSession, MotifEcart motifEcart) throws DAOException;

    /**
     * Calcule le total des ecarts pour une session.
     *
     * @param idSession l'ID de la session
     * @return le total des ecarts
     * @throws DAOException si une erreur survient
     */
    int getTotalEcart(int idSession) throws DAOException;

    /**
     * Compte le nombre de comptages pour une session.
     *
     * @param idSession l'ID de la session
     * @return le nombre de comptages
     * @throws DAOException si une erreur survient
     */
    int countBySession(int idSession) throws DAOException;

    /**
     * Compte le nombre d'ecarts pour une session.
     *
     * @param idSession l'ID de la session
     * @return le nombre d'ecarts
     * @throws DAOException si une erreur survient
     */
    int countEcarts(int idSession) throws DAOException;

    /**
     * Supprime tous les comptages d'une session.
     *
     * @param idSession l'ID de la session
     * @throws DAOException si une erreur survient
     */
    void deleteBySession(int idSession) throws DAOException;
}
