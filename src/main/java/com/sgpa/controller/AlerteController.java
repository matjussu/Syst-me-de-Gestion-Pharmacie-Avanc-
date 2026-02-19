package com.sgpa.controller;

import com.sgpa.dto.AlertePeremption;
import com.sgpa.dto.AlerteStock;
import com.sgpa.model.Lot;
import com.sgpa.service.AlerteService;
import com.sgpa.service.ExcelExportService;
import com.sgpa.service.ExportService;
import com.sgpa.service.RapportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controleur pour l'ecran des alertes.
 * Gere les onglets stock bas, peremption proche et lots perimes.
 *
 * @author SGPA Team
 * @version 2.0
 */
public class AlerteController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AlerteController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Toolbar
    @FXML private TextField searchField;
    @FXML private HBox exportGroup;
    @FXML private Button btnExportStockBas;
    @FXML private Button btnExportPeremption;
    @FXML private Button btnExportPerimes;

    // Stats
    @FXML private Label lblTotalAlertes;

    @FXML private TabPane tabPane;

    // Stock bas
    @FXML private Label lblStockBasCount;
    @FXML private TableView<AlerteStock> tableStockBas;
    @FXML private TableColumn<AlerteStock, String> colStockMedicament;
    @FXML private TableColumn<AlerteStock, String> colStockActuel;
    @FXML private TableColumn<AlerteStock, String> colSeuilMin;
    @FXML private TableColumn<AlerteStock, String> colStockDeficit;
    @FXML private TableColumn<AlerteStock, String> colStockNiveau;
    @FXML private TableColumn<AlerteStock, Void> colStockAction;

    // Peremption proche
    @FXML private Label lblPeremptionCount;
    @FXML private TableView<AlertePeremption> tablePeremption;
    @FXML private TableColumn<AlertePeremption, String> colPeremptionMedicament;
    @FXML private TableColumn<AlertePeremption, String> colPeremptionLot;
    @FXML private TableColumn<AlertePeremption, String> colPeremptionDate;
    @FXML private TableColumn<AlertePeremption, String> colPeremptionJours;
    @FXML private TableColumn<AlertePeremption, String> colPeremptionQuantite;
    @FXML private TableColumn<AlertePeremption, String> colPeremptionUrgence;

    // Lots perimes
    @FXML private Label lblPerimesCount;
    @FXML private TableView<Lot> tablePerimes;
    @FXML private TableColumn<Lot, String> colPerimesMedicament;
    @FXML private TableColumn<Lot, String> colPerimesLot;
    @FXML private TableColumn<Lot, String> colPerimesDate;
    @FXML private TableColumn<Lot, String> colPerimesQuantite;
    @FXML private TableColumn<Lot, String> colPerimesRetard;

    private final AlerteService alerteService;
    private final RapportService rapportService;
    private final ExportService exportService;
    private final ExcelExportService excelExportService;
    private final ObservableList<AlerteStock> stockBasData = FXCollections.observableArrayList();
    private final ObservableList<AlertePeremption> peremptionData = FXCollections.observableArrayList();
    private final ObservableList<Lot> perimesData = FXCollections.observableArrayList();

    private FilteredList<AlerteStock> filteredStockBas;
    private FilteredList<AlertePeremption> filteredPeremption;
    private FilteredList<Lot> filteredPerimes;

    public AlerteController() {
        this.alerteService = new AlerteService();
        this.rapportService = new RapportService();
        this.exportService = new ExportService();
        this.excelExportService = new ExcelExportService();
    }

    @Override
    protected void onUserSet() {
        if (currentUser == null || !currentUser.isAdmin()) {
            if (exportGroup != null) {
                exportGroup.setVisible(false);
                exportGroup.setManaged(false);
            }
            if (btnExportStockBas != null) {
                btnExportStockBas.setVisible(false);
                btnExportStockBas.setManaged(false);
            }
            if (btnExportPeremption != null) {
                btnExportPeremption.setVisible(false);
                btnExportPeremption.setManaged(false);
            }
            if (btnExportPerimes != null) {
                btnExportPerimes.setVisible(false);
                btnExportPerimes.setManaged(false);
            }
        }
    }

    @FXML
    public void initialize() {
        setupStockBasTable();
        setupPeremptionTable();
        setupPerimesTable();
        setupSearchField();
        setupResponsiveTable(tableStockBas);
        setupResponsiveTable(tablePeremption);
        setupResponsiveTable(tablePerimes);
        loadAllAlertes();
    }

    private void setupSearchField() {
        filteredStockBas = new FilteredList<>(stockBasData, p -> true);
        filteredPeremption = new FilteredList<>(peremptionData, p -> true);
        filteredPerimes = new FilteredList<>(perimesData, p -> true);

        tableStockBas.setItems(filteredStockBas);
        tablePeremption.setItems(filteredPeremption);
        tablePerimes.setItems(filteredPerimes);
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";

        filteredStockBas.setPredicate(a ->
            query.isEmpty() || a.getNomMedicament().toLowerCase().contains(query)
        );
        filteredPeremption.setPredicate(a ->
            query.isEmpty() ||
            a.getNomMedicament().toLowerCase().contains(query) ||
            a.getNumeroLot().toLowerCase().contains(query)
        );
        filteredPerimes.setPredicate(lot -> {
            String nomMed = lot.getMedicament() != null
                ? lot.getMedicament().getNomCommercial()
                : "medicament #" + lot.getIdMedicament();
            return query.isEmpty() ||
                   nomMed.toLowerCase().contains(query) ||
                   lot.getNumeroLot().toLowerCase().contains(query);
        });
    }

    private void setupStockBasTable() {
        colStockMedicament.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomMedicament()));
        colStockActuel.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getStockActuel())));
        colSeuilMin.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getSeuilMin())));
        colStockDeficit.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getDeficit())));

        // Colorer le deficit en rouge
        colStockDeficit.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("-" + item);
                    setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                }
            }
        });

        // Colonne Niveau avec ProgressBar
        colStockNiveau.setCellValueFactory(data ->
            new SimpleStringProperty(String.valueOf(data.getValue().getNiveauCriticite()))
        );
        colStockNiveau.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            {
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setPrefHeight(10);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    double pct = Integer.parseInt(item) / 100.0;
                    bar.setProgress(Math.min(pct, 1.0));
                    bar.getStyleClass().removeAll("stock-progress-low", "stock-progress-medium", "inventaire-progress");
                    if (pct < 0.25) {
                        bar.getStyleClass().add("stock-progress-low");
                    } else if (pct < 0.5) {
                        bar.getStyleClass().add("stock-progress-medium");
                    } else {
                        bar.getStyleClass().add("inventaire-progress");
                    }
                    setGraphic(bar);
                    setText(null);
                }
            }
        });

        // Colonne Action avec bouton Commander
        colStockAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnCommander = new Button("Commander");
            {
                btnCommander.getStyleClass().add("commander-button");
                FontIcon icon = new FontIcon("fas-cart-plus");
                icon.setIconSize(12);
                icon.setStyle("-fx-icon-color: white;");
                btnCommander.setGraphic(icon);
                btnCommander.setOnAction(e -> {
                    AlerteStock item = getTableView().getItems().get(getIndex());
                    if (dashboardController != null) {
                        dashboardController.navigateToCommandeWithMedicament(item.getNomMedicament());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnCommander);
            }
        });

        // Row factory pour colorer les lignes selon le niveau de criticite
        tableStockBas.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AlerteStock item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("alert-row-critique", "alert-row-urgent", "alert-row-attention");
                if (!empty && item != null) {
                    int niveau = item.getNiveauCriticite();
                    if (niveau == 0) {
                        getStyleClass().add("alert-row-critique");
                    } else if (niveau < 50) {
                        getStyleClass().add("alert-row-urgent");
                    } else {
                        getStyleClass().add("alert-row-attention");
                    }
                }
            }
        });
    }

    private void setupPeremptionTable() {
        colPeremptionMedicament.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNomMedicament()));
        colPeremptionLot.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumeroLot()));
        colPeremptionDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDatePeremption().format(DATE_FORMAT)));
        colPeremptionJours.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getJoursRestants())));
        colPeremptionQuantite.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantiteStock())));
        colPeremptionUrgence.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNiveauUrgence()));

        // Colorer les jours restants
        colPeremptionJours.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    long jours = Long.parseLong(item);
                    setText(item + " j");
                    if (jours < 0) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-style: italic;");
                    } else if (jours <= 30) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else if (jours <= 60) {
                        setStyle("-fx-text-fill: #ea580c; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #d97706;");
                    }
                }
            }
        });

        // Urgence avec badge style
        colPeremptionUrgence.setCellFactory(column -> new TableCell<>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("urgence-badge");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().removeAll(
                        "urgence-critique", "urgence-urgent", "urgence-attention", "urgence-perime"
                    );
                    switch (item) {
                        case "PERIME" -> badge.getStyleClass().add("urgence-perime");
                        case "CRITIQUE" -> badge.getStyleClass().add("urgence-critique");
                        case "URGENT" -> badge.getStyleClass().add("urgence-urgent");
                        default -> badge.getStyleClass().add("urgence-attention");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Row factory pour colorer les lignes
        tablePeremption.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(AlertePeremption item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                    "alert-row-critique", "alert-row-urgent", "alert-row-attention", "alert-row-perime"
                );
                if (!empty && item != null) {
                    switch (item.getNiveauUrgence()) {
                        case "PERIME" -> getStyleClass().add("alert-row-perime");
                        case "CRITIQUE" -> getStyleClass().add("alert-row-critique");
                        case "URGENT" -> getStyleClass().add("alert-row-urgent");
                        default -> getStyleClass().add("alert-row-attention");
                    }
                }
            }
        });
    }

    private void setupPerimesTable() {
        colPerimesMedicament.setCellValueFactory(data -> {
            Lot lot = data.getValue();
            String nom = lot.getMedicament() != null ? lot.getMedicament().getNomCommercial() : "Medicament #" + lot.getIdMedicament();
            return new SimpleStringProperty(nom);
        });
        colPerimesLot.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNumeroLot()));
        colPerimesDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDatePeremption().format(DATE_FORMAT)));
        colPerimesQuantite.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantiteStock())));

        // Colonne Retard (jours depuis peremption)
        colPerimesRetard.setCellValueFactory(data -> {
            long retard = ChronoUnit.DAYS.between(
                data.getValue().getDatePeremption(),
                LocalDate.now()
            );
            return new SimpleStringProperty(String.valueOf(retard));
        });
        colPerimesRetard.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText("+" + item + " j");
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                }
            }
        });

        // Row factory - tous les lots perimes en rouge
        tablePerimes.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Lot item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("alert-row-perime");
                if (!empty && item != null) {
                    getStyleClass().add("alert-row-perime");
                }
            }
        });
    }

    private void loadAllAlertes() {
        Task<Void> loadTask = new Task<>() {
            private List<AlerteStock> stockBas;
            private List<AlertePeremption> peremption;
            private List<Lot> perimes;

            @Override
            protected Void call() throws Exception {
                stockBas = alerteService.getAlertesStockBas();
                peremption = alerteService.getAlertesPeremption();
                perimes = alerteService.getLotsPerimes();
                return null;
            }

            @Override
            protected void succeeded() {
                stockBasData.setAll(stockBas);
                peremptionData.setAll(peremption);
                perimesData.setAll(perimes);

                int total = stockBas.size() + peremption.size() + perimes.size();
                lblTotalAlertes.setText(total + " alerte(s) au total");
                lblStockBasCount.setText(stockBas.size() + " stock(s) bas");
                lblPeremptionCount.setText(peremption.size() + " peremption(s) proche");
                lblPerimesCount.setText(perimes.size() + " lot(s) perime(s)");
            }

            @Override
            protected void failed() {
                logger.error("Erreur lors du chargement des alertes", getException());
            }
        };

        runAsync(loadTask);
    }

    @FXML
    private void handleRefresh() {
        loadAllAlertes();
    }

    /**
     * Exporte toutes les alertes en PDF.
     */
    @FXML
    private void handleExportAll() {
        exportPDF(() -> rapportService.genererRapportAlertesComplet(), "Rapport complet des alertes");
    }

    /**
     * Exporte les alertes de stock bas en PDF.
     */
    @FXML
    private void handleExportStockBas() {
        exportPDF(() -> rapportService.genererRapportAlertesStock(), "Alertes stock bas");
    }

    /**
     * Exporte les alertes de peremption en PDF.
     */
    @FXML
    private void handleExportPeremption() {
        exportPDF(() -> rapportService.genererRapportAlertesPeremption(), "Alertes peremption");
    }

    /**
     * Exporte les lots perimes en PDF.
     */
    @FXML
    private void handleExportPerimes() {
        exportPDF(() -> rapportService.genererRapportLotsPerimes(), "Lots perimes");
    }

    /**
     * Methode generique pour exporter un rapport PDF.
     */
    private void exportPDF(PDFExporter exporter, String titre) {
        Task<String> exportTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return exporter.export();
            }
        };

        exportTask.setOnSucceeded(event -> {
            String filePath = exportTask.getValue();
            logger.info("Rapport genere: {}", filePath);

            try {
                File pdfFile = new File(filePath);
                if (Desktop.isDesktopSupported() && pdfFile.exists()) {
                    Desktop.getDesktop().open(pdfFile);
                }
                showSuccess("Export reussi", titre + " exporte avec succes:\n" + filePath);
            } catch (Exception e) {
                logger.warn("Impossible d'ouvrir le PDF", e);
                showSuccess("Export reussi", titre + " exporte:\n" + filePath);
            }
        });

        exportTask.setOnFailed(event -> {
            logger.error("Erreur lors de l'export", exportTask.getException());
            showError("Erreur d'export", "Impossible de generer le rapport PDF.");
        });

        runAsync(exportTask);
    }

    @FXML
    private void handleExportCSV() {
        executeExport(() -> {
            List<AlerteStock> stockBas = alerteService.getAlertesStockBas();
            // Export alertes stock bas as CSV (uses the main alerte data)
            String filePath = com.sgpa.utils.CSVExporter.generateFilePath("alertes_stock_bas");
            String[] headers = {"Medicament", "Stock Actuel", "Seuil Min", "Deficit", "Criticite %"};
            List<Object[]> rows = new java.util.ArrayList<>();
            for (AlerteStock a : stockBas) {
                rows.add(new Object[]{a.getNomMedicament(), a.getStockActuel(), a.getSeuilMin(), a.getDeficit(), a.getNiveauCriticite() + "%"});
            }
            return com.sgpa.utils.CSVExporter.export(filePath, headers, rows);
        }, "Export CSV Alertes", true);
    }

    @FXML
    private void handleExportExcel() {
        executeExport(() -> {
            List<AlerteStock> stockBas = alerteService.getAlertesStockBas();
            List<AlertePeremption> peremption = alerteService.getAlertesPeremption();
            List<Lot> perimes = alerteService.getLotsPerimes();
            // Convert Lot perimes to AlertePeremption for the Excel service
            List<AlertePeremption> perimesAlertes = new java.util.ArrayList<>();
            for (Lot lot : perimes) {
                String nomMed = lot.getMedicament() != null ? lot.getMedicament().getNomCommercial() : "Medicament #" + lot.getIdMedicament();
                long retard = java.time.temporal.ChronoUnit.DAYS.between(lot.getDatePeremption(), java.time.LocalDate.now());
                perimesAlertes.add(new AlertePeremption(lot.getIdLot(), lot.getNumeroLot(), lot.getIdMedicament(), nomMed, lot.getDatePeremption(), -retard, lot.getQuantiteStock()));
            }
            return excelExportService.exportAlertes(stockBas, peremption, perimesAlertes);
        }, "Export Excel Alertes", true);
    }

    @FunctionalInterface
    private interface PDFExporter {
        String export() throws Exception;
    }
}
