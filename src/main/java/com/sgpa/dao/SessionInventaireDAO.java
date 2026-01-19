package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.SessionInventaire;
import com.sgpa.model.enums.StatutInventaire;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface DAO pour les operations sur les sessions d'inventaire.
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface SessionInventaireDAO extends GenericDAO<SessionInventaire, Integer> {

    /**
     * Recherche les sessions par statut.
     *
     * @param statut le statut recherche
     * @return la liste des sessions
     * @throws DAOException si une erreur survient
     */
    List<SessionInventaire> findByStatut(StatutInventaire statut) throws DAOException;

    /**
     * Recherche les sessions dans une periode donnee.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des sessions
     * @throws DAOException si une erreur survient
     */
    List<SessionInventaire> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException;

    /**
     * Recherche les sessions creees par un utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @return la liste des sessions
     * @throws DAOException si une erreur survient
     */
    List<SessionInventaire> findByUtilisateur(int idUtilisateur) throws DAOException;

    /**
     * Recupere les sessions les plus recentes.
     *
     * @param limit le nombre maximum de sessions
     * @return la liste des sessions
     * @throws DAOException si une erreur survient
     */
    List<SessionInventaire> findRecent(int limit) throws DAOException;

    /**
     * Verifie s'il existe une session en cours.
     *
     * @return true s'il y a une session en cours
     * @throws DAOException si une erreur survient
     */
    boolean hasSessionEnCours() throws DAOException;

    /**
     * Termine une session d'inventaire.
     *
     * @param idSession l'ID de la session
     * @throws DAOException si une erreur survient
     */
    void terminerSession(int idSession) throws DAOException;

    /**
     * Annule une session d'inventaire.
     *
     * @param idSession l'ID de la session
     * @throws DAOException si une erreur survient
     */
    void annulerSession(int idSession) throws DAOException;
}
