package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Fournisseur;

import java.util.List;

/**
 * Interface DAO pour les operations sur les fournisseurs.
 * <p>
 * Etend {@link GenericDAO} avec des methodes specifiques
 * pour la gestion des fournisseurs.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface FournisseurDAO extends GenericDAO<Fournisseur, Integer> {

    /**
     * Recherche des fournisseurs par nom (recherche partielle).
     *
     * @param nom le nom ou fragment de nom a rechercher
     * @return la liste des fournisseurs correspondants
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Fournisseur> findByNom(String nom) throws DAOException;

    /**
     * Recherche les fournisseurs actifs.
     *
     * @return la liste des fournisseurs actifs
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Fournisseur> findAllActive() throws DAOException;

    /**
     * Recherche un fournisseur par son nom exact.
     *
     * @param nom le nom exact du fournisseur
     * @return le fournisseur ou null si non trouve
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Fournisseur findByNomExact(String nom) throws DAOException;

    /**
     * Active ou desactive un fournisseur.
     *
     * @param idFournisseur l'identifiant du fournisseur
     * @param actif         true pour activer, false pour desactiver
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    void setActif(int idFournisseur, boolean actif) throws DAOException;
}
