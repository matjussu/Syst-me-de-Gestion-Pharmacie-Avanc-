package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Utilisateur;
import com.sgpa.model.enums.Role;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour les operations sur les utilisateurs.
 * <p>
 * Etend {@link GenericDAO} avec des methodes specifiques
 * pour l'authentification et la gestion des utilisateurs.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface UtilisateurDAO extends GenericDAO<Utilisateur, Integer> {

    /**
     * Recherche un utilisateur par son nom d'utilisateur.
     * <p>
     * Utilise pour l'authentification.
     * </p>
     *
     * @param nomUtilisateur le nom d'utilisateur
     * @return un Optional contenant l'utilisateur si trouve
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Optional<Utilisateur> findByNomUtilisateur(String nomUtilisateur) throws DAOException;

    /**
     * Recherche les utilisateurs par role.
     *
     * @param role le role recherche
     * @return la liste des utilisateurs ayant ce role
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Utilisateur> findByRole(Role role) throws DAOException;

    /**
     * Recherche les utilisateurs actifs.
     *
     * @return la liste des utilisateurs actifs
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Utilisateur> findAllActive() throws DAOException;

    /**
     * Verifie si un nom d'utilisateur existe deja.
     *
     * @param nomUtilisateur le nom d'utilisateur a verifier
     * @return true si le nom existe deja
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    boolean existsByNomUtilisateur(String nomUtilisateur) throws DAOException;

    /**
     * Met a jour la date de derniere connexion d'un utilisateur.
     *
     * @param idUtilisateur l'identifiant de l'utilisateur
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void updateDerniereConnexion(int idUtilisateur) throws DAOException;

    /**
     * Met a jour le mot de passe d'un utilisateur.
     *
     * @param idUtilisateur    l'identifiant de l'utilisateur
     * @param nouveauMotDePasse le nouveau mot de passe hashe
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void updateMotDePasse(int idUtilisateur, String nouveauMotDePasse) throws DAOException;

    /**
     * Active ou desactive un utilisateur.
     *
     * @param idUtilisateur l'identifiant de l'utilisateur
     * @param actif         true pour activer, false pour desactiver
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void setActif(int idUtilisateur, boolean actif) throws DAOException;
}
