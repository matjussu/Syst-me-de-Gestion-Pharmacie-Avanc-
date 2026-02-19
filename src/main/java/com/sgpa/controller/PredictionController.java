package com.sgpa.controller;

import com.sgpa.dto.PredictionReapprovisionnement;
import com.sgpa.service.ConfigService;
import com.sgpa.service.ExcelExportService;
import com.sgpa.service.PredictionService;
import com.sgpa.service.RapportService;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Controleur pour la vue des predictions de reapprovisionnement.
 *
 * @author SGPA Team
 * @version 1.1
 */
public class PredictionController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(PredictionController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private ComboBox<String> comboPeriode;
    @FXML private TextField txtRecherche;
    @FXML private Button btnCommanderDetail;

    // Badges statistiques (cliquables pour filtrer)
    @FXML private HBox badgeTotal;
    @FXML private HBox badgeRuptures;
    @FXML private HBox badgeCritiques;
    @FXML private HBox badgeUrgents;

    // Labels statistiques
    @FXML private Label lblTotal;
    @FXML private Label lblRuptures;
    @FXML private Label lblCritiques;
    @FXML private Label lblUrgents;

    // Table des predictions
    @FXML private TableView<PredictionReapprovisionnement> tablePredictions;
    @FXML private TableColumn<PredictionReapprovisionnement, String> colMedicament;
    @FXML private TableColumn<PredictionReapprovisionnement, Number> colStockActuel;
    @FXML private TableColumn<PredictionReapprovisionnement, String> colConsoJour;
    @FXML private TableColumn<PredictionReapprovisionnement, String> colJoursRestants;
    @FXML private TableColumn<PredictionReapprovisionnement, String> colDateRupture;
    @FXML private TableColumn<PredictionReapprovisionnement, Number> colQuantiteSuggeree;
    @FXML private TableColumn<PredictionReapprovisionnement, String> colUrgence;

    // Detail - placeholder et contenu
    @FXML private Label lblDetailTitre;
    @FXML private VBox detailPlaceholder;
    @FXML private VBox detailContent;

    // Detail - labels
    @FXML private Label lblDetailStock;
    @FXML private Label lblDetailConsoMois;
    @FXML private Label lblDetailSeuil;
    @FXML private Label lblDetailDelai;

    // Graphique
    @FXML private LineChart<String, Number> chartPrevision;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final PredictionService predictionService;
    private final ConfigService configService;
    private final RapportService rapportService;
    private final ExcelExportService excelExportService;

    private ObservableList<PredictionReapprovisionnement> predictions;
    private FilteredList<PredictionReapprovisionnement> filteredPredictions;
    private String activeFilter = null;

    public PredictionController() {
        this.predictionService = new PredictionService();
        this.configService = new ConfigService();
        this.rapportService = new RapportService();
        this.excelExportService = new ExcelExportService();
    }

    @FXML
    public void initialize() {
        setupComboBox();
        setupTable();
        setupSearch();
        setupSelection();
        setupBadgeFilters();
        setupResponsiveTable(tablePredictions);
        clearDetail();
    }

    @Override
    protected void onUserSet() {
        loadPredictions();
    }

    private void setupComboBox() {
        comboPeriode.getItems().addAll("30 jours", "60 jours", "90 jours", "180 jours");
        comboPeriode.setValue("90 jours");
        comboPeriode.setOnAction(e -> loadPredictions());
    }

    private void setupTable() {
        colMedicament.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNomMedicament()));

        colStockActuel.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getStockVendable()));

        colConsoJour.setCellValueFactory(data ->
                new SimpleStringProperty(String.format("%.1f /j", data.getValue().getConsommationJournaliere())));

        colJoursRestants.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getJoursRestantsFormate()));

        colDateRupture.setCellValueFactory(data -> {
            PredictionReapprovisionnement p = data.getValue();
            if (p.getConsommationJournaliere() <= 0) {
                return new SimpleStringProperty("Aucune vente");
            }
            LocalDate date = p.getDateRupturePrevue();
            return new SimpleStringProperty(date != null ? date.format(DATE_FORMATTER) : "-");
        });

        colQuantiteSuggeree.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getQuantiteSuggeree()));

        colUrgence.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNiveauUrgence()));

        // Colorer les cellules d'urgence
        colUrgence.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case PredictionReapprovisionnement.NIVEAU_RUPTURE ->
                                setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-background-color: #f8d7da;");
                        case PredictionReapprovisionnement.NIVEAU_CRITIQUE ->
                                setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                        case PredictionReapprovisionnement.NIVEAU_URGENT ->
                                setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                        case PredictionReapprovisionnement.NIVEAU_ATTENTION ->
                                setStyle("-fx-text-fill: #ffc107;");
                        default ->
                                setStyle("-fx-text-fill: #28a745;");
                    }
                }
            }
        });

        // Colorer les lignes selon le niveau d'urgence
        tablePredictions.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(PredictionReapprovisionnement item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (PredictionReapprovisionnement.NIVEAU_RUPTURE.equals(item.getNiveauUrgence())) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else if (PredictionReapprovisionnement.NIVEAU_CRITIQUE.equals(item.getNiveauUrgence())) {
                    setStyle("-fx-background-color: #ffe0e0;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupSearch() {
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupSelection() {
        tablePredictions.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        showDetail(newVal);
                        detailPlaceholder.setVisible(false);
                        detailPlaceholder.setManaged(false);
                        detailContent.setVisible(true);
                        detailContent.setManaged(true);
                    } else {
                        clearDetail();
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Filtres par badges cliquables
    // -----------------------------------------------------------------------

    private void setupBadgeFilters() {
        setupBadgeClick(badgeTotal, null);
        setupBadgeClick(badgeRuptures, PredictionReapprovisionnement.NIVEAU_RUPTURE);
        setupBadgeClick(badgeCritiques, PredictionReapprovisionnement.NIVEAU_CRITIQUE);
        setupBadgeClick(badgeUrgents, PredictionReapprovisionnement.NIVEAU_URGENT);
    }

    private void setupBadgeClick(HBox badge, String filterLevel) {
        badge.setOnMouseClicked(e -> {
            if (filterLevel != null && filterLevel.equals(activeFilter)) {
                activeFilter = null;
            } else {
                activeFilter = filterLevel;
            }
            updateBadgeStyles();
            applyFilters();
        });
    }

    private void updateBadgeStyles() {
        badgeTotal.getStyleClass().remove("badge-active");
        badgeRuptures.getStyleClass().remove("badge-active");
        badgeCritiques.getStyleClass().remove("badge-active");
        badgeUrgents.getStyleClass().remove("badge-active");

        HBox activeBadge = badgeTotal;
        if (activeFilter != null) {
            activeBadge = switch (activeFilter) {
                case PredictionReapprovisionnement.NIVEAU_RUPTURE -> badgeRuptures;
                case PredictionReapprovisionnement.NIVEAU_CRITIQUE -> badgeCritiques;
                case PredictionReapprovisionnement.NIVEAU_URGENT -> badgeUrgents;
                default -> badgeTotal;
            };
        }
        if (!activeBadge.getStyleClass().contains("badge-active")) {
            activeBadge.getStyleClass().add("badge-active");
        }
    }

    private void applyFilters() {
        if (filteredPredictions == null) return;
        filteredPredictions.setPredicate(p -> {
            // Filtre texte
            String searchText = txtRecherche.getText();
            if (searchText != null && !searchText.isEmpty()) {
                if (!p.getNomMedicament().toLowerCase().contains(searchText.toLowerCase())) {
                    return false;
                }
            }
            // Filtre badge
            if (activeFilter != null) {
                return activeFilter.equals(p.getNiveauUrgence());
            }
            return true;
        });
    }

    // -----------------------------------------------------------------------
    // Chargement des predictions
    // -----------------------------------------------------------------------

    private int getSelectedPeriod() {
        String selected = comboPeriode.getValue();
        if (selected == null) return 90;
        return switch (selected) {
            case "30 jours" -> 30;
            case "60 jours" -> 60;
            case "180 jours" -> 180;
            default -> 90;
        };
    }

    private void loadPredictions() {
        int nbJours = getSelectedPeriod();

        Task<List<PredictionReapprovisionnement>> loadTask = new Task<>() {
            @Override
            protected List<PredictionReapprovisionnement> call() throws Exception {
                return predictionService.genererPredictions(nbJours);
            }

            @Override
            protected void succeeded() {
                List<PredictionReapprovisionnement> result = getValue();
                // Tri par urgence : RUPTURE en haut, OK en bas
                result.sort(Comparator.comparingInt(PredictionReapprovisionnement::getOrdreUrgence));

                predictions = FXCollections.observableArrayList(result);
                filteredPredictions = new FilteredList<>(predictions, p -> true);
                tablePredictions.setItems(filteredPredictions);

                applyFilters();
                updateStats(result);
                updateBadgeStyles();
                logger.info("{} predictions chargees", result.size());
            }

            @Override
            protected void failed() {
                logger.error("Erreur lors du chargement des predictions", getException());
                showError("Erreur",
                        "Impossible de charger les predictions: " + getException().getMessage());
            }
        };

        runAsync(loadTask);
    }

    private void updateStats(List<PredictionReapprovisionnement> list) {
        int total = list.size();
        int ruptures = 0, critiques = 0, urgents = 0;

        for (PredictionReapprovisionnement p : list) {
            switch (p.getNiveauUrgence()) {
                case PredictionReapprovisionnement.NIVEAU_RUPTURE -> ruptures++;
                case PredictionReapprovisionnement.NIVEAU_CRITIQUE -> critiques++;
                case PredictionReapprovisionnement.NIVEAU_URGENT -> urgents++;
            }
        }

        lblTotal.setText(total + " medicament(s)");
        lblRuptures.setText(ruptures + " rupture(s)");
        lblCritiques.setText(critiques + " critique(s)");
        lblUrgents.setText(urgents + " urgent(s)");
    }

    // -----------------------------------------------------------------------
    // Panneau detail
    // -----------------------------------------------------------------------

    private void showDetail(PredictionReapprovisionnement prediction) {
        lblDetailTitre.setText(prediction.getNomMedicament());
        lblDetailStock.setText(String.valueOf(prediction.getStockVendable()));
        lblDetailConsoMois.setText(String.format("%.0f unites", prediction.getConsommationMensuelle()));
        lblDetailSeuil.setText(String.valueOf(prediction.getSeuilMin()));
        lblDetailDelai.setText(configService.getPredictionDelaiLivraisonDefaut() + " jours");

        updateChart(prediction);
    }

    private void clearDetail() {
        lblDetailTitre.setText("Detail du medicament");
        detailPlaceholder.setVisible(true);
        detailPlaceholder.setManaged(true);
        detailContent.setVisible(false);
        detailContent.setManaged(false);
        chartPrevision.getData().clear();
    }

    // -----------------------------------------------------------------------
    // Graphique
    // -----------------------------------------------------------------------

    private void updateChart(PredictionReapprovisionnement prediction) {
        chartPrevision.getData().clear();

        XYChart.Series<String, Number> stockSeries = new XYChart.Series<>();
        stockSeries.setName("Stock prevu");

        XYChart.Series<String, Number> seuilSeries = new XYChart.Series<>();
        seuilSeries.setName("Seuil minimum");

        int stock = prediction.getStockVendable();
        double consoJour = prediction.getConsommationJournaliere();
        int seuil = prediction.getSeuilMin();

        // Adapter la duree du graphique a la situation
        int chartDays = 60;
        if (consoJour > 0) {
            int daysToZero = (int) Math.ceil(stock / consoJour);
            chartDays = Math.max(30, Math.min(90, daysToZero + 15));
        }

        int step = Math.max(1, chartDays / 12);
        LocalDate today = LocalDate.now();

        for (int i = 0; i <= chartDays; i += step) {
            String dateLabel = today.plusDays(i).format(DateTimeFormatter.ofPattern("dd/MM"));
            int stockPrevu = Math.max(0, (int) (stock - (consoJour * i)));
            stockSeries.getData().add(new XYChart.Data<>(dateLabel, stockPrevu));
            seuilSeries.getData().add(new XYChart.Data<>(dateLabel, seuil));
        }

        chartPrevision.getData().addAll(stockSeries, seuilSeries);

        // Styler la ligne seuil en pointilles rouges (ligne + legende)
        Platform.runLater(() -> {
            if (seuilSeries.getNode() != null) {
                seuilSeries.getNode().setStyle(
                        "-fx-stroke-dash-array: 8 4; -fx-stroke: #dc3545; -fx-stroke-width: 1.5;");
            }
            for (XYChart.Data<String, Number> data : seuilSeries.getData()) {
                if (data.getNode() != null) {
                    data.getNode().setVisible(false);
                }
            }
            // Corriger la couleur de la legende pour correspondre a la ligne
            for (javafx.scene.Node node : chartPrevision.lookupAll(".chart-legend-item-symbol")) {
                if (node.getStyleClass().contains("default-color1")) {
                    node.setStyle("-fx-background-color: #dc3545;");
                }
            }
        });
    }

    // -----------------------------------------------------------------------
    // Actions
    // -----------------------------------------------------------------------

    @FXML
    private void handleRefresh() {
        loadPredictions();
    }

    @FXML
    private void handleCommanderMedicament() {
        PredictionReapprovisionnement selected = tablePredictions.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showWarning("Selection requise",
                    "Veuillez selectionner un medicament.");
            return;
        }

        if (dashboardController != null) {
            dashboardController.navigateToCommandeWithMedicament(selected.getNomMedicament());
        }
    }

    @FXML
    private void handleExportPDF() {
        if (predictions == null || predictions.isEmpty()) {
            showWarning("Aucune donnee",
                    "Aucune prediction a exporter.");
            return;
        }
        executeExport(() -> rapportService.genererRapportPredictions(predictions), "Export PDF Predictions", true);
    }

    @FXML
    private void handleExportExcel() {
        if (predictions == null || predictions.isEmpty()) {
            showWarning("Aucune donnee", "Aucune prediction a exporter.");
            return;
        }
        executeExport(() -> excelExportService.exportPredictions(predictions), "Export Excel Predictions", true);
    }

}
