package com.sgpa.controller;

import com.sgpa.exception.ServiceException;
import com.sgpa.model.Utilisateur;
import com.sgpa.service.AuthenticationService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controleur pour l'ecran de connexion.
 * Gere l'authentification des utilisateurs via BCrypt.
 *
 * @author SGPA Team
 * @version 1.0
 */
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    @FXML
    private ProgressIndicator loadingIndicator;

    private final AuthenticationService authService;

    public LoginController() {
        this.authService = new AuthenticationService();
    }

    @FXML
    public void initialize() {
        // Focus sur le champ username au demarrage
        Platform.runLater(() -> usernameField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        // Desactiver le formulaire pendant l'authentification
        setFormDisabled(true);
        hideError();

        // Tache asynchrone pour ne pas bloquer l'UI
        Task<Utilisateur> loginTask = new Task<>() {
            @Override
            protected Utilisateur call() throws Exception {
                return authService.authenticate(username, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            Utilisateur user = loginTask.getValue();
            logger.info("Connexion reussie: {}", user.getNomComplet());
            openDashboard(user);
        });

        loginTask.setOnFailed(event -> {
            Throwable exception = loginTask.getException();
            logger.warn("Echec de connexion: {}", exception.getMessage());

            if (exception instanceof ServiceException) {
                showError(exception.getMessage());
            } else {
                showError("Erreur de connexion au serveur");
            }
            setFormDisabled(false);
            passwordField.clear();
            passwordField.requestFocus();
        });

        new Thread(loginTask).start();
    }

    private void openDashboard(Utilisateur user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
            Parent root = loader.load();

            // Passer l'utilisateur au dashboard
            DashboardController dashboardController = loader.getController();
            dashboardController.setCurrentUser(user);
            dashboardController.setAuthService(authService);

            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("SGPA - Tableau de Bord - " + user.getNomComplet());
            stage.centerOnScreen();
            stage.setMaximized(true);

        } catch (IOException e) {
            logger.error("Erreur lors du chargement du dashboard", e);
            showError("Erreur lors du chargement de l'application");
            setFormDisabled(false);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void setFormDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
        loadingIndicator.setVisible(disabled);
        loadingIndicator.setManaged(disabled);
    }
}
