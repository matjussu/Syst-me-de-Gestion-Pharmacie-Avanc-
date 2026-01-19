package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Retour;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface DAO pour les operations sur les retours.
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface RetourDAO extends GenericDAO<Retour, Integer> {

    /**
     * Recherche les retours pour une vente donnee.
     *
     * @param idVente l'identifiant de la vente
     * @return la liste des retours
     * @throws DAOException si une erreur survient
     */
    List<Retour> findByVente(int idVente) throws DAOException;

    /**
     * Recherche les retours dans une periode donnee.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des retours
     * @throws DAOException si une erreur survient
     */
    List<Retour> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException;

    /**
     * Recherche les retours effectues par un utilisateur.
     *
     * @param idUtilisateur l'identifiant de l'utilisateur
     * @return la liste des retours
     * @throws DAOException si une erreur survient
     */
    List<Retour> findByUtilisateur(int idUtilisateur) throws DAOException;

    /**
     * Recherche les retours reintegres ou non.
     *
     * @param reintegre true pour les retours reintegres, false sinon
     * @return la liste des retours
     * @throws DAOException si une erreur survient
     */
    List<Retour> findByReintegre(boolean reintegre) throws DAOException;

    /**
     * Compte le nombre total de retours.
     *
     * @return le nombre de retours
     * @throws DAOException si une erreur survient
     */
    long count() throws DAOException;
}
