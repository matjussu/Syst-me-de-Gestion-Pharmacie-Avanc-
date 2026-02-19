package com.sgpa.controller;

import com.sgpa.dto.AlertePeremption;
import com.sgpa.dto.AlerteStock;
import com.sgpa.dao.VenteDAO;
import com.sgpa.dao.impl.VenteDAOImpl;
import com.sgpa.model.LigneVente;
import com.sgpa.model.Lot;
import com.sgpa.model.Vente;
import com.sgpa.service.AlerteService;
import com.sgpa.service.ExcelExportService;
import com.sgpa.service.RapportService;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.shape.Rectangle;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controleur pour l'ecran des statistiques et graphiques.
 *
 * @author SGPA Team
 * @version 3.0
 */
public class StatistiquesController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(StatistiquesController.class);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");

    // Filtres
    @FXML private ComboBox<String> comboPeriode;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private ProgressIndicator progressIndicator;

    // Resume KPI
    @FXML private Label lblChiffreAffaires;
    @FXML private Label lblNombreVentes;
    @FXML private Label lblPanierMoyen;
    @FXML private Label lblArticlesVendus;

    // Tendances KPI
    @FXML private Label lblTrendCA;
    @FXML private Label lblTrendVentes;
    @FXML private Label lblTrendPanier;
    @FXML private Label lblTrendArticles;

    // Graphiques
    @FXML private LineChart<String, Number> chartVentes;
    @FXML private BarChart<Number, String> chartTopMedicaments;
    @FXML private BarChart<Number, String> chartTopCA;
    @FXML private CategoryAxis yAxisTop;
    @FXML private CategoryAxis yAxisCA;
    @FXML private Label lblChartVentesInfo;

    // Alertes badges
    @FXML private Label lblAlertesStockBas;
    @FXML private Label lblAlertesPeremption;
    @FXML private Label lblAlertesPerimes;
    @FXML private Label lblNoAlertes;
    @FXML private HBox badgeStockBas;
    @FXML private HBox badgePeremption;
    @FXML private HBox badgePerimes;

    // Panels pour animations
    @FXML private HBox summaryRow;
    @FXML private HBox topRow;
    @FXML private VBox caPanel;
    @FXML private VBox ventesPanel;

    private final VenteDAO venteDAO;
    private final AlerteService alerteService;
    private final RapportService rapportService;
    private final ExcelExportService excelExportService;

    public StatistiquesController() {
        this.venteDAO = new VenteDAOImpl();
        this.alerteService = new AlerteService();
        this.rapportService = new RapportService();
        this.excelExportService = new ExcelExportService();
    }

    @FXML
    public void initialize() {
        setupFilters();
        loadData();
    }

    private void setupFilters() {
        comboPeriode.setItems(FXCollections.observableArrayList(
                "Aujourd'hui",
                "Cette semaine",
                "Ce mois",
                "Les 30 derniers jours",
                "Les 90 derniers jours",
                "Personnalise"
        ));
        comboPeriode.setValue("Les 30 derniers jours");
        comboPeriode.setOnAction(e -> onPeriodeChanged());

        dateFin.setValue(LocalDate.now());
        dateDebut.setValue(LocalDate.now().minusDays(30));

        updateDatePickersState();
    }

    private void onPeriodeChanged() {
        String periode = comboPeriode.getValue();
        LocalDate now = LocalDate.now();

        switch (periode) {
            case "Aujourd'hui" -> {
                dateDebut.setValue(now);
                dateFin.setValue(now);
            }
            case "Cette semaine" -> {
                dateDebut.setValue(now.minusDays(now.getDayOfWeek().getValue() - 1));
                dateFin.setValue(now);
            }
            case "Ce mois" -> {
                dateDebut.setValue(now.withDayOfMonth(1));
                dateFin.setValue(now);
            }
            case "Les 30 derniers jours" -> {
                dateDebut.setValue(now.minusDays(30));
                dateFin.setValue(now);
            }
            case "Les 90 derniers jours" -> {
                dateDebut.setValue(now.minusDays(90));
                dateFin.setValue(now);
            }
        }

        updateDatePickersState();

        if (!"Personnalise".equals(periode)) {
            loadData();
        }
    }

    private void updateDatePickersState() {
        boolean custom = "Personnalise".equals(comboPeriode.getValue());
        dateDebut.setDisable(!custom);
        dateFin.setDisable(!custom);
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        progressIndicator.setVisible(true);

        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();

        if (debut == null) debut = LocalDate.now().minusDays(30);
        if (fin == null) fin = LocalDate.now();

        final LocalDate finalDebut = debut;
        final LocalDate finalFin = fin;

        Task<StatistiquesData> loadTask = new Task<>() {
            @Override
            protected StatistiquesData call() throws Exception {
                StatistiquesData data = new StatistiquesData();

                // === Ventes periode courante ===
                List<Vente> ventes = venteDAO.findByDateRange(finalDebut, finalFin);
                data.ventes = ventes;

                data.chiffreAffaires = BigDecimal.ZERO;
                data.articlesVendus = 0;
                Map<String, Integer> ventesParMedicament = new HashMap<>();
                Map<String, BigDecimal> caParMedicament = new HashMap<>();

                for (Vente v : ventes) {
                    if (v.getMontantTotal() != null) {
                        data.chiffreAffaires = data.chiffreAffaires.add(v.getMontantTotal());
                    }

                    List<LigneVente> lignes = venteDAO.findLignesByVenteId(v.getIdVente());
                    for (LigneVente ligne : lignes) {
                        data.articlesVendus += ligne.getQuantite();

                        if (ligne.getLot() != null && ligne.getLot().getMedicament() != null) {
                            String nomMed = ligne.getLot().getMedicament().getNomCommercial();
                            ventesParMedicament.merge(nomMed, ligne.getQuantite(), Integer::sum);
                            caParMedicament.merge(nomMed, ligne.getMontantLigne(), BigDecimal::add);
                        }
                    }
                }

                // === Ventes periode precedente (pour tendances) ===
                long daysBetween = ChronoUnit.DAYS.between(finalDebut, finalFin) + 1;
                LocalDate prevFin = finalDebut.minusDays(1);
                LocalDate prevDebut = prevFin.minusDays(daysBetween - 1);

                List<Vente> ventesPrev = venteDAO.findByDateRange(prevDebut, prevFin);
                data.prevChiffreAffaires = BigDecimal.ZERO;
                data.prevNombreVentes = ventesPrev.size();
                data.prevArticlesVendus = 0;

                for (Vente v : ventesPrev) {
                    if (v.getMontantTotal() != null) {
                        data.prevChiffreAffaires = data.prevChiffreAffaires.add(v.getMontantTotal());
                    }
                    List<LigneVente> lignes = venteDAO.findLignesByVenteId(v.getIdVente());
                    for (LigneVente ligne : lignes) {
                        data.prevArticlesVendus += ligne.getQuantite();
                    }
                }

                // Top 10 medicaments par quantite
                data.topMedicaments = ventesParMedicament.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(10)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

                // Top 10 medicaments par chiffre d'affaires
                data.topMedicamentsCA = caParMedicament.entrySet().stream()
                        .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                        .limit(10)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

                // === Ventes par jour ===
                Map<String, BigDecimal> ventesParJourBrut = new LinkedHashMap<>();
                LocalDate current = finalDebut;
                while (!current.isAfter(finalFin)) {
                    final LocalDate day = current;
                    BigDecimal totalJour = ventes.stream()
                            .filter(v -> v.getDateVente() != null &&
                                    v.getDateVente().toLocalDate().equals(day))
                            .map(v -> v.getMontantTotal() != null ? v.getMontantTotal() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    ventesParJourBrut.put(day.format(DATE_FORMAT), totalJour);
                    current = current.plusDays(1);
                }

                // Agreger par semaine si > 30 jours
                if (ventesParJourBrut.size() > 30) {
                    data.ventesParJour = aggregateByWeek(ventesParJourBrut, finalDebut);
                    data.ventesAgregeParSemaine = true;
                } else {
                    data.ventesParJour = ventesParJourBrut;
                    data.ventesAgregeParSemaine = false;
                }

                // Alertes
                data.alertesStock = alerteService.getAlertesStockBas();
                data.alertesPeremption = alerteService.getAlertesPeremption();
                data.lotsPerimes = alerteService.getLotsPerimes();

                return data;
            }
        };

        loadTask.setOnSucceeded(event -> {
            progressIndicator.setVisible(false);
            StatistiquesData data = loadTask.getValue();
            updateUI(data);
        });

        loadTask.setOnFailed(event -> {
            progressIndicator.setVisible(false);
            logger.error("Erreur lors du chargement des statistiques", loadTask.getException());
        });

        runAsync(loadTask);
    }

    private Map<String, BigDecimal> aggregateByWeek(Map<String, BigDecimal> daily, LocalDate start) {
        Map<String, BigDecimal> weekly = new LinkedHashMap<>();
        BigDecimal weekTotal = BigDecimal.ZERO;
        LocalDate weekStart = start;
        LocalDate current = start;
        int dayCount = 0;

        for (Map.Entry<String, BigDecimal> entry : daily.entrySet()) {
            weekTotal = weekTotal.add(entry.getValue());
            dayCount++;
            if (dayCount % 7 == 0) {
                String label = weekStart.format(DATE_FORMAT) + "-" + current.format(DATE_FORMAT);
                weekly.put(label, weekTotal);
                weekTotal = BigDecimal.ZERO;
                weekStart = current.plusDays(1);
            }
            current = current.plusDays(1);
        }
        if (weekTotal.compareTo(BigDecimal.ZERO) > 0 || dayCount % 7 != 0) {
            String label = weekStart.format(DATE_FORMAT) + "-" + current.minusDays(1).format(DATE_FORMAT);
            weekly.put(label, weekTotal);
        }
        return weekly;
    }

    private void updateUI(StatistiquesData data) {
        // === KPI ===
        lblChiffreAffaires.setText(PRICE_FORMAT.format(data.chiffreAffaires) + " EUR");
        lblNombreVentes.setText(String.valueOf(data.ventes.size()));
        lblArticlesVendus.setText(String.valueOf(data.articlesVendus));

        if (!data.ventes.isEmpty()) {
            BigDecimal panierMoyen = data.chiffreAffaires.divide(
                    BigDecimal.valueOf(data.ventes.size()), 2, RoundingMode.HALF_UP);
            lblPanierMoyen.setText(PRICE_FORMAT.format(panierMoyen) + " EUR");
        } else {
            lblPanierMoyen.setText("0.00 EUR");
        }

        // === Tendances ===
        setTrendLabel(lblTrendCA, data.chiffreAffaires.doubleValue(), data.prevChiffreAffaires.doubleValue());
        setTrendLabel(lblTrendVentes, data.ventes.size(), data.prevNombreVentes);
        setTrendLabel(lblTrendArticles, data.articlesVendus, data.prevArticlesVendus);

        double currentPanier = data.ventes.isEmpty() ? 0 : data.chiffreAffaires.doubleValue() / data.ventes.size();
        double prevPanier = data.prevNombreVentes == 0 ? 0 : data.prevChiffreAffaires.doubleValue() / data.prevNombreVentes;
        setTrendLabel(lblTrendPanier, currentPanier, prevPanier);

        // === Graphiques ===
        updateChartVentes(data);
        updateChartTopMedicaments(data.topMedicaments);
        updateAlerteSummary(data);
        updateChartTopCA(data.topMedicamentsCA);

        // === Animations ===
        animatePanels();
        animateHBarChart(chartTopMedicaments, 0);
        animateHBarChart(chartTopCA, 200);
        animateLineChart();
    }

    // === Animations d'apparition ===

    private void animatePanels() {
        Node[] panels = { summaryRow, topRow, caPanel, ventesPanel };

        for (int i = 0; i < panels.length; i++) {
            Node panel = panels[i];
            if (panel == null) continue;

            panel.setOpacity(0);
            panel.setTranslateY(20);

            FadeTransition fade = new FadeTransition(Duration.millis(400), panel);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(400), panel);
            slide.setFromY(20);
            slide.setToY(0);

            ParallelTransition parallel = new ParallelTransition(fade, slide);
            parallel.setDelay(Duration.millis(i * 100L));
            parallel.play();
        }
    }

    private void animateHBarChart(BarChart<Number, String> chart, long baseDelay) {
        Platform.runLater(() -> {
            int index = 0;
            for (XYChart.Series<Number, String> series : chart.getData()) {
                for (XYChart.Data<Number, String> data : series.getData()) {
                    Node node = data.getNode();
                    if (node != null) {
                        node.setScaleX(0);
                        ScaleTransition st = new ScaleTransition(Duration.millis(400), node);
                        st.setFromX(0);
                        st.setToX(1);
                        st.setDelay(Duration.millis(baseDelay + index * 60L));
                        st.setInterpolator(Interpolator.EASE_OUT);
                        st.play();
                        index++;
                    }
                }
            }
        });
    }

    private void animateLineChart() {
        Platform.runLater(() -> {
            Rectangle clip = new Rectangle(0, 0, 0, chartVentes.getHeight());
            chartVentes.setClip(clip);
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(clip.widthProperty(), 0)),
                new KeyFrame(Duration.millis(800), new KeyValue(clip.widthProperty(), chartVentes.getWidth(), Interpolator.EASE_OUT))
            );
            timeline.setOnFinished(e -> chartVentes.setClip(null));
            timeline.play();
        });
    }

    private void setTrendLabel(Label label, double current, double previous) {
        if (previous == 0 && current == 0) {
            label.setText("");
            return;
        }
        if (previous == 0) {
            label.setText("\u25B2 Nouveau");
            label.getStyleClass().setAll("trend-label", "trend-up");
            return;
        }

        double change = ((current - previous) / previous) * 100;
        String arrow = change >= 0 ? "\u25B2" : "\u25BC";
        label.setText(String.format("%s %.1f%% vs per. prec.", arrow, Math.abs(change)));

        if (change > 0) {
            label.getStyleClass().setAll("trend-label", "trend-up");
        } else if (change < 0) {
            label.getStyleClass().setAll("trend-label", "trend-down");
        } else {
            label.getStyleClass().setAll("trend-label", "trend-neutral");
        }
    }

    // === Graphique Evolution des Ventes (LineChart) ===

    private void updateChartVentes(StatistiquesData data) {
        chartVentes.getData().clear();

        if (data.ventesAgregeParSemaine) {
            lblChartVentesInfo.setText("Agrege par semaine");
        } else {
            lblChartVentesInfo.setText("");
        }

        CategoryAxis xAxis = (CategoryAxis) chartVentes.getXAxis();
        if (data.ventesParJour.size() > 14) {
            xAxis.setTickLabelRotation(-45);
            chartVentes.setCreateSymbols(false);
        } else {
            xAxis.setTickLabelRotation(0);
            chartVentes.setCreateSymbols(true);
        }

        boolean allZero = data.ventesParJour.values().stream()
                .allMatch(v -> v.compareTo(BigDecimal.ZERO) == 0);

        if (allZero && !data.ventesParJour.isEmpty()) {
            chartVentes.setTitle("Aucune vente sur cette periode");
        } else {
            chartVentes.setTitle(null);
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Ventes");

        for (Map.Entry<String, BigDecimal> entry : data.ventesParJour.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        chartVentes.getData().add(series);

        if (chartVentes.getCreateSymbols()) {
            Platform.runLater(() -> installLineChartTooltips(series));
        }
    }

    // === Graphique Top 10 Medicaments par Quantite (BarChart horizontal) ===

    private void updateChartTopMedicaments(Map<String, Integer> topMedicaments) {
        chartTopMedicaments.getData().clear();
        yAxisTop.setAutoRanging(true);
        yAxisTop.getCategories().clear();

        if (topMedicaments.isEmpty()) {
            chartTopMedicaments.setTitle("Aucune vente sur cette periode");
            return;
        }
        chartTopMedicaments.setTitle(null);

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Quantite vendue");

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(topMedicaments.entrySet());
        Collections.reverse(entries);

        List<String> categories = entries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        yAxisTop.setCategories(FXCollections.observableArrayList(categories));
        yAxisTop.setAutoRanging(false);

        for (Map.Entry<String, Integer> entry : entries) {
            series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
        }

        chartTopMedicaments.getData().add(series);

        Platform.runLater(() -> {
            chartTopMedicaments.requestLayout();
            installHBarTooltips(chartTopMedicaments, false);
        });
    }

    // === Graphique Top 10 Medicaments par Chiffre d'Affaires (BarChart horizontal) ===

    private void updateChartTopCA(Map<String, BigDecimal> topCA) {
        chartTopCA.getData().clear();
        yAxisCA.setAutoRanging(true);
        yAxisCA.getCategories().clear();

        if (topCA.isEmpty()) {
            chartTopCA.setTitle("Aucune vente sur cette periode");
            return;
        }
        chartTopCA.setTitle(null);

        XYChart.Series<Number, String> series = new XYChart.Series<>();
        series.setName("Chiffre d'affaires");

        List<Map.Entry<String, BigDecimal>> entries = new ArrayList<>(topCA.entrySet());
        Collections.reverse(entries);

        List<String> categories = entries.stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        yAxisCA.setCategories(FXCollections.observableArrayList(categories));
        yAxisCA.setAutoRanging(false);

        for (Map.Entry<String, BigDecimal> entry : entries) {
            series.getData().add(new XYChart.Data<>(entry.getValue(), entry.getKey()));
        }

        chartTopCA.getData().add(series);

        Platform.runLater(() -> {
            chartTopCA.requestLayout();
            // Couleur orange pour différencier du Top 10 Quantité
            for (XYChart.Data<Number, String> d : series.getData()) {
                if (d.getNode() != null) {
                    d.getNode().setStyle("-fx-bar-fill: #f59e0b;");
                }
            }
            installHBarTooltips(chartTopCA, true);
        });
    }

    // === Alertes Summary (badges) ===

    private void updateAlerteSummary(StatistiquesData data) {
        int stockBas = data.alertesStock.size();
        int peremptionProche = data.alertesPeremption.size();
        int perimes = data.lotsPerimes.size();

        lblAlertesStockBas.setText(String.valueOf(stockBas));
        lblAlertesPeremption.setText(String.valueOf(peremptionProche));
        lblAlertesPerimes.setText(String.valueOf(perimes));

        boolean noAlertes = (stockBas == 0 && peremptionProche == 0 && perimes == 0);
        lblNoAlertes.setVisible(noAlertes);
        lblNoAlertes.setManaged(noAlertes);

        badgeStockBas.setOpacity(stockBas > 0 ? 1.0 : 0.4);
        badgePeremption.setOpacity(peremptionProche > 0 ? 1.0 : 0.4);
        badgePerimes.setOpacity(perimes > 0 ? 1.0 : 0.4);
    }

    // === Tooltips utilitaires ===

    private void installHBarTooltips(BarChart<Number, String> chart, boolean isCurrency) {
        for (XYChart.Series<Number, String> series : chart.getData()) {
            for (XYChart.Data<Number, String> data : series.getData()) {
                Node node = data.getNode();
                if (node != null) {
                    String valueText = isCurrency
                            ? PRICE_FORMAT.format(data.getXValue()) + " EUR"
                            : String.valueOf(data.getXValue());
                    Tooltip tooltip = new Tooltip(data.getYValue() + " : " + valueText);
                    tooltip.setShowDelay(Duration.millis(100));
                    Tooltip.install(node, tooltip);

                    node.setOnMouseEntered(e -> node.setStyle(node.getStyle() + "-fx-opacity: 0.85;"));
                    node.setOnMouseExited(e -> node.setStyle(node.getStyle().replace("-fx-opacity: 0.85;", "")));
                }
            }
        }
    }

    private void installLineChartTooltips(XYChart.Series<String, Number> series) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            Node node = data.getNode();
            if (node != null) {
                Tooltip tooltip = new Tooltip(
                        data.getXValue() + "\n" + PRICE_FORMAT.format(data.getYValue()) + " EUR"
                );
                tooltip.setShowDelay(Duration.millis(100));
                Tooltip.install(node, tooltip);
            }
        }
    }

    // === Exports ===

    @FXML
    private void handleExportPDF() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut == null || fin == null) {
            return;
        }
        executeExport(() -> rapportService.genererRapportVentes(debut, fin), "Export PDF Statistiques", true);
    }

    @FXML
    private void handleExportExcel() {
        LocalDate debut = dateDebut.getValue();
        LocalDate fin = dateFin.getValue();
        if (debut == null || fin == null) {
            return;
        }
        executeExport(() -> excelExportService.exportVentes(debut, fin), "Export Excel Statistiques", true);
    }

    // === Classes internes ===

    private static class StatistiquesData {
        List<Vente> ventes = new ArrayList<>();
        BigDecimal chiffreAffaires = BigDecimal.ZERO;
        int articlesVendus = 0;
        Map<String, BigDecimal> ventesParJour = new LinkedHashMap<>();
        boolean ventesAgregeParSemaine = false;
        Map<String, Integer> topMedicaments = new LinkedHashMap<>();
        Map<String, BigDecimal> topMedicamentsCA = new LinkedHashMap<>();
        List<AlerteStock> alertesStock = new ArrayList<>();
        List<AlertePeremption> alertesPeremption = new ArrayList<>();
        List<Lot> lotsPerimes = new ArrayList<>();

        // Donnees periode precedente (pour tendances)
        BigDecimal prevChiffreAffaires = BigDecimal.ZERO;
        int prevNombreVentes = 0;
        int prevArticlesVendus = 0;
    }
}
