package com.sgpa.service;

import com.sgpa.dao.LotDAO;
import com.sgpa.dao.MedicamentDAO;
import com.sgpa.dao.VenteDAO;
import com.sgpa.dao.impl.LotDAOImpl;
import com.sgpa.dao.impl.MedicamentDAOImpl;
import com.sgpa.dao.impl.VenteDAOImpl;
import com.sgpa.dto.AlertePeremption;
import com.sgpa.dto.AlerteStock;
import com.sgpa.dto.PredictionReapprovisionnement;
import com.sgpa.exception.DAOException;
import com.sgpa.exception.ServiceException;
import com.sgpa.model.*;
import com.sgpa.utils.ExcelExporter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service d'export des donnees au format Excel (.xlsx) avec Apache POI.
 * Genere des fichiers multi-feuilles avec mise en forme conditionnelle.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VenteDAO venteDAO;
    private final MedicamentDAO medicamentDAO;
    private final LotDAO lotDAO;
    private final ConfigService configService;

    public ExcelExportService() {
        this.venteDAO = new VenteDAOImpl();
        this.medicamentDAO = new MedicamentDAOImpl();
        this.lotDAO = new LotDAOImpl();
        this.configService = new ConfigService();
    }

    private String getPharmacyName() {
        try {
            String nom = configService.getPharmacieNom();
            return nom != null && !nom.isEmpty() ? nom : "ApotiCare";
        } catch (Exception e) {
            return "ApotiCare";
        }
    }

    // =====================================================
    // EXPORT STOCK COMPLET
    // =====================================================

    /**
     * Exporte le stock complet en Excel multi-feuilles.
     * Feuilles : Couverture + Resume + Stock par lot + Stock par medicament
     *
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportStock() throws ServiceException {
        try {
            List<Lot> lots = lotDAO.findAll();
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("stock_complet");

            // Feuille 1 : Stock par lot
            String[] headers = {
                "Medicament", "N\u00b0 Lot", "Date Peremption", "Quantite",
                "Prix Achat", "Fournisseur", "Date Reception", "Statut"
            };
            List<Object[]> rows = new ArrayList<>();
            int totalStock = 0;
            int nbPerimes = 0;
            int nbPeremptionProche = 0;

            for (Lot lot : lots) {
                String nomMed = lot.getMedicament() != null
                        ? lot.getMedicament().getNomCommercial() : "ID:" + lot.getIdMedicament();
                String fournisseur = lot.getFournisseur() != null
                        ? lot.getFournisseur().getNom() : "";
                String statut = lot.isPerime() ? "PERIME" :
                               lot.isPeremptionProche() ? "Peremption proche" : "OK";

                if (lot.isPerime()) nbPerimes++;
                else if (lot.isPeremptionProche()) nbPeremptionProche++;
                totalStock += lot.getQuantiteStock();

                rows.add(new Object[]{
                    nomMed, lot.getNumeroLot(), lot.getDatePeremption(),
                    lot.getQuantiteStock(), lot.getPrixAchat(), fournisseur,
                    lot.getDateReception(), statut
                });
            }

            Sheet stockSheet = ExcelExporter.createDataSheet(wb, "Stock par lot", headers, rows);
            applyStockConditionalFormatting(wb, stockSheet, rows);

            // Feuille 2 : Resume
            Sheet resumeSheet = wb.createSheet("Resume");
            CellStyle titleStyle = ExcelExporter.createTitleStyle(wb);
            CellStyle subtitleStyle = ExcelExporter.createSubtitleStyle(wb);

            Row r0 = resumeSheet.createRow(1);
            Cell c0 = r0.createCell(1);
            c0.setCellValue("Resume du Stock");
            c0.setCellStyle(titleStyle);

            createResumeRow(resumeSheet, 3, "Nombre total de lots", lots.size(), subtitleStyle);
            createResumeRow(resumeSheet, 4, "Stock total (unites)", totalStock, subtitleStyle);
            createResumeRow(resumeSheet, 5, "Lots perimes", nbPerimes, subtitleStyle);
            createResumeRow(resumeSheet, 6, "Lots peremption proche", nbPeremptionProche, subtitleStyle);

            ExcelExporter.autoSizeColumns(resumeSheet, 3);

            // Couverture
            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Rapport de Stock Complet", getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export Excel du stock", e);
            throw new ServiceException("Erreur lors de l'export du stock", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT HISTORIQUE VENTES
    // =====================================================

    /**
     * Exporte l'historique des ventes en Excel.
     * Feuilles : Couverture + Resume + Ventes
     *
     * @param dateDebut date de debut
     * @param dateFin   date de fin
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportVentes(LocalDate dateDebut, LocalDate dateFin) throws ServiceException {
        try {
            List<Vente> ventes = venteDAO.findByDateRange(dateDebut, dateFin);
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("ventes");

            // Feuille Ventes
            String[] headers = {
                "N\u00b0 Vente", "Date", "Vendeur", "Nb Articles",
                "Montant Total", "Ordonnance", "N\u00b0 Ordonnance"
            };
            List<Object[]> rows = new ArrayList<>();
            BigDecimal totalCA = BigDecimal.ZERO;

            for (Vente v : ventes) {
                String vendeur = v.getUtilisateur() != null
                        ? v.getUtilisateur().getNomComplet() : "ID:" + v.getIdUtilisateur();
                totalCA = totalCA.add(v.getMontantTotal() != null ? v.getMontantTotal() : BigDecimal.ZERO);

                rows.add(new Object[]{
                    v.getIdVente(), v.getDateVente(), vendeur,
                    v.getNombreArticles(), v.getMontantTotal(),
                    v.isEstSurOrdonnance() ? "Oui" : "Non",
                    v.getNumeroOrdonnance() != null ? v.getNumeroOrdonnance() : ""
                });
            }

            ExcelExporter.createDataSheet(wb, "Ventes", headers, rows);

            // Resume
            Sheet resumeSheet = wb.createSheet("Resume");
            CellStyle titleStyle = ExcelExporter.createTitleStyle(wb);
            CellStyle subtitleStyle = ExcelExporter.createSubtitleStyle(wb);
            CellStyle moneyStyle = ExcelExporter.createMoneyStyle(wb);

            Row r0 = resumeSheet.createRow(1);
            Cell c0 = r0.createCell(1);
            c0.setCellValue("Resume des Ventes");
            c0.setCellStyle(titleStyle);

            Row r2 = resumeSheet.createRow(3);
            r2.createCell(1).setCellValue("Periode");
            r2.createCell(2).setCellValue(dateDebut + " au " + dateFin);

            createResumeRow(resumeSheet, 4, "Nombre de ventes", ventes.size(), subtitleStyle);

            Row r5 = resumeSheet.createRow(5);
            r5.createCell(1).setCellValue("Chiffre d'affaires total");
            Cell caCell = r5.createCell(2);
            caCell.setCellValue(totalCA.doubleValue());
            caCell.setCellStyle(moneyStyle);

            if (!ventes.isEmpty()) {
                BigDecimal panierMoyen = totalCA.divide(BigDecimal.valueOf(ventes.size()), 2, java.math.RoundingMode.HALF_UP);
                Row r6 = resumeSheet.createRow(6);
                r6.createCell(1).setCellValue("Panier moyen");
                Cell pmCell = r6.createCell(2);
                pmCell.setCellValue(panierMoyen.doubleValue());
                pmCell.setCellStyle(moneyStyle);
            }

            ExcelExporter.autoSizeColumns(resumeSheet, 3);

            // Couverture
            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Historique des Ventes", getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export Excel des ventes", e);
            throw new ServiceException("Erreur lors de l'export des ventes", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT MEDICAMENTS
    // =====================================================

    /**
     * Exporte le catalogue des medicaments en Excel.
     *
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportMedicaments() throws ServiceException {
        try {
            List<Medicament> medicaments = medicamentDAO.findAll();
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("medicaments");

            String[] headers = {
                "ID", "Nom Commercial", "Principe Actif", "Forme", "Dosage",
                "Prix Public", "Seuil Min", "Ordonnance", "Actif"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Medicament m : medicaments) {
                rows.add(new Object[]{
                    m.getIdMedicament(), m.getNomCommercial(), m.getPrincipeActif(),
                    m.getFormeGalenique(), m.getDosage(), m.getPrixPublic(),
                    m.getSeuilMin(),
                    m.isNecessiteOrdonnance() ? "Oui" : "Non",
                    m.isActif() ? "Oui" : "Non"
                });
            }

            ExcelExporter.createDataSheet(wb, "Medicaments", headers, rows);

            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Catalogue des Medicaments", getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (DAOException e) {
            logger.error("Erreur lors de l'export Excel des medicaments", e);
            throw new ServiceException("Erreur lors de l'export des medicaments", e);
        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT COMMANDES
    // =====================================================

    /**
     * Exporte les commandes en Excel multi-feuilles.
     * Feuilles : Couverture + Liste commandes + Detail lignes
     *
     * @param commandes la liste des commandes
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportCommandes(List<Commande> commandes) throws ServiceException {
        try {
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("commandes");

            // Feuille 1 : Liste des commandes
            String[] headers = {
                "N\u00b0 Commande", "Date Creation", "Fournisseur", "Nb Articles",
                "Statut", "Date Reception", "Notes"
            };

            List<Object[]> rows = new ArrayList<>();
            for (Commande c : commandes) {
                String fournisseur = c.getFournisseur() != null
                        ? c.getFournisseur().getNom() : "ID:" + c.getIdFournisseur();
                rows.add(new Object[]{
                    c.getIdCommande(), c.getDateCreation(), fournisseur,
                    c.getNombreArticlesCommandes(),
                    c.getStatut() != null ? c.getStatut().getLibelle() : "",
                    c.getDateReception(),
                    c.getNotes() != null ? c.getNotes() : ""
                });
            }

            ExcelExporter.createDataSheet(wb, "Commandes", headers, rows);

            // Feuille 2 : Detail des lignes
            String[] headersLignes = {
                "N\u00b0 Commande", "Medicament", "Qte Commandee", "Qte Recue",
                "Prix Unitaire", "Montant"
            };

            List<Object[]> lignesRows = new ArrayList<>();
            for (Commande c : commandes) {
                if (c.getLignesCommande() != null) {
                    for (LigneCommande lc : c.getLignesCommande()) {
                        String nomMed = lc.getMedicament() != null
                                ? lc.getMedicament().getNomCommercial()
                                : "ID:" + lc.getIdMedicament();
                        lignesRows.add(new Object[]{
                            c.getIdCommande(), nomMed, lc.getQuantiteCommandee(),
                            lc.getQuantiteRecue(), lc.getPrixUnitaire(), lc.getMontantLigne()
                        });
                    }
                }
            }

            if (!lignesRows.isEmpty()) {
                ExcelExporter.createDataSheet(wb, "Detail lignes", headersLignes, lignesRows);
            }

            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Commandes Fournisseurs", getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel commandes", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT INVENTAIRE
    // =====================================================

    /**
     * Exporte une session d'inventaire en Excel.
     * Feuilles : Couverture + Info session + Comptages avec ecarts
     *
     * @param session   la session d'inventaire
     * @param comptages la liste des comptages
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportInventaire(SessionInventaire session,
                                     List<ComptageInventaire> comptages) throws ServiceException {
        try {
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("inventaire_" + session.getIdSession());

            // Feuille 1 : Comptages
            String[] headers = {
                "Medicament", "N\u00b0 Lot", "Stock Theorique", "Stock Physique",
                "Ecart", "Ecart %", "Motif", "Commentaire"
            };

            List<Object[]> rows = new ArrayList<>();
            int totalEcarts = 0;
            int ecartsNegatifs = 0;
            int ecartsPositifs = 0;

            for (ComptageInventaire c : comptages) {
                if (c.getEcart() != 0) totalEcarts++;
                if (c.getEcart() < 0) ecartsNegatifs++;
                if (c.getEcart() > 0) ecartsPositifs++;

                rows.add(new Object[]{
                    c.getNomMedicament(), c.getNumeroLot(),
                    c.getQuantiteTheorique(), c.getQuantitePhysique(),
                    c.getEcart(),
                    String.format("%.1f%%", c.getEcartPourcentage()),
                    c.getMotifLibelle(),
                    c.getCommentaire() != null ? c.getCommentaire() : ""
                });
            }

            Sheet comptageSheet = ExcelExporter.createDataSheet(wb, "Comptages", headers, rows);
            applyInventaireConditionalFormatting(wb, comptageSheet, rows);

            // Feuille 2 : Info session
            Sheet infoSheet = wb.createSheet("Info Session");
            CellStyle titleStyle = ExcelExporter.createTitleStyle(wb);
            CellStyle subtitleStyle = ExcelExporter.createSubtitleStyle(wb);

            Row r0 = infoSheet.createRow(1);
            Cell c0 = r0.createCell(1);
            c0.setCellValue("Session d'Inventaire #" + session.getIdSession());
            c0.setCellStyle(titleStyle);

            createInfoRow(infoSheet, 3, "Debut", session.getDateDebut() != null
                    ? session.getDateDebut().format(DATE_FMT) : "-", subtitleStyle);
            createInfoRow(infoSheet, 4, "Fin", session.getDateFin() != null
                    ? session.getDateFin().format(DATE_FMT) : "En cours", subtitleStyle);
            createInfoRow(infoSheet, 5, "Statut", session.getStatut() != null
                    ? session.getStatut().toString() : "-", subtitleStyle);
            createInfoRow(infoSheet, 6, "Operateur", session.getNomUtilisateur(), subtitleStyle);
            createInfoRow(infoSheet, 7, "Notes", session.getNotes() != null
                    ? session.getNotes() : "-", subtitleStyle);

            createResumeRow(infoSheet, 9, "Total comptages", comptages.size(), subtitleStyle);
            createResumeRow(infoSheet, 10, "Avec ecarts", totalEcarts, subtitleStyle);
            createResumeRow(infoSheet, 11, "Ecarts negatifs (manque)", ecartsNegatifs, subtitleStyle);
            createResumeRow(infoSheet, 12, "Ecarts positifs (surplus)", ecartsPositifs, subtitleStyle);

            ExcelExporter.autoSizeColumns(infoSheet, 3);

            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Inventaire #" + session.getIdSession(),
                    getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel inventaire", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT PREDICTIONS
    // =====================================================

    /**
     * Exporte les predictions de reapprovisionnement en Excel.
     * Feuilles : Couverture + Predictions avec niveaux d'urgence
     *
     * @param predictions la liste des predictions
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportPredictions(List<PredictionReapprovisionnement> predictions) throws ServiceException {
        try {
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("predictions");

            String[] headers = {
                "Medicament", "Stock Actuel", "Stock Vendable", "Conso/Jour",
                "Jours Restants", "Date Rupture Prevue", "Qte Suggeree",
                "Seuil Min", "Urgence"
            };

            List<Object[]> rows = new ArrayList<>();
            int nbRupture = 0, nbCritique = 0, nbUrgent = 0;

            for (PredictionReapprovisionnement p : predictions) {
                if (PredictionReapprovisionnement.NIVEAU_RUPTURE.equals(p.getNiveauUrgence())) nbRupture++;
                else if (PredictionReapprovisionnement.NIVEAU_CRITIQUE.equals(p.getNiveauUrgence())) nbCritique++;
                else if (PredictionReapprovisionnement.NIVEAU_URGENT.equals(p.getNiveauUrgence())) nbUrgent++;

                rows.add(new Object[]{
                    p.getNomMedicament(), p.getStockActuel(), p.getStockVendable(),
                    Math.round(p.getConsommationJournaliere() * 100.0) / 100.0,
                    p.getJoursRestantsFormate(), p.getDateRupturePrevue(),
                    p.getQuantiteSuggeree(), p.getSeuilMin(), p.getNiveauUrgence()
                });
            }

            Sheet predSheet = ExcelExporter.createDataSheet(wb, "Predictions", headers, rows);
            applyPredictionConditionalFormatting(wb, predSheet, rows);

            // Resume
            Sheet resumeSheet = wb.createSheet("Resume");
            CellStyle titleStyle = ExcelExporter.createTitleStyle(wb);
            CellStyle subtitleStyle = ExcelExporter.createSubtitleStyle(wb);

            Row r0 = resumeSheet.createRow(1);
            Cell c0 = r0.createCell(1);
            c0.setCellValue("Resume des Predictions");
            c0.setCellStyle(titleStyle);

            createResumeRow(resumeSheet, 3, "Total medicaments analyses", predictions.size(), subtitleStyle);
            createResumeRow(resumeSheet, 4, "En RUPTURE", nbRupture, subtitleStyle);
            createResumeRow(resumeSheet, 5, "CRITIQUE (< 7 jours)", nbCritique, subtitleStyle);
            createResumeRow(resumeSheet, 6, "URGENT (< 14 jours)", nbUrgent, subtitleStyle);

            ExcelExporter.autoSizeColumns(resumeSheet, 3);

            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Predictions de Reapprovisionnement",
                    getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel predictions", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT RETOURS
    // =====================================================

    /**
     * Exporte l'historique des retours en Excel.
     *
     * @param retours la liste des retours
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportRetours(List<Retour> retours) throws ServiceException {
        try {
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("retours");

            String[] headers = {
                "N\u00b0 Retour", "Date", "N\u00b0 Vente", "Medicament", "N\u00b0 Lot",
                "Quantite", "Motif", "Reintegre", "Commentaire"
            };

            List<Object[]> rows = new ArrayList<>();
            int totalReintegres = 0;

            for (Retour r : retours) {
                if (r.isReintegre()) totalReintegres++;
                rows.add(new Object[]{
                    r.getIdRetour(), r.getDateRetour(), r.getIdVente(),
                    r.getNomMedicament(), r.getNumeroLot(), r.getQuantite(),
                    r.getMotif() != null ? r.getMotif() : "",
                    r.isReintegre() ? "Oui" : "Non",
                    r.getCommentaire() != null ? r.getCommentaire() : ""
                });
            }

            ExcelExporter.createDataSheet(wb, "Retours", headers, rows);

            // Resume
            Sheet resumeSheet = wb.createSheet("Resume");
            CellStyle titleStyle = ExcelExporter.createTitleStyle(wb);
            CellStyle subtitleStyle = ExcelExporter.createSubtitleStyle(wb);

            Row r0 = resumeSheet.createRow(1);
            Cell c0 = r0.createCell(1);
            c0.setCellValue("Resume des Retours");
            c0.setCellStyle(titleStyle);

            createResumeRow(resumeSheet, 3, "Total retours", retours.size(), subtitleStyle);
            createResumeRow(resumeSheet, 4, "Reintegres au stock", totalReintegres, subtitleStyle);
            createResumeRow(resumeSheet, 5, "Non reintegres", retours.size() - totalReintegres, subtitleStyle);

            ExcelExporter.autoSizeColumns(resumeSheet, 3);

            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Historique des Retours", getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel retours", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // EXPORT ALERTES
    // =====================================================

    /**
     * Exporte les alertes (stock bas + peremption + perimes) en Excel.
     *
     * @param alertesStock      alertes de stock bas
     * @param alertesPeremption alertes de peremption proche
     * @param lotsPerimes       lots perimes
     * @return le chemin du fichier genere
     * @throws ServiceException si une erreur survient
     */
    public String exportAlertes(List<AlerteStock> alertesStock,
                                  List<AlertePeremption> alertesPeremption,
                                  List<AlertePeremption> lotsPerimes) throws ServiceException {
        try {
            XSSFWorkbook wb = ExcelExporter.createWorkbook();
            String filePath = ExcelExporter.generateFilePath("alertes");

            // Feuille 1 : Stock bas
            String[] headersStock = {
                "Medicament", "Stock Actuel", "Seuil Min", "Deficit", "Criticite %"
            };
            List<Object[]> rowsStock = new ArrayList<>();
            for (AlerteStock a : alertesStock) {
                rowsStock.add(new Object[]{
                    a.getNomMedicament(), a.getStockActuel(), a.getSeuilMin(),
                    a.getDeficit(), a.getNiveauCriticite() + "%"
                });
            }
            ExcelExporter.createDataSheet(wb, "Stock bas", headersStock, rowsStock);

            // Feuille 2 : Peremption proche
            String[] headersPer = {
                "Medicament", "N\u00b0 Lot", "Date Peremption", "Jours Restants",
                "Quantite", "Urgence"
            };
            List<Object[]> rowsPer = new ArrayList<>();
            for (AlertePeremption a : alertesPeremption) {
                rowsPer.add(new Object[]{
                    a.getNomMedicament(), a.getNumeroLot(), a.getDatePeremption(),
                    a.getJoursRestants(), a.getQuantiteStock(), a.getNiveauUrgence()
                });
            }
            ExcelExporter.createDataSheet(wb, "Peremption proche", headersPer, rowsPer);

            // Feuille 3 : Lots perimes
            String[] headersPerimes = {
                "Medicament", "N\u00b0 Lot", "Date Peremption", "Jours depasses",
                "Quantite"
            };
            List<Object[]> rowsPerimes = new ArrayList<>();
            for (AlertePeremption a : lotsPerimes) {
                rowsPerimes.add(new Object[]{
                    a.getNomMedicament(), a.getNumeroLot(), a.getDatePeremption(),
                    Math.abs(a.getJoursRestants()), a.getQuantiteStock()
                });
            }
            ExcelExporter.createDataSheet(wb, "Lots perimes", headersPerimes, rowsPerimes);

            // Resume
            Sheet resumeSheet = wb.createSheet("Resume");
            CellStyle titleStyle = ExcelExporter.createTitleStyle(wb);
            CellStyle subtitleStyle = ExcelExporter.createSubtitleStyle(wb);

            Row r0 = resumeSheet.createRow(1);
            Cell c0 = r0.createCell(1);
            c0.setCellValue("Resume des Alertes");
            c0.setCellStyle(titleStyle);

            createResumeRow(resumeSheet, 3, "Medicaments en stock bas", alertesStock.size(), subtitleStyle);
            createResumeRow(resumeSheet, 4, "Lots peremption proche", alertesPeremption.size(), subtitleStyle);
            createResumeRow(resumeSheet, 5, "Lots perimes", lotsPerimes.size(), subtitleStyle);

            ExcelExporter.autoSizeColumns(resumeSheet, 3);

            String date = LocalDateTime.now().format(DATE_FMT);
            ExcelExporter.addCoverSheet(wb, "Rapport des Alertes", getPharmacyName(), date);

            return ExcelExporter.save(wb, filePath);

        } catch (IOException e) {
            logger.error("Erreur d'ecriture du fichier Excel alertes", e);
            throw new ServiceException("Erreur lors de l'ecriture du fichier", e);
        }
    }

    // =====================================================
    // METHODES UTILITAIRES PRIVEES
    // =====================================================

    private void createResumeRow(Sheet sheet, int rowNum, String label, int value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(1);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(2).setCellValue(value);
    }

    private void createInfoRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(1);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        row.createCell(2).setCellValue(value != null ? value : "-");
    }

    /**
     * Applique la mise en forme conditionnelle pour le stock.
     * Rouge = perime, Orange = peremption proche
     */
    private void applyStockConditionalFormatting(XSSFWorkbook wb, Sheet sheet, List<Object[]> rows) {
        CellStyle redStyle = ExcelExporter.createRedBgStyle(wb);
        CellStyle orangeStyle = ExcelExporter.createOrangeBgStyle(wb);

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String statut = row.length > 7 ? String.valueOf(row[7]) : "";
            if ("PERIME".equals(statut)) {
                Row sheetRow = sheet.getRow(i + 1);
                if (sheetRow != null) {
                    for (int c = 0; c < row.length; c++) {
                        Cell cell = sheetRow.getCell(c);
                        if (cell != null) cell.setCellStyle(redStyle);
                    }
                }
            } else if ("Peremption proche".equals(statut)) {
                Row sheetRow = sheet.getRow(i + 1);
                if (sheetRow != null) {
                    for (int c = 0; c < row.length; c++) {
                        Cell cell = sheetRow.getCell(c);
                        if (cell != null) cell.setCellStyle(orangeStyle);
                    }
                }
            }
        }
    }

    /**
     * Applique la mise en forme conditionnelle pour l'inventaire.
     * Rouge = ecart negatif, Vert (jaune) = ecart positif
     */
    private void applyInventaireConditionalFormatting(XSSFWorkbook wb, Sheet sheet, List<Object[]> rows) {
        CellStyle redStyle = ExcelExporter.createRedBgStyle(wb);
        CellStyle yellowStyle = ExcelExporter.createYellowBgStyle(wb);

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            int ecart = row.length > 4 && row[4] instanceof Integer ? (Integer) row[4] : 0;
            if (ecart < 0) {
                Row sheetRow = sheet.getRow(i + 1);
                if (sheetRow != null) {
                    for (int c = 0; c < row.length; c++) {
                        Cell cell = sheetRow.getCell(c);
                        if (cell != null) cell.setCellStyle(redStyle);
                    }
                }
            } else if (ecart > 0) {
                Row sheetRow = sheet.getRow(i + 1);
                if (sheetRow != null) {
                    for (int c = 0; c < row.length; c++) {
                        Cell cell = sheetRow.getCell(c);
                        if (cell != null) cell.setCellStyle(yellowStyle);
                    }
                }
            }
        }
    }

    /**
     * Applique la mise en forme conditionnelle pour les predictions.
     * Rouge = RUPTURE/CRITIQUE, Orange = URGENT
     */
    private void applyPredictionConditionalFormatting(XSSFWorkbook wb, Sheet sheet, List<Object[]> rows) {
        CellStyle redStyle = ExcelExporter.createRedBgStyle(wb);
        CellStyle orangeStyle = ExcelExporter.createOrangeBgStyle(wb);
        CellStyle yellowStyle = ExcelExporter.createYellowBgStyle(wb);

        for (int i = 0; i < rows.size(); i++) {
            Object[] row = rows.get(i);
            String urgence = row.length > 8 ? String.valueOf(row[8]) : "";
            CellStyle style = null;

            if (PredictionReapprovisionnement.NIVEAU_RUPTURE.equals(urgence)
                    || PredictionReapprovisionnement.NIVEAU_CRITIQUE.equals(urgence)) {
                style = redStyle;
            } else if (PredictionReapprovisionnement.NIVEAU_URGENT.equals(urgence)) {
                style = orangeStyle;
            } else if (PredictionReapprovisionnement.NIVEAU_ATTENTION.equals(urgence)) {
                style = yellowStyle;
            }

            if (style != null) {
                Row sheetRow = sheet.getRow(i + 1);
                if (sheetRow != null) {
                    for (int c = 0; c < row.length; c++) {
                        Cell cell = sheetRow.getCell(c);
                        if (cell != null) cell.setCellStyle(style);
                    }
                }
            }
        }
    }

    /**
     * Retourne le repertoire d'export.
     */
    public String getExportDir() {
        return ExcelExporter.getOutputDir();
    }
}
