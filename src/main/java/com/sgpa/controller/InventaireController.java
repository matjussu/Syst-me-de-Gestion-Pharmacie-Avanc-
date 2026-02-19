package com.sgpa.controller;

import com.sgpa.exception.ServiceException;
import com.sgpa.model.ComptageInventaire;
import com.sgpa.model.Lot;
import com.sgpa.model.SessionInventaire;
import com.sgpa.model.enums.MotifEcart;
import com.sgpa.service.ExcelExportService;
import com.sgpa.service.ExportService;
import com.sgpa.service.InventaireService;
import com.sgpa.service.RapportService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Controleur pour l'ecran de gestion des inventaires.
 */
public class InventaireController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(InventaireController.class);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Session
    @FXML private VBox paneSession;
    @FXML private Label lblSessionStatut;
    @FXML private VBox paneNoSession;
    @FXML private TextField txtNotes;
    @FXML private VBox paneSessionEnCours;
    @FXML private Label lblSessionId;
    @FXML private Label lblSessionDebut;
    @FXML private Label lblDureeSession;
    @FXML private Label lblNbComptages;
    @FXML private Label lblNbEcarts;
    @FXML private ProgressBar progressBar;

    // Lots
    @FXML private TextField txtRecherche;
    @FXML private HBox hboxFiltres;
    @FXML private ToggleButton btnFiltreTous;
    @FXML private ToggleButton btnFiltreACompter;
    @FXML private ToggleButton btnFiltreComptes;
    @FXML private ToggleButton btnFiltreEcarts;
    @FXML private TableView<LotRow> tableLots;
    @FXML private TableColumn<LotRow, String> colMedicament;
    @FXML private TableColumn<LotRow, String> colLot;
    @FXML private TableColumn<LotRow, String> colPeremption;
    @FXML private TableColumn<LotRow, String> colStockTheorique;
    @FXML private TableColumn<LotRow, String> colStockPhysique;
    @FXML private TableColumn<LotRow, String> colEcart;
    @FXML private TableColumn<LotRow, String> colStatut;

    // Comptage
    @FXML private VBox panePlaceholder;
    @FXML private VBox paneComptage;
    @FXML private Label lblMedicament;
    @FXML private Label lblNumeroLot;
    @FXML private Label lblPeremption;
    @FXML private Label lblQteTheorique;
    @FXML private Spinner<Integer> spinnerQtePhysique;
    @FXML private Label lblEcartCalcule;
    @FXML private VBox paneMotif;
    @FXML private ComboBox<MotifEcart> comboMotif;
    @FXML private TextArea txtCommentaire;

    // Historique
    @FXML private TitledPane titledPaneHistorique;
    @FXML private Label lblHistoriqueCount;
    @FXML private TableView<SessionRow> tableHistorique;
    @FXML private TableColumn<SessionRow, String> colHistId;
    @FXML private TableColumn<SessionRow, String> colHistDebut;
    @FXML private TableColumn<SessionRow, String> colHistFin;
    @FXML private TableColumn<SessionRow, String> colHistStatut;
    @FXML private TableColumn<SessionRow, String> colHistComptages;
    @FXML private TableColumn<SessionRow, String> colHistEcarts;
    @FXML private TableColumn<SessionRow, String> colHistUtilisateur;

    private final InventaireService inventaireService;
    private final RapportService rapportService;
    private final ExportService exportService;
    private final ExcelExportService excelExportService;
    private final ObservableList<LotRow> lotsData = FXCollections.observableArrayList();
    private final ObservableList<SessionRow> sessionsData = FXCollections.observableArrayList();
    private FilteredList<LotRow> filteredLots;

    private SessionInventaire sessionEnCours;
    private LotRow lotSelectionne;
    private Map<Integer, ComptageInventaire> comptagesMap = new HashMap<>();

    private ToggleGroup filterGroup;
    private String activeFilter = "TOUS";
    private Timeline dureeTimeline;

    public InventaireController() {
        this.inventaireService = new InventaireService();
        this.rapportService = new RapportService();
        this.exportService = new ExportService();
        this.excelExportService = new ExcelExportService();
    }

    @FXML
    public void initialize() {
        setupMotifs();
        setupLotsTable();
        setupHistoriqueTable();
        setupSpinner();
        setupSearch();
        setupFilters();
        setupResponsiveTable(tableLots);
        setupResponsiveTable(tableHistorique);

        // Motif toujours visible mais desactive par defaut
        comboMotif.setDisable(true);
        paneMotif.setOpacity(0.5);

        loadData();
    }

    private void setupMotifs() {
        comboMotif.setItems(FXCollections.observableArrayList(MotifEcart.values()));
        comboMotif.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(MotifEcart motif) {
                return motif != null ? motif.getLibelle() : "";
            }

            @Override
            public MotifEcart fromString(String string) {
                return MotifEcart.fromString(string);
            }
        });
    }

    private void setupLotsTable() {
        colMedicament.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().medicament));
        colLot.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().numeroLot));
        colPeremption.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().datePeremption));
        colStockTheorique.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().stockTheorique)));
        colStockPhysique.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().stockPhysique != null ? String.valueOf(data.getValue().stockPhysique) : "-"));
        colEcart.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().ecart != null ? String.valueOf(data.getValue().ecart) : "-"));
        colStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statut));

        // Colorer les differences
        colEcart.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "-".equals(item)) {
                    setText(item);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        int ecart = Integer.parseInt(item);
                        if (ecart < 0) {
                            setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
                        } else if (ecart > 0) {
                            setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #6c757d;");
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });

        // Badges visuels pour le statut
        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    setText(null);
                    Label badge = new Label(item);
                    badge.setStyle(getStatutBadgeStyle(item));
                    badge.setPadding(new Insets(2, 8, 2, 8));
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        filteredLots = new FilteredList<>(lotsData, p -> true);
        tableLots.setItems(filteredLots);

        // Selection pour comptage
        tableLots.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && sessionEnCours != null) {
                selectLotForComptage(newVal);
            }
        });
    }

    private String getStatutBadgeStyle(String statut) {
        if ("Compte".equals(statut)) {
            return "-fx-background-color: #d1fae5; -fx-text-fill: #166534; -fx-background-radius: 10; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;";
        } else if ("A compter".equals(statut)) {
            return "-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-background-radius: 10; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;";
        } else {
            return "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-background-radius: 10; " +
                    "-fx-font-size: 11px; -fx-font-weight: bold;";
        }
    }

    private void setupHistoriqueTable() {
        colHistId.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().id)));
        colHistDebut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dateDebut));
        colHistFin.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().dateFin));
        colHistStatut.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statut));
        colHistComptages.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().nbComptages)));
        colHistEcarts.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().nbEcarts)));
        colHistUtilisateur.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().utilisateur));

        tableHistorique.setItems(sessionsData);
    }

    private void setupSpinner() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 99999, 0);
        spinnerQtePhysique.setValueFactory(valueFactory);
        spinnerQtePhysique.setEditable(true);

        // Validation : accepter uniquement les chiffres
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                return change;
            }
            if (newText.matches("\\d+")) {
                return change;
            }
            return null;
        };
        spinnerQtePhysique.getEditor().setTextFormatter(new TextFormatter<>(integerFilter));

        // Calculer la difference en temps reel
        spinnerQtePhysique.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (lotSelectionne != null && newVal != null) {
                int ecart = newVal - lotSelectionne.stockTheorique;
                lblEcartCalcule.setText(String.valueOf(ecart));

                // Activer/desactiver le motif selon la difference
                boolean hasEcart = ecart != 0;
                comboMotif.setDisable(!hasEcart);
                paneMotif.setOpacity(hasEcart ? 1.0 : 0.5);
                if (!hasEcart) {
                    comboMotif.setValue(null);
                }

                // Colorer la difference
                if (ecart < 0) {
                    lblEcartCalcule.setStyle("-fx-text-fill: #dc3545;");
                } else if (ecart > 0) {
                    lblEcartCalcule.setStyle("-fx-text-fill: #28a745;");
                } else {
                    lblEcartCalcule.setStyle("-fx-text-fill: #6c757d;");
                }
            }
        });
    }

    private void setupSearch() {
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupFilters() {
        filterGroup = new ToggleGroup();
        btnFiltreTous.setToggleGroup(filterGroup);
        btnFiltreACompter.setToggleGroup(filterGroup);
        btnFiltreComptes.setToggleGroup(filterGroup);
        btnFiltreEcarts.setToggleGroup(filterGroup);

        // Empecher la deselection totale
        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
                return;
            }
            if (newVal == btnFiltreTous) activeFilter = "TOUS";
            else if (newVal == btnFiltreACompter) activeFilter = "A_COMPTER";
            else if (newVal == btnFiltreComptes) activeFilter = "COMPTES";
            else if (newVal == btnFiltreEcarts) activeFilter = "ECARTS";
            applyFilters();
        });

        // Masquer les filtres quand pas de session
        hboxFiltres.setVisible(false);
        hboxFiltres.setManaged(false);
    }

    private void applyFilters() {
        filteredLots.setPredicate(lot -> {
            // Filtre texte
            String searchText = txtRecherche.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                if (!lot.medicament.toLowerCase().contains(lowerCaseFilter) &&
                        !lot.numeroLot.toLowerCase().contains(lowerCaseFilter)) {
                    return false;
                }
            }

            // Filtre statut
            switch (activeFilter) {
                case "A_COMPTER":
                    return "A compter".equals(lot.statut);
                case "COMPTES":
                    return "Compte".equals(lot.statut);
                case "ECARTS":
                    return lot.ecart != null && lot.ecart != 0;
                default:
                    return true;
            }
        });
    }

    private void loadData() {
        // Chainer : lots d'abord, puis session (pour eviter la race condition)
        loadLotsSequential();
        loadHistorique();
    }

    private void loadLotsSequential() {
        Task<List<Lot>> task = new Task<>() {
            @Override
            protected List<Lot> call() throws Exception {
                return inventaireService.getAllLotsForComptage();
            }
        };

        task.setOnSucceeded(e -> {
            lotsData.clear();
            for (Lot lot : task.getValue()) {
                lotsData.add(new LotRow(lot));
            }
            // Une fois les lots charges, charger la session
            loadSessionEnCours();
        });

        task.setOnFailed(e -> {
            logger.error("Erreur chargement lots", task.getException());
            // Tenter quand meme de charger la session
            loadSessionEnCours();
        });

        runAsync(task);
    }

    private void loadSessionEnCours() {
        Task<Optional<SessionInventaire>> task = new Task<>() {
            @Override
            protected Optional<SessionInventaire> call() throws Exception {
                return inventaireService.getSessionEnCours();
            }
        };

        task.setOnSucceeded(e -> {
            Optional<SessionInventaire> session = task.getValue();
            if (session.isPresent()) {
                sessionEnCours = session.get();
                afficherSessionEnCours();
                loadComptagesSession();
            } else {
                sessionEnCours = null;
                afficherNoSession();
            }
        });

        task.setOnFailed(e -> {
            logger.error("Erreur chargement session", task.getException());
            afficherNoSession();
        });

        runAsync(task);
    }

    private void afficherSessionEnCours() {
        paneNoSession.setVisible(false);
        paneNoSession.setManaged(false);
        paneSessionEnCours.setVisible(true);
        paneSessionEnCours.setManaged(true);

        // Afficher le placeholder, masquer le comptage
        panePlaceholder.setVisible(true);
        panePlaceholder.setManaged(true);
        paneComptage.setVisible(false);
        paneComptage.setManaged(false);

        // Afficher les filtres
        hboxFiltres.setVisible(true);
        hboxFiltres.setManaged(true);

        lblSessionStatut.setText("EN COURS");
        lblSessionStatut.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 3;");

        lblSessionId.setText(String.valueOf(sessionEnCours.getIdSession()));
        lblSessionDebut.setText(sessionEnCours.getDateDebut() != null ?
                sessionEnCours.getDateDebut().format(DATE_TIME_FORMAT) : "-");

        // Replier l'historique pendant le comptage
        titledPaneHistorique.setExpanded(false);

        startDureeTimer();
        updateComptagesStats();
    }

    private void afficherNoSession() {
        paneNoSession.setVisible(true);
        paneNoSession.setManaged(true);
        paneSessionEnCours.setVisible(false);
        paneSessionEnCours.setManaged(false);
        paneComptage.setVisible(false);
        paneComptage.setManaged(false);
        panePlaceholder.setVisible(true);
        panePlaceholder.setManaged(true);

        // Masquer les filtres
        hboxFiltres.setVisible(false);
        hboxFiltres.setManaged(false);

        lblSessionStatut.setText("AUCUNE SESSION");
        lblSessionStatut.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 3;");

        // Deplier l'historique quand pas de session
        titledPaneHistorique.setExpanded(true);

        stopDureeTimer();
        comptagesMap.clear();

        // Reset filtre
        activeFilter = "TOUS";
        btnFiltreTous.setSelected(true);
        applyFilters();
    }

    private void startDureeTimer() {
        stopDureeTimer();
        dureeTimeline = new Timeline(new KeyFrame(Duration.seconds(30), e -> updateDureeLabel()));
        dureeTimeline.setCycleCount(Timeline.INDEFINITE);
        dureeTimeline.play();
        updateDureeLabel();
    }

    private void stopDureeTimer() {
        if (dureeTimeline != null) {
            dureeTimeline.stop();
            dureeTimeline = null;
        }
    }

    private void updateDureeLabel() {
        if (sessionEnCours == null || sessionEnCours.getDateDebut() == null) {
            lblDureeSession.setText("-");
            return;
        }
        long minutes = ChronoUnit.MINUTES.between(sessionEnCours.getDateDebut(), LocalDateTime.now());
        if (minutes < 60) {
            lblDureeSession.setText(minutes + " min");
        } else {
            long hours = minutes / 60;
            long remainingMinutes = minutes % 60;
            lblDureeSession.setText(hours + "h " + remainingMinutes + "min");
        }
    }

    private void loadComptagesSession() {
        if (sessionEnCours == null) return;

        Task<List<ComptageInventaire>> task = new Task<>() {
            @Override
            protected List<ComptageInventaire> call() throws Exception {
                return inventaireService.getComptagesBySession(sessionEnCours.getIdSession());
            }
        };

        task.setOnSucceeded(e -> {
            comptagesMap.clear();
            for (ComptageInventaire c : task.getValue()) {
                comptagesMap.put(c.getIdLot(), c);
            }
            updateLotsWithComptages();
            updateComptagesStats();
        });

        runAsync(task);
    }

    private void updateLotsWithComptages() {
        for (LotRow lot : lotsData) {
            ComptageInventaire comptage = comptagesMap.get(lot.idLot);
            if (comptage != null) {
                lot.stockPhysique = comptage.getQuantitePhysique();
                lot.ecart = comptage.getEcart();
                lot.statut = "Compte";
            } else {
                lot.stockPhysique = null;
                lot.ecart = null;
                lot.statut = "A compter";
            }
        }
        tableLots.refresh();
        applyFilters();
    }

    private void updateComptagesStats() {
        if (sessionEnCours == null) return;

        int nbComptages = comptagesMap.size();
        int totalLots = lotsData.size();
        int nbEcarts = (int) comptagesMap.values().stream().filter(c -> c.getEcart() != 0).count();

        lblNbComptages.setText(nbComptages + " / " + totalLots + " lots");
        lblNbEcarts.setText(String.valueOf(nbEcarts));

        // Barre de progression
        double progress = totalLots > 0 ? (double) nbComptages / totalLots : 0;
        progressBar.setProgress(progress);

        if (nbEcarts > 0) {
            lblNbEcarts.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        } else {
            lblNbEcarts.setStyle("");
        }
    }

    private void loadHistorique() {
        Task<List<SessionInventaire>> task = new Task<>() {
            @Override
            protected List<SessionInventaire> call() throws Exception {
                return inventaireService.getAllSessions();
            }
        };

        task.setOnSucceeded(e -> {
            sessionsData.clear();
            for (SessionInventaire s : task.getValue()) {
                sessionsData.add(new SessionRow(s));
            }
            lblHistoriqueCount.setText("Historique (" + sessionsData.size() + " sessions)");
        });

        task.setOnFailed(e -> logger.error("Erreur chargement historique", task.getException()));

        runAsync(task);
    }

    private void selectLotForComptage(LotRow lot) {
        lotSelectionne = lot;

        lblMedicament.setText(lot.medicament);
        lblNumeroLot.setText(lot.numeroLot);
        lblPeremption.setText(lot.datePeremption);
        lblQteTheorique.setText(String.valueOf(lot.stockTheorique));

        // Initialiser avec la valeur existante ou theorique
        ComptageInventaire comptageExistant = comptagesMap.get(lot.idLot);
        int valeurInitiale = comptageExistant != null ? comptageExistant.getQuantitePhysique() : lot.stockTheorique;
        spinnerQtePhysique.getValueFactory().setValue(valeurInitiale);

        if (comptageExistant != null && comptageExistant.getMotifEcart() != null) {
            comboMotif.setValue(comptageExistant.getMotifEcart());
        } else {
            comboMotif.setValue(null);
        }

        if (comptageExistant != null && comptageExistant.getCommentaire() != null) {
            txtCommentaire.setText(comptageExistant.getCommentaire());
        } else {
            txtCommentaire.clear();
        }

        // Masquer placeholder, afficher formulaire
        panePlaceholder.setVisible(false);
        panePlaceholder.setManaged(false);
        paneComptage.setVisible(true);
        paneComptage.setManaged(true);
    }

    @FXML
    private void handleDemarrerSession() {
        String notes = txtNotes.getText();

        Task<SessionInventaire> task = new Task<>() {
            @Override
            protected SessionInventaire call() throws Exception {
                return inventaireService.creerSession(currentUser.getIdUtilisateur(), notes);
            }
        };

        task.setOnSucceeded(e -> {
            sessionEnCours = task.getValue();
            txtNotes.clear();
            afficherSessionEnCours();
            loadHistorique();
            showInfo("Session demarree", "Session d'inventaire demarree. Cliquez sur un lot pour commencer le comptage.");
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            showError("Erreur", ex instanceof ServiceException ? ex.getMessage() : "Erreur lors du demarrage de la session.");
            logger.error("Erreur demarrage session", ex);
        });

        runAsync(task);
    }

    @FXML
    private void handleAnnulerSession() {
        if (sessionEnCours == null) return;

        showDangerConfirmation("Annuler la session d'inventaire ?",
                "Tous les comptages effectues seront perdus. Cette action est irreversible.",
                () -> {
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            inventaireService.annulerSession(sessionEnCours.getIdSession());
                            return null;
                        }
                    };

                    task.setOnSucceeded(e -> {
                        sessionEnCours = null;
                        afficherNoSession();
                        loadHistorique();
                        showInfo("Session annulee", "La session d'inventaire a ete annulee.");
                    });

                    task.setOnFailed(e -> {
                        showError("Erreur", "Erreur lors de l'annulation de la session.");
                        logger.error("Erreur annulation session", task.getException());
                    });

                    runAsync(task);
                });
    }

    @FXML
    private void handleTerminerSession() {
        if (sessionEnCours == null) return;

        // Empecher terminaison sans aucun comptage
        if (comptagesMap.isEmpty()) {
            showError("Validation", "Vous devez compter au moins un lot avant de valider l'inventaire.");
            return;
        }

        // Verifier que tous les ecarts ont un motif
        boolean motifsManquants = comptagesMap.values().stream()
                .anyMatch(c -> c.getEcart() != 0 && c.getMotifEcart() == null);

        if (motifsManquants) {
            showError("Validation", "Veuillez renseigner une raison pour toutes les differences avant de valider.");
            return;
        }

        int nbEcarts = (int) comptagesMap.values().stream().filter(c -> c.getEcart() != 0).count();

        // Construire le contenu custom du recapitulatif
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        Label summary = new Label(String.format(
                "%d lot(s) compte(s) sur %d\n%d difference(s) detectee(s)\n\nLes stocks seront mis a jour avec les quantites comptees.",
                comptagesMap.size(), lotsData.size(), nbEcarts));
        summary.setWrapText(true);
        summary.setStyle("-fx-text-fill: #e2e8f0;");
        content.getChildren().add(summary);

        // Tableau des ecarts si il y en a
        if (nbEcarts > 0) {
            Label ecartTitle = new Label("Details des differences :");
            ecartTitle.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 5 0; -fx-text-fill: #e2e8f0;");
            content.getChildren().add(ecartTitle);

            TableView<ComptageInventaire> ecartTable = new TableView<>();
            ecartTable.setPrefHeight(200);
            ecartTable.setMaxWidth(500);

            TableColumn<ComptageInventaire, String> colLotResume = new TableColumn<>("Lot");
            colLotResume.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNumeroLot()));
            colLotResume.setPrefWidth(100);

            TableColumn<ComptageInventaire, String> colSysResume = new TableColumn<>("Systeme");
            colSysResume.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantiteTheorique())));
            colSysResume.setPrefWidth(70);

            TableColumn<ComptageInventaire, String> colCompteResume = new TableColumn<>("Compte");
            colCompteResume.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantitePhysique())));
            colCompteResume.setPrefWidth(70);

            TableColumn<ComptageInventaire, String> colDiffResume = new TableColumn<>("Diff.");
            colDiffResume.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getEcart())));
            colDiffResume.setPrefWidth(60);

            TableColumn<ComptageInventaire, String> colMotifResume = new TableColumn<>("Raison");
            colMotifResume.setCellValueFactory(d -> new SimpleStringProperty(
                    d.getValue().getMotifEcart() != null ? d.getValue().getMotifEcart().getLibelle() : "-"));
            colMotifResume.setPrefWidth(120);

            ecartTable.getColumns().addAll(colLotResume, colSysResume, colCompteResume, colDiffResume, colMotifResume);

            ObservableList<ComptageInventaire> ecartsList = FXCollections.observableArrayList();
            comptagesMap.values().stream()
                    .filter(c -> c.getEcart() != 0)
                    .forEach(ecartsList::add);
            ecartTable.setItems(ecartsList);

            content.getChildren().add(ecartTable);
        }

        final int sessionId = sessionEnCours.getIdSession();

        showCustomContent("Recapitulatif avant validation", content,
                "Confirmer la validation", "Annuler",
                () -> {
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            inventaireService.appliquerRegularisations(sessionId, currentUser.getIdUtilisateur());
                            inventaireService.terminerSession(sessionId);
                            return null;
                        }
                    };

                    task.setOnSucceeded(e -> {
                        sessionEnCours = null;
                        afficherNoSession();
                        loadLotsSequential();
                        loadHistorique();
                        showSuccess("Inventaire valide",
                                String.format("%d ajustement(s) de stock applique(s).", nbEcarts));
                    });

                    task.setOnFailed(e -> {
                        Throwable ex = task.getException();
                        showError("Erreur", ex instanceof ServiceException ? ex.getMessage() : "Erreur lors de la validation.");
                        logger.error("Erreur terminaison session", ex);
                    });

                    runAsync(task);
                }, null);
    }

    @FXML
    private void handleEnregistrerComptage() {
        if (sessionEnCours == null || lotSelectionne == null) return;

        int qtePhysique = spinnerQtePhysique.getValue();
        int ecart = qtePhysique - lotSelectionne.stockTheorique;

        // Verifier le motif si difference
        MotifEcart motif = comboMotif.getValue();
        if (ecart != 0 && motif == null) {
            showError("Validation", "Veuillez selectionner une raison pour la difference.");
            return;
        }

        String commentaire = txtCommentaire.getText();

        Task<ComptageInventaire> task = new Task<>() {
            @Override
            protected ComptageInventaire call() throws Exception {
                return inventaireService.enregistrerComptage(
                        sessionEnCours.getIdSession(),
                        lotSelectionne.idLot,
                        qtePhysique,
                        motif,
                        commentaire,
                        currentUser.getIdUtilisateur()
                );
            }
        };

        task.setOnSucceeded(e -> {
            ComptageInventaire comptage = task.getValue();
            comptagesMap.put(comptage.getIdLot(), comptage);
            updateLotsWithComptages();
            updateComptagesStats();

            // Passer au lot suivant
            int currentIndex = tableLots.getSelectionModel().getSelectedIndex();
            if (currentIndex < tableLots.getItems().size() - 1) {
                tableLots.getSelectionModel().select(currentIndex + 1);
            } else {
                paneComptage.setVisible(false);
                paneComptage.setManaged(false);
                panePlaceholder.setVisible(true);
                panePlaceholder.setManaged(true);
            }
        });

        task.setOnFailed(e -> {
            showError("Erreur", "Erreur lors de l'enregistrement du comptage.");
            logger.error("Erreur enregistrement comptage", task.getException());
        });

        runAsync(task);
    }

    @FXML
    private void handleAnnulerComptage() {
        paneComptage.setVisible(false);
        paneComptage.setManaged(false);
        panePlaceholder.setVisible(true);
        panePlaceholder.setManaged(true);
        lotSelectionne = null;
        tableLots.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleExportPDF() {
        if (sessionsData.isEmpty()) {
            showError("Erreur", "Aucun inventaire dans l'historique a exporter.");
            return;
        }
        // Recuperer la derniere session terminee
        SessionRow lastSession = sessionsData.get(0);
        executeExport(() -> {
            SessionInventaire session = inventaireService.getSessionById(lastSession.id);
            List<ComptageInventaire> comptages = inventaireService.getComptagesBySession(lastSession.id);
            return rapportService.genererRapportInventaire(session, comptages);
        }, "Export PDF Inventaire", true);
    }

    @FXML
    private void handleExportCSV() {
        if (sessionsData.isEmpty()) {
            showError("Erreur", "Aucun inventaire dans l'historique a exporter.");
            return;
        }
        SessionRow lastSession = sessionsData.get(0);
        executeExport(() -> {
            SessionInventaire session = inventaireService.getSessionById(lastSession.id);
            List<ComptageInventaire> comptages = inventaireService.getComptagesBySession(lastSession.id);
            return exportService.exportInventaire(session, comptages);
        }, "Export CSV Inventaire", true);
    }

    @FXML
    private void handleExportExcel() {
        if (sessionsData.isEmpty()) {
            showError("Erreur", "Aucun inventaire dans l'historique a exporter.");
            return;
        }
        SessionRow lastSession = sessionsData.get(0);
        executeExport(() -> {
            SessionInventaire session = inventaireService.getSessionById(lastSession.id);
            List<ComptageInventaire> comptages = inventaireService.getComptagesBySession(lastSession.id);
            return excelExportService.exportInventaire(session, comptages);
        }, "Export Excel Inventaire", true);
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    /**
     * Classe interne pour l'affichage des lots.
     */
    public static class LotRow {
        public final int idLot;
        public final String medicament;
        public final String numeroLot;
        public final String datePeremption;
        public final int stockTheorique;
        public Integer stockPhysique;
        public Integer ecart;
        public String statut;

        public LotRow(Lot lot) {
            this.idLot = lot.getIdLot();
            this.medicament = lot.getMedicament() != null ? lot.getMedicament().getNomCommercial() : "Lot #" + idLot;
            this.numeroLot = lot.getNumeroLot() != null ? lot.getNumeroLot() : "-";
            this.datePeremption = lot.getDatePeremption() != null ?
                    lot.getDatePeremption().format(DATE_FORMAT) : "-";
            this.stockTheorique = lot.getQuantiteStock();
            this.stockPhysique = null;
            this.ecart = null;
            this.statut = "A compter";
        }
    }

    /**
     * Classe interne pour l'affichage des sessions.
     */
    public static class SessionRow {
        public final int id;
        public final String dateDebut;
        public final String dateFin;
        public final String statut;
        public final int nbComptages;
        public final int nbEcarts;
        public final String utilisateur;

        public SessionRow(SessionInventaire session) {
            this.id = session.getIdSession() != null ? session.getIdSession() : 0;
            this.dateDebut = session.getDateDebut() != null ?
                    session.getDateDebut().format(DATE_TIME_FORMAT) : "-";
            this.dateFin = session.getDateFin() != null ?
                    session.getDateFin().format(DATE_TIME_FORMAT) : "-";
            this.statut = session.getStatut() != null ? session.getStatut().getLibelle() : "-";
            this.nbComptages = session.getNombreComptages();
            this.nbEcarts = session.getNombreEcarts();
            this.utilisateur = session.getNomUtilisateur();
        }
    }
}
