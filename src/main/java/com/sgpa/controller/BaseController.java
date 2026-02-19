package com.sgpa.controller;

import com.sgpa.model.Utilisateur;
import com.sgpa.utils.AnimationUtils;
import com.sgpa.utils.DialogHelper;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BaseController {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    protected Utilisateur currentUser;
    protected DashboardController dashboardController;

    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
        onUserSet();
    }

    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    protected void onUserSet() {
    }

    /**
     * Appelee quand une vue cachee est re-affichee.
     * Les sous-controleurs peuvent surcharger pour rafraichir les donnees.
     */
    public void onViewDisplayed() {
    }

    public Utilisateur getCurrentUser() {
        return currentUser;
    }

    protected static ExecutorService getExecutor() {
        return EXECUTOR;
    }

    protected void runAsync(javafx.concurrent.Task<?> task) {
        EXECUTOR.submit(task);
    }

    // --- Methodes de dialog utilitaires ---

    protected StackPane getDialogOwner() {
        return dashboardController.getContentArea();
    }

    protected void showSuccess(String title, String message) {
        DialogHelper.showSuccess(getDialogOwner(), title, message);
    }

    protected void showError(String title, String message) {
        DialogHelper.showError(getDialogOwner(), title, message);
    }

    protected void showWarning(String title, String message) {
        DialogHelper.showWarning(getDialogOwner(), title, message);
    }

    protected void showInfo(String title, String message) {
        DialogHelper.showInfo(getDialogOwner(), title, message);
    }

    protected void showConfirmation(String title, String message, Runnable onConfirm) {
        DialogHelper.showConfirmation(getDialogOwner(), title, message, onConfirm, null);
    }

    protected void showConfirmation(String title, String message,
                                     String confirmLabel, String cancelLabel,
                                     Runnable onConfirm, Runnable onCancel) {
        DialogHelper.showConfirmation(getDialogOwner(), title, message, confirmLabel, cancelLabel, onConfirm, onCancel);
    }

    protected void showDangerConfirmation(String title, String message, Runnable onConfirm) {
        DialogHelper.showDangerConfirmation(getDialogOwner(), title, message, onConfirm, null);
    }

    protected void showDangerConfirmation(String title, String message,
                                           String confirmLabel, String cancelLabel,
                                           Runnable onConfirm, Runnable onCancel) {
        DialogHelper.showDangerConfirmation(getDialogOwner(), title, message, confirmLabel, cancelLabel, onConfirm, onCancel);
    }

    protected void showCustomContent(String title, Node content,
                                      String confirmLabel, String cancelLabel,
                                      Runnable onConfirm, Runnable onCancel) {
        DialogHelper.showCustomContent(getDialogOwner(), title, content, confirmLabel, cancelLabel, onConfirm, onCancel);
    }

    // --- Methodes d'animation utilitaires ---

    protected void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(100), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    protected void fadeOut(Node node, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(Duration.millis(100), node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(0);
        if (onFinished != null) {
            ft.setOnFinished(e -> onFinished.run());
        }
        ft.play();
    }

    protected void slideInFromRight(Node node) {
        node.setOpacity(0);
        node.setTranslateX(15);
        FadeTransition fade = new FadeTransition(Duration.millis(100), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(100), node);
        slide.setFromX(15);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        fade.play();
        slide.play();
    }

    protected void staggerFadeIn(Node... nodes) {
        AnimationUtils.staggerSlideIn(nodes);
    }

    protected void scaleOnHover(Node node, double scaleFactor) {
        AnimationUtils.applyHoverScale(node, scaleFactor);
    }

    protected void setupResponsiveTable(TableView<?> tableView) {
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
    }

    // --- Export utilitaires ---

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    @FunctionalInterface
    protected interface ExportFunction {
        String export() throws Exception;
    }

    protected void executeExport(ExportFunction exportFn, String successTitle, boolean openFile) {
        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
            @Override
            protected String call() throws Exception {
                return exportFn.export();
            }
        };
        task.setOnSucceeded(e -> {
            String filePath = task.getValue();
            if (openFile && filePath != null) {
                try {
                    File f = new File(filePath);
                    if (Desktop.isDesktopSupported() && f.exists()) {
                        Desktop.getDesktop().open(f);
                    }
                } catch (Exception ex) {
                    logger.warn("Impossible d'ouvrir le fichier: {}", filePath, ex);
                }
            }
            Platform.runLater(() -> showSuccess(successTitle, "Fichier genere:\n" + filePath));
        });
        task.setOnFailed(e -> {
            logger.error("Erreur lors de l'export", task.getException());
            Platform.runLater(() -> showError("Erreur d'export", "Une erreur est survenue lors de la generation du fichier."));
        });
        runAsync(task);
    }
}
