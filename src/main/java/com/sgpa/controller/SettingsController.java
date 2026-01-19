package com.sgpa.controller;

import com.sgpa.service.ConfigService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Controleur pour l'ecran de configuration de l'application.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class SettingsController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

    // Champs Pharmacie
    @FXML private TextField txtPharmacieNom;
    @FXML private TextField txtPharmacieAdresse;
    @FXML private TextField txtPharmacieTelephone;

    // Champs Alertes
    @FXML private Spinner<Integer> spinnerPeremption;
    @FXML private Spinner<Integer> spinnerStockSeuil;

    // Champs Repertoires
    @FXML private TextField txtRapportsDir;
    @FXML private TextField txtBackupDir;

    // Options Sauvegarde
    @FXML private CheckBox chkBackupCompression;

    // Interface
    @FXML private ComboBox<Integer> comboPagination;
    @FXML private ComboBox<String> comboMonnaie;

    // Statut
    @FXML private Label lblStatus;

    private final ConfigService configService;

    public SettingsController() {
        this.configService = new ConfigService();
    }

    @FXML
    public void initialize() {
        setupControls();
        loadSettings();
    }

    private void setupControls() {
        // Spinners
        spinnerPeremption.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(7, 365, 90));
        spinnerStockSeuil.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10));

        // ComboBox pagination
        comboPagination.setItems(FXCollections.observableArrayList(
                10, 25, 50, 100, 200
        ));

        // ComboBox monnaie
        comboMonnaie.setItems(FXCollections.observableArrayList(
                "EUR", "USD", "GBP", "CHF", "CAD"
        ));
    }

    private void loadSettings() {
        // Pharmacie
        txtPharmacieNom.setText(configService.getPharmacieNom());
        txtPharmacieAdresse.setText(configService.getPharmacieAdresse());
        txtPharmacieTelephone.setText(configService.getPharmacieTelephone());

        // Alertes
        spinnerPeremption.getValueFactory().setValue(configService.getAlertePeremptionJours());
        spinnerStockSeuil.getValueFactory().setValue(configService.getAlerteStockSeuilDefaut());

        // Repertoires
        txtRapportsDir.setText(configService.getRapportsRepertoire());
        txtBackupDir.setText(configService.getBackupRepertoire());

        // Sauvegarde
        chkBackupCompression.setSelected(configService.isBackupCompression());

        // Interface
        Integer paginationValue = configService.getUiPaginationTaille();
        if (!comboPagination.getItems().contains(paginationValue)) {
            comboPagination.getItems().add(paginationValue);
            comboPagination.getItems().sort(Integer::compareTo);
        }
        comboPagination.setValue(paginationValue);

        String monnaie = configService.getFormatMonnaie();
        if (!comboMonnaie.getItems().contains(monnaie)) {
            comboMonnaie.getItems().add(monnaie);
        }
        comboMonnaie.setValue(monnaie);

        lblStatus.setText("");
    }

    @FXML
    private void handleSave() {
        try {
            // Pharmacie
            configService.setPharmacieNom(txtPharmacieNom.getText());
            configService.setPharmacieAdresse(txtPharmacieAdresse.getText());
            configService.setPharmacieTelephone(txtPharmacieTelephone.getText());

            // Alertes
            configService.setAlertePeremptionJours(spinnerPeremption.getValue());
            configService.setAlerteStockSeuilDefaut(spinnerStockSeuil.getValue());

            // Repertoires
            configService.setRapportsRepertoire(txtRapportsDir.getText());
            configService.setBackupRepertoire(txtBackupDir.getText());

            // Sauvegarde
            configService.setBackupCompression(chkBackupCompression.isSelected());

            // Interface
            configService.setUiPaginationTaille(comboPagination.getValue());

            // Sauvegarder
            configService.saveConfig();

            lblStatus.setText("Configuration enregistree avec succes!");
            lblStatus.setStyle("-fx-text-fill: #28a745;");

            showAlert(Alert.AlertType.INFORMATION, "Succes",
                    "La configuration a ete enregistree.\n" +
                            "Certains changements prendront effet au prochain demarrage.");

        } catch (IOException e) {
            logger.error("Erreur lors de la sauvegarde de la configuration", e);
            lblStatus.setText("Erreur lors de la sauvegarde");
            lblStatus.setStyle("-fx-text-fill: #dc3545;");

            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'enregistrer la configuration: " + e.getMessage());
        }
    }

    @FXML
    private void handleReset() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reinitialiser la configuration");
        confirm.setHeaderText("Reinitialiser aux valeurs par defaut?");
        confirm.setContentText("Cette action va restaurer tous les parametres a leurs valeurs par defaut.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    configService.resetToDefaults();
                    loadSettings();
                    lblStatus.setText("Configuration reinitialisee");
                    lblStatus.setStyle("-fx-text-fill: #17a2b8;");
                } catch (IOException e) {
                    logger.error("Erreur lors de la reinitialisation", e);
                    showAlert(Alert.AlertType.ERROR, "Erreur",
                            "Impossible de reinitialiser: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void handleBrowseRapports() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le repertoire des rapports");

        String currentPath = txtRapportsDir.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
        }

        File selectedDir = chooser.showDialog(txtRapportsDir.getScene().getWindow());
        if (selectedDir != null) {
            txtRapportsDir.setText(selectedDir.getAbsolutePath());
        }
    }

    @FXML
    private void handleBrowseBackup() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choisir le repertoire des sauvegardes");

        String currentPath = txtBackupDir.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentDir = new File(currentPath);
            if (currentDir.exists() && currentDir.isDirectory()) {
                chooser.setInitialDirectory(currentDir);
            }
        }

        File selectedDir = chooser.showDialog(txtBackupDir.getScene().getWindow());
        if (selectedDir != null) {
            txtBackupDir.setText(selectedDir.getAbsolutePath());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
