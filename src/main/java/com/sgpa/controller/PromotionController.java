package com.sgpa.controller;

import com.sgpa.exception.ServiceException;
import com.sgpa.model.Promotion;
import com.sgpa.model.enums.TypePromotion;
import com.sgpa.service.PromotionService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controleur pour la gestion des promotions et remises.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class PromotionController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(PromotionController.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Barre d'outils
    @FXML private ComboBox<String> comboFiltre;
    @FXML private TextField txtRecherche;

    // Labels statistiques
    @FXML private Label lblTotal;
    @FXML private Label lblActives;
    @FXML private Label lblFutures;
    @FXML private Label lblExpirees;

    // Table
    @FXML private TableView<Promotion> tablePromotions;
    @FXML private TableColumn<Promotion, String> colCode;
    @FXML private TableColumn<Promotion, String> colNom;
    @FXML private TableColumn<Promotion, String> colType;
    @FXML private TableColumn<Promotion, String> colValeur;
    @FXML private TableColumn<Promotion, String> colDebut;
    @FXML private TableColumn<Promotion, String> colFin;
    @FXML private TableColumn<Promotion, String> colStatut;

    // Formulaire
    @FXML private Label lblFormTitre;
    @FXML private TextField txtCode;
    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private ComboBox<TypePromotion> comboType;
    @FXML private TextField txtValeur;
    @FXML private Label lblUnite;
    @FXML private HBox hboxQuantites;
    @FXML private Spinner<Integer> spinnerQteRequise;
    @FXML private Spinner<Integer> spinnerQteOfferte;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private CheckBox checkActif;
    @FXML private CheckBox checkUsageUnique;
    @FXML private CheckBox checkCumulable;
    @FXML private Label lblNbMedicaments;

    // Boutons
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;
    @FXML private Button btnSauvegarder;

    private final PromotionService promotionService;

    private ObservableList<Promotion> promotions;
    private FilteredList<Promotion> filteredPromotions;
    private Promotion promotionSelectionnee;
    private List<Integer> medicamentIds = new ArrayList<>();

    public PromotionController() {
        this.promotionService = new PromotionService();
    }

    @FXML
    public void initialize() {
        setupComboBoxes();
        setupTable();
        setupSearch();
        setupSelection();
        setupTypeListener();
        setupSpinners();
        clearForm();
    }

    @Override
    protected void onUserSet() {
        loadPromotions();
    }

    private void setupComboBoxes() {
        comboFiltre.getItems().addAll("Toutes", "Actives", "Futures", "Expirees", "Inactives");
        comboFiltre.setValue("Toutes");
        comboFiltre.setOnAction(e -> applyFilter());

        comboType.getItems().addAll(TypePromotion.values());
    }

    private void setupTable() {
        colCode.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCodePromo() != null ?
                        data.getValue().getCodePromo() : "-"));

        colNom.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNom()));

        colType.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTypePromotion().getLibelle()));

        colValeur.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDescriptionFormatee()));

        colDebut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDateDebut().format(DATE_FORMATTER)));

        colFin.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDateFin().format(DATE_FORMATTER)));

        colStatut.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatut()));

        // Colorer le statut
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
                        case "Active" -> setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
                        case "Future" -> setStyle("-fx-text-fill: #fd7e14;");
                        case "Expiree" -> setStyle("-fx-text-fill: #dc3545;");
                        case "Inactive" -> setStyle("-fx-text-fill: #6c757d;");
                        default -> setStyle("");
                    }
                }
            }
        });
    }

    private void setupSearch() {
        txtRecherche.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredPromotions != null) {
                applyFilter();
            }
        });
    }

    private void setupSelection() {
        tablePromotions.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        selectPromotion(newVal);
                    }
                });
    }

    private void setupTypeListener() {
        comboType.setOnAction(e -> {
            TypePromotion type = comboType.getValue();
            if (type != null) {
                lblUnite.setText(type.getUnite());
                boolean showQuantites = type == TypePromotion.OFFRE_GROUPEE;
                hboxQuantites.setVisible(showQuantites);
                hboxQuantites.setManaged(showQuantites);
            }
        });
    }

    private void setupSpinners() {
        spinnerQteRequise.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 2));
        spinnerQteOfferte.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
    }

    private void loadPromotions() {
        Task<List<Promotion>> loadTask = new Task<>() {
            @Override
            protected List<Promotion> call() throws Exception {
                return promotionService.getToutesPromotions();
            }

            @Override
            protected void succeeded() {
                List<Promotion> result = getValue();
                promotions = FXCollections.observableArrayList(result);
                filteredPromotions = new FilteredList<>(promotions, p -> true);
                tablePromotions.setItems(filteredPromotions);
                applyFilter();
                updateStats();
                logger.info("{} promotions chargees", result.size());
            }

            @Override
            protected void failed() {
                logger.error("Erreur lors du chargement des promotions", getException());
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de charger les promotions: " + getException().getMessage());
            }
        };

        new Thread(loadTask).start();
    }

    private void applyFilter() {
        if (filteredPromotions == null) return;

        String filtre = comboFiltre.getValue();
        String recherche = txtRecherche.getText();

        filteredPromotions.setPredicate(p -> {
            // Filtre par recherche textuelle
            if (recherche != null && !recherche.isEmpty()) {
                String lower = recherche.toLowerCase();
                boolean matchNom = p.getNom().toLowerCase().contains(lower);
                boolean matchCode = p.getCodePromo() != null &&
                        p.getCodePromo().toLowerCase().contains(lower);
                if (!matchNom && !matchCode) {
                    return false;
                }
            }

            // Filtre par statut
            if (filtre == null || "Toutes".equals(filtre)) {
                return true;
            }

            return switch (filtre) {
                case "Actives" -> p.estValide();
                case "Futures" -> p.estFuture();
                case "Expirees" -> p.estExpiree();
                case "Inactives" -> !p.isActif();
                default -> true;
            };
        });
    }

    private void updateStats() {
        if (promotions == null) return;

        int total = promotions.size();
        int actives = 0, futures = 0, expirees = 0;

        for (Promotion p : promotions) {
            if (p.estValide()) actives++;
            else if (p.estFuture()) futures++;
            else if (p.estExpiree()) expirees++;
        }

        lblTotal.setText(total + " promotion(s)");
        lblActives.setText(actives + " active(s)");
        lblFutures.setText(futures + " future(s)");
        lblExpirees.setText(expirees + " expiree(s)");
    }

    private void selectPromotion(Promotion promotion) {
        this.promotionSelectionnee = promotion;
        lblFormTitre.setText("Modifier: " + promotion.getNom());
        btnSupprimer.setDisable(false);

        txtCode.setText(promotion.getCodePromo());
        txtNom.setText(promotion.getNom());
        txtDescription.setText(promotion.getDescription());
        comboType.setValue(promotion.getTypePromotion());
        txtValeur.setText(promotion.getValeur().toPlainString());
        spinnerQteRequise.getValueFactory().setValue(promotion.getQuantiteRequise());
        spinnerQteOfferte.getValueFactory().setValue(promotion.getQuantiteOfferte());
        dateDebut.setValue(promotion.getDateDebut());
        dateFin.setValue(promotion.getDateFin());
        checkActif.setSelected(promotion.isActif());
        checkUsageUnique.setSelected(promotion.isUsageUnique());
        checkCumulable.setSelected(promotion.isCumulable());

        medicamentIds = new ArrayList<>(promotion.getMedicamentIds());
        updateMedicamentsLabel();

        // Afficher/masquer les quantites selon le type
        boolean showQuantites = promotion.getTypePromotion() == TypePromotion.OFFRE_GROUPEE;
        hboxQuantites.setVisible(showQuantites);
        hboxQuantites.setManaged(showQuantites);
        lblUnite.setText(promotion.getTypePromotion().getUnite());
    }

    private void clearForm() {
        promotionSelectionnee = null;
        lblFormTitre.setText("Nouvelle Promotion");
        btnSupprimer.setDisable(true);

        txtCode.clear();
        txtNom.clear();
        txtDescription.clear();
        comboType.setValue(null);
        txtValeur.clear();
        lblUnite.setText("");
        spinnerQteRequise.getValueFactory().setValue(2);
        spinnerQteOfferte.getValueFactory().setValue(1);
        dateDebut.setValue(LocalDate.now());
        dateFin.setValue(LocalDate.now().plusMonths(1));
        checkActif.setSelected(true);
        checkUsageUnique.setSelected(false);
        checkCumulable.setSelected(false);

        hboxQuantites.setVisible(false);
        hboxQuantites.setManaged(false);

        medicamentIds.clear();
        updateMedicamentsLabel();

        tablePromotions.getSelectionModel().clearSelection();
    }

    private void updateMedicamentsLabel() {
        if (medicamentIds.isEmpty()) {
            lblNbMedicaments.setText("(tous si vide)");
        } else {
            lblNbMedicaments.setText("(" + medicamentIds.size() + " selectionne(s))");
        }
    }

    @FXML
    private void handleNouvelle() {
        clearForm();
    }

    @FXML
    private void handleRefresh() {
        loadPromotions();
    }

    @FXML
    private void handleAnnuler() {
        clearForm();
    }

    @FXML
    private void handleSauvegarder() {
        if (!validateForm()) {
            return;
        }

        Promotion promotion = promotionSelectionnee != null ?
                promotionSelectionnee : new Promotion();

        promotion.setCodePromo(txtCode.getText().isBlank() ? null : txtCode.getText().trim().toUpperCase());
        promotion.setNom(txtNom.getText().trim());
        promotion.setDescription(txtDescription.getText());
        promotion.setTypePromotion(comboType.getValue());
        promotion.setValeur(new BigDecimal(txtValeur.getText().trim()));
        promotion.setQuantiteRequise(spinnerQteRequise.getValue());
        promotion.setQuantiteOfferte(spinnerQteOfferte.getValue());
        promotion.setDateDebut(dateDebut.getValue());
        promotion.setDateFin(dateFin.getValue());
        promotion.setActif(checkActif.isSelected());
        promotion.setUsageUnique(checkUsageUnique.isSelected());
        promotion.setCumulable(checkCumulable.isSelected());
        promotion.setMedicamentIds(medicamentIds);

        if (currentUser != null) {
            promotion.setCreePar(currentUser.getIdUtilisateur());
        }

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (promotionSelectionnee != null) {
                    promotionService.modifierPromotion(promotion);
                } else {
                    promotionService.creerPromotion(promotion);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                showAlert(Alert.AlertType.INFORMATION, "Succes",
                        promotionSelectionnee != null ?
                                "Promotion modifiee avec succes." :
                                "Promotion creee avec succes.");
                clearForm();
                loadPromotions();
            }

            @Override
            protected void failed() {
                logger.error("Erreur lors de la sauvegarde", getException());
                showAlert(Alert.AlertType.ERROR, "Erreur",
                        "Impossible de sauvegarder: " + getException().getMessage());
            }
        };

        new Thread(saveTask).start();
    }

    @FXML
    private void handleSupprimer() {
        if (promotionSelectionnee == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la promotion");
        confirm.setContentText("Voulez-vous vraiment supprimer la promotion \"" +
                promotionSelectionnee.getNom() + "\" ?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    promotionService.supprimerPromotion(promotionSelectionnee.getIdPromotion());
                    showAlert(Alert.AlertType.INFORMATION, "Succes",
                            "Promotion supprimee avec succes.");
                    clearForm();
                    loadPromotions();
                } catch (ServiceException e) {
                    logger.error("Erreur lors de la suppression", e);
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible de supprimer: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleSelectMedicaments() {
        // TODO: Ouvrir une fenetre de selection de medicaments
        showAlert(Alert.AlertType.INFORMATION, "Selection medicaments",
                "Fonctionnalite a implementer: selection des medicaments concernes.\n" +
                "Pour l'instant, laissez vide pour appliquer a tous les medicaments.");
    }

    @FXML
    private void handleClearMedicaments() {
        medicamentIds.clear();
        updateMedicamentsLabel();
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (txtNom.getText() == null || txtNom.getText().isBlank()) {
            errors.append("- Le nom est obligatoire\n");
        }

        if (comboType.getValue() == null) {
            errors.append("- Le type est obligatoire\n");
        }

        if (txtValeur.getText() == null || txtValeur.getText().isBlank()) {
            errors.append("- La valeur est obligatoire\n");
        } else {
            try {
                BigDecimal valeur = new BigDecimal(txtValeur.getText().trim());
                if (valeur.compareTo(BigDecimal.ZERO) <= 0) {
                    errors.append("- La valeur doit etre positive\n");
                }
                if (comboType.getValue() == TypePromotion.POURCENTAGE &&
                    valeur.compareTo(BigDecimal.valueOf(100)) > 0) {
                    errors.append("- Le pourcentage ne peut pas depasser 100%\n");
                }
            } catch (NumberFormatException e) {
                errors.append("- La valeur doit etre un nombre valide\n");
            }
        }

        if (dateDebut.getValue() == null) {
            errors.append("- La date de debut est obligatoire\n");
        }

        if (dateFin.getValue() == null) {
            errors.append("- La date de fin est obligatoire\n");
        }

        if (dateDebut.getValue() != null && dateFin.getValue() != null &&
            dateFin.getValue().isBefore(dateDebut.getValue())) {
            errors.append("- La date de fin doit etre apres la date de debut\n");
        }

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.WARNING, "Validation",
                    "Veuillez corriger les erreurs suivantes:\n" + errors);
            return false;
        }

        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
