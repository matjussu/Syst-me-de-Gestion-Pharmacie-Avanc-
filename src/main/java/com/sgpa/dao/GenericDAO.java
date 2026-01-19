package com.sgpa.dao;

import com.sgpa.exception.DAOException;

import java.util.List;
import java.util.Optional;

/**
 * Interface generique pour les operations CRUD de base.
 * <p>
 * Definit le contrat commun pour tous les DAOs de l'application.
 * Utilise les generiques pour etre reutilisable avec n'importe quelle entite.
 * </p>
 *
 * @param <T>  le type de l'entite
 * @param <ID> le type de l'identifiant
 * @author SGPA Team
 * @version 1.0
 */
public interface GenericDAO<T, ID> {

    /**
     * Recherche une entite par son identifiant.
     *
     * @param id l'identifiant de l'entite
     * @return un Optional contenant l'entite si trouvee, vide sinon
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Optional<T> findById(ID id) throws DAOException;

    /**
     * Recupere toutes les entites.
     *
     * @return la liste de toutes les entites
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<T> findAll() throws DAOException;

    /**
     * Sauvegarde une nouvelle entite.
     *
     * @param entity l'entite a sauvegarder
     * @return l'entite sauvegardee avec son identifiant genere
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    T save(T entity) throws DAOException;

    /**
     * Met a jour une entite existante.
     *
     * @param entity l'entite a mettre a jour
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void update(T entity) throws DAOException;

    /**
     * Supprime une entite par son identifiant.
     *
     * @param id l'identifiant de l'entite a supprimer
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void delete(ID id) throws DAOException;

    /**
     * Compte le nombre total d'entites.
     *
     * @return le nombre d'entites
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    long count() throws DAOException;

    /**
     * Verifie si une entite existe par son identifiant.
     *
     * @param id l'identifiant a verifier
     * @return true si l'entite existe
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    boolean existsById(ID id) throws DAOException;
}
