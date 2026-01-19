package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Regularisation;
import com.sgpa.model.enums.MotifEcart;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface DAO pour les operations sur les regularisations.
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface RegularisationDAO extends GenericDAO<Regularisation, Integer> {

    /**
     * Recherche les regularisations d'une session.
     *
     * @param idSession l'ID de la session
     * @return la liste des regularisations
     * @throws DAOException si une erreur survient
     */
    List<Regularisation> findBySession(int idSession) throws DAOException;

    /**
     * Recherche les regularisations d'un lot.
     *
     * @param idLot l'ID du lot
     * @return la liste des regularisations
     * @throws DAOException si une erreur survient
     */
    List<Regularisation> findByLot(int idLot) throws DAOException;

    /**
     * Recherche les regularisations par raison.
     *
     * @param raison la raison recherchee
     * @return la liste des regularisations
     * @throws DAOException si une erreur survient
     */
    List<Regularisation> findByRaison(MotifEcart raison) throws DAOException;

    /**
     * Recherche les regularisations dans une periode.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des regularisations
     * @throws DAOException si une erreur survient
     */
    List<Regularisation> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException;

    /**
     * Calcule le total des ajustements pour une session.
     *
     * @param idSession l'ID de la session
     * @return le total des ajustements (positif ou negatif)
     * @throws DAOException si une erreur survient
     */
    int getTotalAjustement(int idSession) throws DAOException;

    /**
     * Compte le nombre de regularisations pour une session.
     *
     * @param idSession l'ID de la session
     * @return le nombre de regularisations
     * @throws DAOException si une erreur survient
     */
    int countBySession(int idSession) throws DAOException;
}
