package com.sgpa.dao;

import com.sgpa.exception.DAOException;
import com.sgpa.model.Medicament;

import java.util.List;

/**
 * Interface DAO pour les operations sur les medicaments.
 * <p>
 * Etend {@link GenericDAO} avec des methodes specifiques
 * pour la recherche et la gestion des medicaments.
 * </p>
 *
 * @author SGPA Team
 * @version 1.0
 */
public interface MedicamentDAO extends GenericDAO<Medicament, Integer> {

    /**
     * Recherche des medicaments par nom commercial (recherche partielle).
     *
     * @param nom le nom ou fragment de nom a rechercher
     * @return la liste des medicaments correspondants
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Medicament> findByNom(String nom) throws DAOException;

    /**
     * Recherche des medicaments par principe actif.
     *
     * @param principeActif le principe actif a rechercher
     * @return la liste des medicaments correspondants
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Medicament> findByPrincipeActif(String principeActif) throws DAOException;

    /**
     * Recherche les medicaments dont le stock total est en dessous du seuil minimum.
     * <p>
     * Cette methode effectue une jointure avec la table des lots pour calculer
     * le stock total de chaque medicament.
     * </p>
     *
     * @return la liste des medicaments en stock bas
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Medicament> findBelowThreshold() throws DAOException;

    /**
     * Recherche les medicaments necessitant une ordonnance.
     *
     * @return la liste des medicaments sur ordonnance
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Medicament> findRequiringPrescription() throws DAOException;

    /**
     * Recherche les medicaments actifs uniquement.
     *
     * @return la liste des medicaments actifs
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    List<Medicament> findAllActive() throws DAOException;

    /**
     * Recherche un medicament par son nom commercial exact.
     *
     * @param nomCommercial le nom commercial exact
     * @return le medicament ou null si non trouve
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    Medicament findByNomCommercialExact(String nomCommercial) throws DAOException;

    /**
     * Calcule le stock total d'un medicament (somme des quantites de tous les lots).
     *
     * @param idMedicament l'identifiant du medicament
     * @return le stock total
     * @throws DAOException si une erreur d'acces aux donnees survient
     */
    int getStockTotal(int idMedicament) throws DAOException;
}
