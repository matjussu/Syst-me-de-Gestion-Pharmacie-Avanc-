package com.sgpa.controller;

import com.sgpa.dao.impl.FournisseurDAOImpl;
import com.sgpa.dao.impl.MedicamentDAOImpl;
import com.sgpa.exception.ServiceException;
import com.sgpa.model.*;
import com.sgpa.model.enums.StatutCommande;
import com.sgpa.service.CommandeService;
import com.sgpa.service.ExcelExportService;
import com.sgpa.service.ExportService;
import com.sgpa.service.RapportService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controleur pour l'ecran de gestion des commandes fournisseurs.
 * Permet de creer, visualiser, recevoir et annuler des commandes.
 */
public class CommandeController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(CommandeController.class);
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // --- Filtres ToggleButtons ---
    @FXML private ToggleButton btnFilterAll;
    @FXML private ToggleButton btnFilterPending;
    @FXML private ToggleButton btnFilterReceived;
    @FXML private ToggleButton btnFilterCancelled;

    // --- Stats ---
    @FXML private Label lblTotal;
    @FXML private Label lblEnAttente;
    @FXML private Label lblRecues;

    // --- Table commandes ---
    @FXML private TableView<Commande> tableCommandes;
    @FXML private TableColumn<Commande, String> colId;
    @FXML private TableColumn<Commande, String> colDate;
    @FXML private TableColumn<Commande, String> colFournisseur;
    @FXML private TableColumn<Commande, String> colNbArticles;
    @FXML private TableColumn<Commande, String> colStatut;

    // --- Detail ---
    @FXML private Label lblDetailTitle;
    @FXML private Label lblDetailTotal;
    @FXML private Button btnExportPDF;
    @FXML private Button btnReceive;
    @FXML private Button btnCancel;
    @FXML private TableView<LigneCommande> tableLignes;
    @FXML private TableColumn<LigneCommande, String> colLigneMed;
    @FXML private TableColumn<LigneCommande, String> colLigneQte;
    @FXML private TableColumn<LigneCommande, String> colLigneRecue;
    @FXML private TableColumn<LigneCommande, String> colLignePrix;
    @FXML private TableColumn<LigneCommande, String> colLigneTotal;
    @FXML private TextArea txtNotes;

    // --- Dialog nouvelle commande ---
    @FXML private VBox newOrderDialog;
    @FXML private ComboBox<Fournisseur> comboFournisseur;
    @FXML private ComboBox<Medicament> comboMedicament;
    @FXML private Spinner<Integer> spinnerQteCmd;
    @FXML private TextField txtPrixCmd;
    @FXML private TableView<LigneCommande> tableNewLines;
    @FXML private TableColumn<LigneCommande, String> colNewMed;
    @FXML private TableColumn<LigneCommande, String> colNewQte;
    @FXML private TableColumn<LigneCommande, String> colNewPrix;
    @FXML private TableColumn<LigneCommande, String> colNewTotal;
    @FXML private TableColumn<LigneCommande, String> colNewAction;
    @FXML private TextArea txtNewNotes;
    @FXML private Label lblNewOrderTotal;

    // --- Dialog reception ---
    @FXML private VBox receptionDialog;
    @FXML private Label lblReceptionTitle;
    @FXML private VBox receptionLinesContainer;

    // --- Services et DAOs ---
    private final CommandeService commandeService;
    private final FournisseurDAOImpl fournisseurDAO;
    private final MedicamentDAOImpl medicamentDAO;
    private final RapportService rapportService;
    private final ExportService exportService;
    private final ExcelExportService excelExportService;

    // --- Donnees ---
    private final ObservableList<Commande> commandeData = FXCollections.observableArrayList();
    private final ObservableList<LigneCommande> ligneData = FXCollections.observableArrayList();
    private final ObservableList<LigneCommande> newLineData = FXCollections.observableArrayList();
    private List<Commande> allCommandes = new ArrayList<>();
    private List<ReceptionLineRow> receptionRows = new ArrayList<>();

    private Commande selectedCommande;
    private ToggleGroup filterGroup;

    public CommandeController() {
        this.commandeService = new CommandeService();
        this.fournisseurDAO = new FournisseurDAOImpl();
        this.medicamentDAO = new MedicamentDAOImpl();
        this.rapportService = new RapportService();
        this.exportService = new ExportService();
        this.excelExportService = new ExcelExportService();
    }

    @FXML
    public void initialize() {
        setupFilters();
        setupCommandeTable();
        setupLigneTable();
        setupNewLineTable();
        setupCombos();
        setupSpinner();
        setupResponsiveTable(tableCommandes);
        loadData();

        tableCommandes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> handleCommandeSelection(newVal));

        // Recalcul automatique du total de la nouvelle commande
        newLineData.addListener((ListChangeListener<LigneCommande>) c -> updateNewOrderTotal());
    }

    @Override
    public void onViewDisplayed() {
        handleCancelNewOrder();
        handleCancelReception();
        loadData();
    }

    // ==================== FILTRES ====================

    private void setupFilters() {
        filterGroup = new ToggleGroup();
        btnFilterAll.setToggleGroup(filterGroup);
        btnFilterPending.setToggleGroup(filterGroup);
        btnFilterReceived.setToggleGroup(filterGroup);
        btnFilterCancelled.setToggleGroup(filterGroup);

        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                btnFilterAll.setSelected(true);
            } else {
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        Toggle selected = filterGroup.getSelectedToggle();
        if (selected == btnFilterPending) {
            commandeData.setAll(allCommandes.stream()
                    .filter(c -> c.getStatut() == StatutCommande.EN_ATTENTE).toList());
        } else if (selected == btnFilterReceived) {
            commandeData.setAll(allCommandes.stream()
                    .filter(c -> c.getStatut() == StatutCommande.RECUE).toList());
        } else if (selected == btnFilterCancelled) {
            commandeData.setAll(allCommandes.stream()
                    .filter(c -> c.getStatut() == StatutCommande.ANNULEE).toList());
        } else {
            commandeData.setAll(allCommandes);
        }
    }

    // ==================== SETUP TABLES ====================

    private void setupCommandeTable() {
        colId.setCellValueFactory(data -> new SimpleStringProperty(
                "CMD-" + String.format("%04d", data.getValue().getIdCommande())));
        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDateCreation().format(DATETIME_FORMAT)));
        colFournisseur.setCellValueFactory(data -> {
            Fournisseur f = data.getValue().getFournisseur();
            return new SimpleStringProperty(f != null ? f.getNom() : "N/A");
        });
        colNbArticles.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getNombreArticlesCommandes())));

        colStatut.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getStatut().getLibelle()));

        colStatut.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "En attente" -> setStyle("-fx-text-fill: #fd7e14; -fx-font-weight: bold;");
                        case "Recue" -> setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                        case "Annulee" -> setStyle("-fx-text-fill: #dc3545;");
                        default -> setStyle("");
                    }
                }
            }
        });

        tableCommandes.setItems(commandeData);
    }

    private void setupLigneTable() {
        colLigneMed.setCellValueFactory(data -> {
            Medicament m = data.getValue().getMedicament();
            return new SimpleStringProperty(m != null ? m.getNomCommercial() : "N/A");
        });
        colLigneQte.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantiteCommandee())));
        colLigneRecue.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantiteRecue())));
        colLignePrix.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPrixUnitaire() != null ?
                        String.format("%.2f EUR", data.getValue().getPrixUnitaire()) : "N/A"));
        colLigneTotal.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f EUR", data.getValue().getMontantLigne())));

        tableLignes.setItems(ligneData);
    }

    private void setupNewLineTable() {
        colNewMed.setCellValueFactory(data -> {
            Medicament m = data.getValue().getMedicament();
            return new SimpleStringProperty(m != null ? m.getNomCommercial() : "N/A");
        });
        colNewQte.setCellValueFactory(data -> new SimpleStringProperty(
                String.valueOf(data.getValue().getQuantiteCommandee())));
        colNewPrix.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getPrixUnitaire() != null ?
                        String.format("%.2f EUR", data.getValue().getPrixUnitaire()) : "0"));
        colNewTotal.setCellValueFactory(data -> new SimpleStringProperty(
                String.format("%.2f EUR", data.getValue().getMontantLigne())));

        colNewAction.setCellFactory(column -> new TableCell<>() {
            private final Button btnRemove = new Button();
            {
                btnRemove.setText("X");
                btnRemove.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; " +
                        "-fx-font-size: 11; -fx-font-weight: bold; -fx-cursor: hand; " +
                        "-fx-background-radius: 6; -fx-padding: 2 8;");
                btnRemove.setOnAction(e -> {
                    LigneCommande ligne = getTableView().getItems().get(getIndex());
                    newLineData.remove(ligne);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnRemove);
            }
        });

        tableNewLines.setItems(newLineData);
    }

    private void setupCombos() {
        comboFournisseur.setConverter(new StringConverter<>() {
            @Override
            public String toString(Fournisseur f) {
                return f != null ? f.getNom() : "";
            }
            @Override
            public Fournisseur fromString(String s) { return null; }
        });

        comboMedicament.setConverter(new StringConverter<>() {
            @Override
            public String toString(Medicament m) {
                return m != null ? m.getNomCommercial() : "";
            }
            @Override
            public Medicament fromString(String s) { return null; }
        });
    }

    private void setupSpinner() {
        spinnerQteCmd.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99999, 10));
        spinnerQteCmd.setEditable(true);
    }

    // ==================== CHARGEMENT DES DONNEES ====================

    private void loadData() {
        tableCommandes.setPlaceholder(new ProgressIndicator());

        Task<List<Commande>> task = new Task<>() {
            @Override
            protected List<Commande> call() throws Exception {
                return commandeService.getAllCommandes();
            }

            @Override
            protected void succeeded() {
                allCommandes = getValue();
                applyFilter();
                updateStats();
                if (commandeData.isEmpty()) {
                    tableCommandes.setPlaceholder(new Label("Aucune commande"));
                }
            }

            @Override
            protected void failed() {
                logger.error("Erreur chargement commandes", getException());
                tableCommandes.setPlaceholder(new Label("Erreur de chargement"));
            }
        };
        runAsync(task);
    }

    private void updateStats() {
        int total = allCommandes.size();
        int enAttente = 0;
        int recues = 0;

        for (Commande c : allCommandes) {
            if (c.getStatut() == StatutCommande.EN_ATTENTE) enAttente++;
            else if (c.getStatut() == StatutCommande.RECUE) recues++;
        }

        lblTotal.setText(total + " commandes");
        lblEnAttente.setText(enAttente + " en attente");
        lblRecues.setText(recues + " recues");
    }

    // ==================== SELECTION COMMANDE ====================

    private void handleCommandeSelection(Commande commande) {
        selectedCommande = commande;
        if (commande == null) {
            lblDetailTitle.setText("Detail de la commande");
            lblDetailTotal.setText("0.00 EUR");
            btnExportPDF.setDisable(true);
            btnReceive.setDisable(true);
            btnCancel.setDisable(true);
            ligneData.clear();
            txtNotes.clear();
            return;
        }

        lblDetailTitle.setText("Commande CMD-" + String.format("%04d", commande.getIdCommande()));
        btnExportPDF.setDisable(false);
        btnReceive.setDisable(!commande.isModifiable());
        btnCancel.setDisable(!commande.isModifiable());
        txtNotes.setText(commande.getNotes() != null ? commande.getNotes() : "");

        // Charger les lignes via le service
        tableLignes.setPlaceholder(new ProgressIndicator());
        Task<List<LigneCommande>> task = new Task<>() {
            @Override
            protected List<LigneCommande> call() throws Exception {
                return commandeService.getLignesCommande(commande.getIdCommande());
            }

            @Override
            protected void succeeded() {
                ligneData.setAll(getValue());
                updateDetailTotal();
                if (ligneData.isEmpty()) {
                    tableLignes.setPlaceholder(new Label("Aucune ligne"));
                }
            }

            @Override
            protected void failed() {
                logger.error("Erreur chargement lignes", getException());
                tableLignes.setPlaceholder(new Label("Erreur de chargement"));
            }
        };
        runAsync(task);
    }

    private void updateDetailTotal() {
        BigDecimal total = ligneData.stream()
                .map(LigneCommande::getMontantLigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblDetailTotal.setText(String.format("%.2f EUR", total));
    }

    private void updateNewOrderTotal() {
        BigDecimal total = newLineData.stream()
                .map(LigneCommande::getMontantLigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblNewOrderTotal.setText(String.format("%.2f EUR", total));
    }

    // ==================== ACTIONS ====================

    @FXML
    private void handleRefresh() {
        loadData();
    }

    // ==================== NOUVELLE COMMANDE ====================

    @FXML
    private void handleNewOrder() {
        Task<Void> task = new Task<>() {
            private List<Fournisseur> fournisseurs;
            private List<Medicament> medicaments;

            @Override
            protected Void call() throws Exception {
                fournisseurs = fournisseurDAO.findAllActive();
                medicaments = medicamentDAO.findAllActive();
                return null;
            }

            @Override
            protected void succeeded() {
                comboFournisseur.setItems(FXCollections.observableArrayList(fournisseurs));
                comboMedicament.setItems(FXCollections.observableArrayList(medicaments));

                comboFournisseur.setValue(null);
                comboMedicament.setValue(null);
                newLineData.clear();
                txtNewNotes.clear();
                spinnerQteCmd.getValueFactory().setValue(10);
                txtPrixCmd.clear();

                newOrderDialog.setVisible(true);
                newOrderDialog.setManaged(true);
            }
        };
        runAsync(task);
    }

    @FXML
    private void handleAddLine() {
        Medicament med = comboMedicament.getValue();
        if (med == null) {
            showWarning( "Attention", "Selectionnez un medicament.");
            return;
        }

        // Verifier les doublons
        boolean alreadyExists = newLineData.stream()
                .anyMatch(l -> l.getIdMedicament().equals(med.getIdMedicament()));
        if (alreadyExists) {
            showWarning( "Doublon",
                    "Ce medicament est deja dans la commande. Supprimez-le d'abord pour le modifier.");
            return;
        }

        BigDecimal prix;
        try {
            String prixText = txtPrixCmd.getText().trim().replace(",", ".");
            if (prixText.isEmpty()) {
                showWarning( "Attention", "Saisissez le prix unitaire.");
                return;
            }
            prix = new BigDecimal(prixText);
            if (prix.compareTo(BigDecimal.ZERO) <= 0) {
                showWarning( "Attention", "Le prix doit etre superieur a 0.");
                return;
            }
        } catch (NumberFormatException e) {
            showWarning( "Attention", "Prix invalide. Utilisez le format : 12.50");
            return;
        }

        LigneCommande ligne = new LigneCommande();
        ligne.setIdMedicament(med.getIdMedicament());
        ligne.setMedicament(med);
        ligne.setQuantiteCommandee(spinnerQteCmd.getValue());
        ligne.setPrixUnitaire(prix);

        newLineData.add(ligne);

        // Reset les champs
        comboMedicament.setValue(null);
        spinnerQteCmd.getValueFactory().setValue(10);
        txtPrixCmd.clear();
    }

    @FXML
    private void handleCancelNewOrder() {
        newOrderDialog.setVisible(false);
        newOrderDialog.setManaged(false);
    }

    @FXML
    private void handleConfirmNewOrder() {
        if (comboFournisseur.getValue() == null) {
            showWarning( "Attention", "Selectionnez un fournisseur.");
            return;
        }
        if (newLineData.isEmpty()) {
            showWarning( "Attention", "Ajoutez au moins un medicament.");
            return;
        }

        int idFournisseur = comboFournisseur.getValue().getIdFournisseur();
        String notes = txtNewNotes.getText().trim();
        List<LigneCommande> lignes = new ArrayList<>(newLineData);

        Task<Commande> task = new Task<>() {
            @Override
            protected Commande call() throws Exception {
                return commandeService.creerCommande(idFournisseur, lignes, notes);
            }

            @Override
            protected void succeeded() {
                handleCancelNewOrder();
                loadData();
                showSuccess( "Succes",
                        "Commande creee avec succes (" + lignes.size() + " article(s)).");
            }

            @Override
            protected void failed() {
                logger.error("Erreur creation commande", getException());
                String msg = getException() instanceof ServiceException ?
                        getException().getMessage() : "Impossible de creer la commande.";
                showError( "Erreur", msg);
            }
        };
        runAsync(task);
    }

    // ==================== RECEPTION ====================

    @FXML
    private void handleReceiveOrder() {
        if (selectedCommande == null || !selectedCommande.isModifiable()) return;

        // Construire les lignes de reception dynamiquement
        receptionRows.clear();
        receptionLinesContainer.getChildren().clear();

        lblReceptionTitle.setText("Reception - CMD-" +
                String.format("%04d", selectedCommande.getIdCommande()));

        for (LigneCommande ligne : ligneData) {
            ReceptionLineRow row = new ReceptionLineRow(ligne);
            receptionRows.add(row);
            receptionLinesContainer.getChildren().add(row.buildUI());
        }

        if (receptionRows.isEmpty()) {
            Label emptyLabel = new Label("Aucune ligne a recevoir");
            emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-padding: 20;");
            receptionLinesContainer.getChildren().add(emptyLabel);
        }

        receptionDialog.setVisible(true);
        receptionDialog.setManaged(true);
    }

    @FXML
    private void handleCancelReception() {
        receptionDialog.setVisible(false);
        receptionDialog.setManaged(false);
    }

    @FXML
    private void handleConfirmReception() {
        List<CommandeService.ReceptionInfo> receptionInfos = new ArrayList<>();

        for (ReceptionLineRow row : receptionRows) {
            int qteRecue = row.spinnerQteRecue.getValue();
            if (qteRecue <= 0) continue;

            String numLot = row.txtNumeroLot.getText().trim();
            if (numLot.isEmpty()) {
                showWarning( "Champ manquant",
                        "Saisissez le numero de lot pour : " + row.medicamentNom);
                return;
            }

            LocalDate datePeremption = row.datePickerPeremption.getValue();
            if (datePeremption == null) {
                showWarning( "Champ manquant",
                        "Saisissez la date de peremption pour : " + row.medicamentNom);
                return;
            }

            if (datePeremption.isBefore(LocalDate.now())) {
                showWarning( "Date invalide",
                        "La date de peremption pour " + row.medicamentNom + " est dans le passe.");
                return;
            }

            CommandeService.ReceptionInfo info = new CommandeService.ReceptionInfo(
                    row.idMedicament,
                    numLot,
                    datePeremption,
                    qteRecue,
                    row.prixAchat
            );
            info.dateFabrication = row.datePickerFabrication.getValue();
            receptionInfos.add(info);
        }

        if (receptionInfos.isEmpty()) {
            showWarning( "Attention",
                    "Aucun article a recevoir (toutes les quantites sont a 0).");
            return;
        }

        int commandeId = selectedCommande.getIdCommande();
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                commandeService.recevoirCommande(commandeId, receptionInfos);
                return null;
            }

            @Override
            protected void succeeded() {
                handleCancelReception();
                loadData();
                showSuccess( "Succes",
                        "Commande recue. " + receptionInfos.size() + " lot(s) cree(s) dans le stock.");
            }

            @Override
            protected void failed() {
                logger.error("Erreur reception commande", getException());
                String msg = getException() instanceof ServiceException ?
                        getException().getMessage() : "Erreur lors de la reception.";
                showError( "Erreur", msg);
            }
        };
        runAsync(task);
    }

    // ==================== ANNULATION ====================

    @FXML
    private void handleCancelOrder() {
        if (selectedCommande == null || !selectedCommande.isModifiable()) return;

        int commandeId = selectedCommande.getIdCommande();
        showDangerConfirmation("Annuler la commande",
                "Voulez-vous vraiment annuler cette commande ?",
                () -> {
                    Task<Void> task = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            commandeService.annulerCommande(commandeId);
                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            loadData();
                            showSuccess("Succes", "Commande annulee.");
                        }

                        @Override
                        protected void failed() {
                            logger.error("Erreur annulation commande", getException());
                            String msg = getException() instanceof ServiceException ?
                                    getException().getMessage() : "Impossible d'annuler la commande.";
                            showError("Erreur", msg);
                        }
                    };
                    runAsync(task);
                });
    }

    // ==================== EXPORT PDF ====================

    @FXML
    private void handleExportPDF() {
        if (selectedCommande == null) {
            showWarning( "Attention",
                    "Selectionnez une commande pour l'exporter.");
            return;
        }

        Commande commandeComplete = selectedCommande;
        commandeComplete.setLignesCommande(new ArrayList<>(ligneData));

        Task<String> exportTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return rapportService.genererBonCommande(commandeComplete);
            }
        };

        exportTask.setOnSucceeded(event -> {
            String filePath = exportTask.getValue();
            logger.info("Bon de commande genere: {}", filePath);

            try {
                File pdfFile = new File(filePath);
                if (Desktop.isDesktopSupported() && pdfFile.exists()) {
                    Desktop.getDesktop().open(pdfFile);
                }
                showSuccess( "Export reussi",
                        "Bon de commande exporte :\n" + filePath);
            } catch (Exception e) {
                logger.warn("Impossible d'ouvrir le PDF", e);
                showSuccess( "Export reussi",
                        "Bon de commande exporte :\n" + filePath);
            }
        });

        exportTask.setOnFailed(event -> {
            logger.error("Erreur export PDF", exportTask.getException());
            showError( "Erreur d'export",
                    "Impossible de generer le bon de commande PDF.");
        });

        runAsync(exportTask);
    }

    // ==================== EXPORT CSV ====================

    @FXML
    private void handleExportCSV() {
        if (allCommandes.isEmpty()) {
            showWarning( "Aucune donnee", "Aucune commande a exporter.");
            return;
        }
        executeExport(() -> exportService.exportCommandes(allCommandes), "Export CSV Commandes", true);
    }

    // ==================== EXPORT EXCEL ====================

    @FXML
    private void handleExportExcel() {
        if (allCommandes.isEmpty()) {
            showWarning( "Aucune donnee", "Aucune commande a exporter.");
            return;
        }
        executeExport(() -> excelExportService.exportCommandes(allCommandes), "Export Excel Commandes", true);
    }

    // ==================== PREFILL (depuis alertes) ====================

    public void prefillMedicament(String medicamentName) {
        Task<Void> task = new Task<>() {
            private List<Fournisseur> fournisseurs;
            private List<Medicament> medicaments;

            @Override
            protected Void call() throws Exception {
                fournisseurs = fournisseurDAO.findAllActive();
                medicaments = medicamentDAO.findAllActive();
                return null;
            }

            @Override
            protected void succeeded() {
                comboFournisseur.setItems(FXCollections.observableArrayList(fournisseurs));
                comboMedicament.setItems(FXCollections.observableArrayList(medicaments));

                comboFournisseur.setValue(null);
                newLineData.clear();
                txtNewNotes.clear();
                spinnerQteCmd.getValueFactory().setValue(10);
                txtPrixCmd.clear();

                // Pre-selectionner le medicament correspondant
                for (Medicament med : medicaments) {
                    if (med.getNomCommercial().equalsIgnoreCase(medicamentName)) {
                        comboMedicament.setValue(med);
                        break;
                    }
                }

                newOrderDialog.setVisible(true);
                newOrderDialog.setManaged(true);
            }
        };
        runAsync(task);
    }

    // ==================== UTILITAIRES ====================


    // ==================== INNER CLASS : LIGNE DE RECEPTION ====================

    /**
     * Represente une ligne de reception avec ses champs de saisie.
     * Genere dynamiquement l'UI pour chaque ligne a recevoir.
     */
    private static class ReceptionLineRow {
        final int idMedicament;
        final String medicamentNom;
        final int quantiteCommandee;
        final BigDecimal prixAchat;

        final Spinner<Integer> spinnerQteRecue;
        final TextField txtNumeroLot;
        final DatePicker datePickerPeremption;
        final DatePicker datePickerFabrication;

        ReceptionLineRow(LigneCommande ligne) {
            this.idMedicament = ligne.getIdMedicament();
            this.medicamentNom = ligne.getMedicament() != null ?
                    ligne.getMedicament().getNomCommercial() : "Medicament #" + idMedicament;
            this.quantiteCommandee = ligne.getQuantiteCommandee();
            this.prixAchat = ligne.getPrixUnitaire();

            // Champs editables
            this.spinnerQteRecue = new Spinner<>(0, 99999, quantiteCommandee);
            this.spinnerQteRecue.setEditable(true);
            this.spinnerQteRecue.setPrefWidth(100);

            this.txtNumeroLot = new TextField();
            this.txtNumeroLot.setPromptText("Ex: LOT-2026-001");
            this.txtNumeroLot.setPrefWidth(160);

            this.datePickerPeremption = new DatePicker();
            this.datePickerPeremption.setPromptText("Date peremption");
            this.datePickerPeremption.setPrefWidth(150);

            this.datePickerFabrication = new DatePicker();
            this.datePickerFabrication.setPromptText("Date fabrication");
            this.datePickerFabrication.setPrefWidth(150);
        }

        VBox buildUI() {
            VBox card = new VBox(8);
            card.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; " +
                    "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 12;");

            // Ligne 1 : Nom du medicament + quantite commandee
            HBox header = new HBox(10);
            header.setAlignment(Pos.CENTER_LEFT);
            Label lblNom = new Label(medicamentNom);
            lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
            Label lblQteCmd = new Label("Qte commandee : " + quantiteCommandee);
            lblQteCmd.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
            header.getChildren().addAll(lblNom, lblQteCmd);

            // Ligne 2 : Champs de saisie
            HBox fields = new HBox(10);
            fields.setAlignment(Pos.CENTER_LEFT);

            VBox vQte = buildFieldGroup("Qte recue *", spinnerQteRecue);
            VBox vLot = buildFieldGroup("N° Lot *", txtNumeroLot);
            HBox.setHgrow(vLot, Priority.ALWAYS);
            VBox vPeremption = buildFieldGroup("Peremption *", datePickerPeremption);
            VBox vFabrication = buildFieldGroup("Fabrication", datePickerFabrication);

            fields.getChildren().addAll(vQte, vLot, vPeremption, vFabrication);

            card.getChildren().addAll(header, fields);
            return card;
        }

        private VBox buildFieldGroup(String labelText, javafx.scene.Node control) {
            VBox group = new VBox(3);
            Label label = new Label(labelText);
            label.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-weight: bold;");
            group.getChildren().addAll(label, control);
            return group;
        }
    }
}
