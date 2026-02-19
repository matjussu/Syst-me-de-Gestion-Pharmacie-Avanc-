package com.sgpa.controller;

import com.sgpa.service.BackupService;
import com.sgpa.service.BackupService.BackupFile;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controleur pour l'ecran de sauvegarde/restauration de la base de donnees.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class BackupController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML private Label lblBackupDir;
    @FXML private CheckBox chkCompress;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;
    @FXML private Label lblCount;

    @FXML private TableView<BackupFile> tableBackups;
    @FXML private TableColumn<BackupFile, String> colFileName;
    @FXML private TableColumn<BackupFile, String> colDate;
    @FXML private TableColumn<BackupFile, String> colSize;
    @FXML private TableColumn<BackupFile, Void> colActions;

    private final BackupService backupService;
    private final ObservableList<BackupFile> backupData = FXCollections.observableArrayList();

    public BackupController() {
        this.backupService = new BackupService();
    }

    @Override
    protected void onUserSet() {
        if (currentUser == null || !currentUser.isAdmin()) {
            logger.warn("Acces non autorise aux sauvegardes");
        }
    }

    @FXML
    public void initialize() {
        lblBackupDir.setText("Repertoire de sauvegarde: " + backupService.getBackupDir());
        setupTable();
        setupResponsiveTable(tableBackups);
        loadBackups();
    }

    private void setupTable() {
        colFileName.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFileName()));

        colDate.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getModifiedTime()
                                .atZone(ZoneId.systemDefault())
                                .format(DATE_FORMAT)
                ));

        colSize.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getFormattedSize()));

        // Colonne actions avec boutons
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnRestore = new Button("Restaurer");
            private final Button btnDelete = new Button();
            private final HBox hbox = new HBox(10);

            {
                btnRestore.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white;");
                btnRestore.setGraphic(new FontIcon("fas-undo"));

                btnDelete.setGraphic(new FontIcon("fas-trash"));
                btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white;");

                hbox.setAlignment(Pos.CENTER);
                hbox.getChildren().addAll(btnRestore, btnDelete);

                btnRestore.setOnAction(event -> {
                    BackupFile backup = getTableView().getItems().get(getIndex());
                    handleRestore(backup);
                });

                btnDelete.setOnAction(event -> {
                    BackupFile backup = getTableView().getItems().get(getIndex());
                    handleDelete(backup);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });

        tableBackups.setItems(backupData);
    }

    private void loadBackups() {
        List<BackupFile> backups = backupService.listBackups();
        backupData.setAll(backups);
        lblCount.setText(backups.size() + " sauvegarde(s)");
    }

    @FXML
    private void handleBackup() {
        setLoading(true, "Sauvegarde en cours...");

        Task<String> backupTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return backupService.backup(chkCompress.isSelected());
            }
        };

        backupTask.setOnSucceeded(event -> {
            setLoading(false, "Sauvegarde terminee!");
            String filePath = backupTask.getValue();
            loadBackups();
            showSuccess("Sauvegarde reussie", "La base de donnees a ete sauvegardee.\nFichier: " + filePath);
        });

        backupTask.setOnFailed(event -> {
            setLoading(false, "Erreur!");
            logger.error("Erreur de sauvegarde", backupTask.getException());
            showError("Echec de la sauvegarde", backupTask.getException().getMessage());
        });

        runAsync(backupTask);
    }

    private void handleRestore(BackupFile backup) {
        showDangerConfirmation("Restaurer la base de donnees?",
                "ATTENTION: Cette action va ecraser toutes les donnees actuelles!\n\n" +
                        "Fichier: " + backup.getFileName() + "\n" +
                        "Date: " + backup.getModifiedTime().atZone(ZoneId.systemDefault()).format(DATE_FORMAT) + "\n\n" +
                        "Voulez-vous continuer?",
                "Restaurer", "Annuler",
                () -> {
                    setLoading(true, "Restauration en cours...");

                    Task<Void> restoreTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            backupService.restore(backup.getPath());
                            return null;
                        }
                    };

                    restoreTask.setOnSucceeded(event -> {
                        setLoading(false, "Restauration terminee!");
                        showSuccess("Restauration reussie",
                                "La base de donnees a ete restauree.\nVeuillez redemarrer l'application pour appliquer les changements.");
                    });

                    restoreTask.setOnFailed(event -> {
                        setLoading(false, "Erreur!");
                        logger.error("Erreur de restauration", restoreTask.getException());
                        showError("Echec de la restauration", restoreTask.getException().getMessage());
                    });

                    runAsync(restoreTask);
                }, null);
    }

    private void handleDelete(BackupFile backup) {
        showDangerConfirmation("Supprimer cette sauvegarde?",
                "Fichier: " + backup.getFileName(),
                () -> {
                    try {
                        backupService.deleteBackup(backup.getPath());
                        loadBackups();
                        lblStatus.setText("Sauvegarde supprimee");
                    } catch (Exception e) {
                        logger.error("Erreur suppression backup", e);
                        showError("Impossible de supprimer", e.getMessage());
                    }
                });
    }

    @FXML
    private void handleRefresh() {
        loadBackups();
        lblStatus.setText("Liste actualisee");
    }

    private void setLoading(boolean loading, String message) {
        progressIndicator.setVisible(loading);
        lblStatus.setText(message);
    }
}
