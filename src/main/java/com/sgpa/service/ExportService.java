package com.sgpa.service;

import com.sgpa.dao.LotDAO;
import com.sgpa.dao.MedicamentDAO;
import com.sgpa.dao.VenteDAO;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.dao.impl.MedicamentDAOImpl;
import com.sgpa.dao.impl.VenteDAOImpl;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.model.AuditLog;
import com.sgpa.model.Lot;
import com.sgpa.model.Medicament;
import com.sgpa.model.Vente;
import com.sgpa.utils.CSVExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'export des donnees au format CSV.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    private final VenteDAO venteDAO;
    private final MedicamentDAO medicamentDAO;
    private final LotDAO lotDAO;
    private final AuditService auditService;

    public ExportService() {
        this.venteDAO = new VenteDAOImpl();
        this.medicamentDAO = new MedicamentDAOImpl();
        this.lotDAO = new LotDAOImpl();
        this.auditService = new AuditService();
    }

    // =====================================================
    // EXPORT VENTES
    // =====================================================

    /**
     * Exporte l'historique des ventes pour une periode.
     *
     * @param dateDebut date de debut
     * @param dateFin   date de fin
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportVentes(LocalDate dateDebut, LocalDate dateFin) throws ServiceException {
        try {
            List<Vente> ventes = venteDAO.findByDateRange(dateDebut, dateFin);
            String filePath = CSVExporter.generateFilePath("ventes");

            String[] headers = {
                "N° Vente", "Date", "Vendeur", "Nb Articles", "Montant Total",
                "Sur Ordonnance", "N° Ordonnance"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Vente v : ventes) {
                rows.add(new Object[]{
                    v.getIdVente(),
                    v.getDateVente(),
                    v.getUtilisateur() != null ? v.getUtilisateur().getNomComplet() : "ID:" + v.getIdUtilisateur(),
                    v.getNombreArticles(),
                    v.getMontantTotal(),
                    v.isEstSurOrdonnance() ? "Oui" : "Non",
                    v.getNumeroOrdonnance() != null ? v.getNumeroOrdonnance() : ""
                });
            }

            return CSVExporter.export(filePath, headers, rows);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export des ventes", e);
            throw new ServiceException("Erreur lors de l'export des ventes", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier CSV", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    /**
     * Exporte toutes les ventes.
     *
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportAllVentes() throws ServiceException {
        try {
            List<Vente> ventes = venteDAO.findAll();
            String filePath = CSVExporter.generateFilePath("ventes_complet");

            String[] headers = {
                "N° Vente", "Date", "Vendeur", "Nb Articles", "Montant Total",
                "Sur Ordonnance", "N° Ordonnance"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Vente v : ventes) {
                rows.add(new Object[]{
                    v.getIdVente(),
                    v.getDateVente(),
                    v.getUtilisateur() != null ? v.getUtilisateur().getNomComplet() : "ID:" + v.getIdUtilisateur(),
                    v.getNombreArticles(),
                    v.getMontantTotal(),
                    v.isEstSurOrdonnance() ? "Oui" : "Non",
                    v.getNumeroOrdonnance() != null ? v.getNumeroOrdonnance() : ""
                });
            }

            return CSVExporter.export(filePath, headers, rows);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export des ventes", e);
            throw new ServiceException("Erreur lors de l'export des ventes", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier CSV", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT STOCK
    // =====================================================

    /**
     * Exporte l'inventaire du stock.
     *
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportStock() throws ServiceException {
        try {
            List<Lot> lots = lotDAO.findAll();
            String filePath = CSVExporter.generateFilePath("stock");

            String[] headers = {
                "Medicament", "N° Lot", "Date Peremption", "Quantite", "Prix Achat",
                "Fournisseur", "Date Reception", "Statut"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Lot lot : lots) {
                String nomMed = lot.getMedicament() != null
                        ? lot.getMedicament().getNomCommercial()
                        : "ID:" + lot.getIdMedicament();
                String fournisseur = lot.getFournisseur() != null
                        ? lot.getFournisseur().getNom()
                        : "";
                String statut = lot.isPerime() ? "PERIME" :
                               lot.isPeremptionProche() ? "Peremption proche" : "OK";

                rows.add(new Object[]{
                    nomMed,
                    lot.getNumeroLot(),
                    lot.getDatePeremption(),
                    lot.getQuantiteStock(),
                    lot.getPrixAchat(),
                    fournisseur,
                    lot.getDateReception(),
                    statut
                });
            }

            return CSVExporter.export(filePath, headers, rows);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export du stock", e);
            throw new ServiceException("Erreur lors de l'export du stock", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier CSV", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    /**
     * Exporte le stock pour un medicament specifique.
     *
     * @param idMedicament l'ID du medicament
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportStockByMedicament(int idMedicament) throws ServiceException {
        try {
            List<Lot> lots = lotDAO.findByMedicamentIdSortedByExpiration(idMedicament);
            Medicament med = medicamentDAO.findById(idMedicament).orElse(null);
            String nomMed = med != null ? med.getNomCommercial() : "medicament_" + idMedicament;

            String filePath = CSVExporter.generateFilePath("stock_" + nomMed.replace(" ", "_"));

            String[] headers = {
                "N° Lot", "Date Peremption", "Quantite", "Prix Achat",
                "Fournisseur", "Date Reception", "Statut"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Lot lot : lots) {
                String fournisseur = lot.getFournisseur() != null
                        ? lot.getFournisseur().getNom()
                        : "";
                String statut = lot.isPerime() ? "PERIME" :
                               lot.isPeremptionProche() ? "Peremption proche" : "OK";

                rows.add(new Object[]{
                    lot.getNumeroLot(),
                    lot.getDatePeremption(),
                    lot.getQuantiteStock(),
                    lot.getPrixAchat(),
                    fournisseur,
                    lot.getDateReception(),
                    statut
                });
            }

            return CSVExporter.export(filePath, headers, rows);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export du stock", e);
            throw new ServiceException("Erreur lors de l'export du stock", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier CSV", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT MEDICAMENTS
    // =====================================================

    /**
     * Exporte le catalogue des medicaments.
     *
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportMedicaments() throws ServiceException {
        try {
            List<Medicament> medicaments = medicamentDAO.findAll();
            String filePath = CSVExporter.generateFilePath("medicaments");

            String[] headers = {
                "ID", "Nom Commercial", "Principe Actif", "Forme", "Dosage",
                "Prix Public", "Seuil Min", "Ordonnance", "Actif", "Description"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Medicament m : medicaments) {
                rows.add(new Object[]{
                    m.getIdMedicament(),
                    m.getNomCommercial(),
                    m.getPrincipeActif(),
                    m.getFormeGalenique(),
                    m.getDosage(),
                    m.getPrixPublic(),
                    m.getSeuilMin(),
                    m.isNecessiteOrdonnance() ? "Oui" : "Non",
                    m.isActif() ? "Oui" : "Non",
                    m.getDescription() != null ? m.getDescription() : ""
                });
            }

            return CSVExporter.export(filePath, headers, rows);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export des medicaments", e);
            throw new ServiceException("Erreur lors de l'export des medicaments", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier CSV", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT AUDIT
    // =====================================================

    /**
     * Exporte le journal d'audit.
     *
     * @param logs les entrees d'audit a exporter
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportAudit(List<AuditLog> logs) throws ServiceException {
        try {
            String filePath = CSVExporter.generateFilePath("audit");

            String[] headers = {
                "Date", "Utilisateur", "Type Action", "Entite", "ID Entite", "Description"
            };

            List<Object[]> rows = new ArrayList<>();
            for (AuditLog log : logs) {
                rows.add(new Object[]{
                    log.getDateAction(),
                    log.getNomUtilisateur() != null ? log.getNomUtilisateur() : "Systeme",
                    log.getTypeAction() != null ? log.getTypeAction().getLibelle() : "",
                    log.getEntite() != null ? log.getEntite() : "",
                    log.getIdEntite() != null ? log.getIdEntite() : "",
                    log.getDescription() != null ? log.getDescription() : ""
                });
            }

            return CSVExporter.export(filePath, headers, rows);

        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier CSV", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    /**
     * Retourne le repertoire d'export.
     *
     * @return le chemin du repertoire
     */
    public String getExportDir() {
        return CSVExporter.getOutputDir();
    }
}
