package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.AuditLog;
import com.sgpa.model.enums.TypeAction;

import java.time.LocalDate;
import java.util.List;

/**
 * Interface DAO pour les operations sur le journal d'audit.
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface AuditLogDAO {

    /**
     * Enregistre une entree d'audit.
     *
     * @param auditLog l'entree a enregistrer
     * @return l'entree avec son ID genere
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    AuditLog save(AuditLog auditLog) throws DAOException;

    /**
     * Recupere toutes les entrees d'audit.
     *
     * @return la liste de toutes les entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> findAll() throws DAOException;

    /**
     * Recupere les entrees d'audit avec pagination.
     *
     * @param limit  le nombre maximum d'entrees
     * @param offset le decalage
     * @return la liste des entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> findAllPaginated(int limit, int offset) throws DAOException;

    /**
     * Recupere les entrees d'audit pour une periode.
     *
     * @param dateDebut la date de debut
     * @param dateFin   la date de fin
     * @return la liste des entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> findByDateRange(LocalDate dateDebut, LocalDate dateFin) throws DAOException;

    /**
     * Recupere les entrees d'audit pour un utilisateur.
     *
     * @param idUtilisateur l'ID de l'utilisateur
     * @return la liste des entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> findByUtilisateur(int idUtilisateur) throws DAOException;

    /**
     * Recupere les entrees d'audit par type d'action.
     *
     * @param typeAction le type d'action
     * @return la liste des entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> findByTypeAction(TypeAction typeAction) throws DAOException;

    /**
     * Recupere les entrees d'audit pour une entite specifique.
     *
     * @param entite   le type d'entite
     * @param idEntite l'ID de l'entite
     * @return la liste des entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> findByEntite(String entite, int idEntite) throws DAOException;

    /**
     * Recherche les entrees d'audit selon plusieurs criteres.
     *
     * @param dateDebut     la date de debut (peut etre null)
     * @param dateFin       la date de fin (peut etre null)
     * @param typeAction    le type d'action (peut etre null)
     * @param entite        le type d'entite (peut etre null)
     * @param idUtilisateur l'ID de l'utilisateur (peut etre null)
     * @param limit         le nombre maximum d'entrees
     * @return la liste des entrees correspondant aux criteres
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<AuditLog> search(LocalDate dateDebut, LocalDate dateFin, TypeAction typeAction,
                          String entite, Integer idUtilisateur, int limit) throws DAOException;

    /**
     * Compte le nombre total d'entrees d'audit.
     *
     * @return le nombre d'entrees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    long count() throws DAOException;

    /**
     * Supprime les entrees d'audit anterieures a une date.
     * Utilise pour la maintenance de la base de donnees.
     *
     * @param dateAvant la date limite
     * @return le nombre d'entrees supprimees
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int deleteOlderThan(LocalDate dateAvant) throws DAOException;
}
