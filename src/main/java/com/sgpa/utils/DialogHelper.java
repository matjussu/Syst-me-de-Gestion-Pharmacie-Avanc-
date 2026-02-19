package com.sgpa.utils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Systeme de dialogs personnalises themes pour ApotiCare.
 * Remplace les Alert JavaFX natifs qui cassent le theme dark.
 */
public class DialogHelper {

    private DialogHelper() {}

    // ========================= DIALOGS SIMPLES =========================

    /**
     * Affiche un dialog d'information.
     */
    public static void showInfo(StackPane owner, String title, String message) {
        showSimpleDialog(owner, title, message, "fas-info-circle", "info");
    }

    /**
     * Affiche un dialog de succes.
     */
    public static void showSuccess(StackPane owner, String title, String message) {
        showSimpleDialog(owner, title, message, "fas-check-circle", "success");
    }

    /**
     * Affiche un dialog d'avertissement.
     */
    public static void showWarning(StackPane owner, String title, String message) {
        showSimpleDialog(owner, title, message, "fas-exclamation-triangle", "warning");
    }

    /**
     * Affiche un dialog d'erreur.
     */
    public static void showError(StackPane owner, String title, String message) {
        showSimpleDialog(owner, title, message, "fas-times-circle", "error");
    }

    // ========================= CONFIRMATIONS =========================

    /**
     * Affiche un dialog de confirmation avec boutons "Confirmer" / "Annuler".
     */
    public static void showConfirmation(StackPane owner, String title, String message,
                                         Runnable onConfirm, Runnable onCancel) {
        showConfirmation(owner, title, message, "Confirmer", "Annuler", onConfirm, onCancel);
    }

    /**
     * Affiche un dialog de confirmation avec labels de boutons personnalises.
     */
    public static void showConfirmation(StackPane owner, String title, String message,
                                         String confirmLabel, String cancelLabel,
                                         Runnable onConfirm, Runnable onCancel) {
        StackPane overlay = createOverlay();
        VBox dialog = createDialogBox();

        FontIcon icon = new FontIcon("fas-question-circle");
        icon.getStyleClass().addAll("custom-dialog-icon", "confirm");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("custom-dialog-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("custom-dialog-message");
        messageLabel.setWrapText(true);

        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.getStyleClass().add("custom-dialog-btn-confirm");

        Button cancelBtn = new Button(cancelLabel);
        cancelBtn.getStyleClass().add("custom-dialog-btn-cancel");

        HBox buttons = new HBox(12, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(icon, titleLabel, messageLabel, buttons);
        overlay.getChildren().add(dialog);

        confirmBtn.setOnAction(e -> closeDialog(owner, overlay, onConfirm));
        cancelBtn.setOnAction(e -> closeDialog(owner, overlay, onCancel));

        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeDialog(owner, overlay, onCancel);
            } else if (e.getCode() == KeyCode.ENTER) {
                closeDialog(owner, overlay, onConfirm);
            }
        });

        owner.getChildren().add(overlay);
        overlay.requestFocus();
        confirmBtn.requestFocus();
        AnimationUtils.scaleIn(dialog);
    }

    /**
     * Affiche un dialog de confirmation pour action dangereuse (bouton rouge).
     */
    public static void showDangerConfirmation(StackPane owner, String title, String message,
                                               Runnable onConfirm, Runnable onCancel) {
        showDangerConfirmation(owner, title, message, "Confirmer", "Annuler", onConfirm, onCancel);
    }

    /**
     * Affiche un dialog de confirmation pour action dangereuse avec labels custom.
     */
    public static void showDangerConfirmation(StackPane owner, String title, String message,
                                               String confirmLabel, String cancelLabel,
                                               Runnable onConfirm, Runnable onCancel) {
        StackPane overlay = createOverlay();
        VBox dialog = createDialogBox();

        FontIcon icon = new FontIcon("fas-exclamation-triangle");
        icon.getStyleClass().addAll("custom-dialog-icon", "danger");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("custom-dialog-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("custom-dialog-message");
        messageLabel.setWrapText(true);

        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.getStyleClass().add("custom-dialog-btn-danger");

        Button cancelBtn = new Button(cancelLabel);
        cancelBtn.getStyleClass().add("custom-dialog-btn-cancel");

        HBox buttons = new HBox(12, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(icon, titleLabel, messageLabel, buttons);
        overlay.getChildren().add(dialog);

        confirmBtn.setOnAction(e -> closeDialog(owner, overlay, onConfirm));
        cancelBtn.setOnAction(e -> closeDialog(owner, overlay, onCancel));

        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeDialog(owner, overlay, onCancel);
            }
            // Pas de raccourci Enter pour les actions dangereuses (securite)
        });

        owner.getChildren().add(overlay);
        overlay.requestFocus();
        cancelBtn.requestFocus(); // Focus sur Annuler par defaut (securite)
        AnimationUtils.scaleIn(dialog);
    }

    // ========================= DIALOG AVEC CONTENU CUSTOM =========================

    /**
     * Affiche un dialog avec un contenu Node personnalise (ex: TableView).
     */
    public static void showCustomContent(StackPane owner, String title, Node content,
                                          String confirmLabel, String cancelLabel,
                                          Runnable onConfirm, Runnable onCancel) {
        StackPane overlay = createOverlay();
        VBox dialog = createDialogBox();
        dialog.setMaxWidth(560);

        FontIcon icon = new FontIcon("fas-clipboard-list");
        icon.getStyleClass().addAll("custom-dialog-icon", "info");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("custom-dialog-title");

        Button confirmBtn = new Button(confirmLabel);
        confirmBtn.getStyleClass().add("custom-dialog-btn-confirm");

        Button cancelBtn = new Button(cancelLabel);
        cancelBtn.getStyleClass().add("custom-dialog-btn-cancel");

        HBox buttons = new HBox(12, cancelBtn, confirmBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(icon, titleLabel, content, buttons);
        overlay.getChildren().add(dialog);

        confirmBtn.setOnAction(e -> closeDialog(owner, overlay, onConfirm));
        cancelBtn.setOnAction(e -> closeDialog(owner, overlay, onCancel));

        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                closeDialog(owner, overlay, onCancel);
            } else if (e.getCode() == KeyCode.ENTER) {
                closeDialog(owner, overlay, onConfirm);
            }
        });

        owner.getChildren().add(overlay);
        overlay.requestFocus();
        confirmBtn.requestFocus();
        AnimationUtils.scaleIn(dialog);
    }

    // ========================= UTILITAIRES INTERNES =========================

    private static void showSimpleDialog(StackPane owner, String title, String message,
                                          String iconLiteral, String iconStyleClass) {
        StackPane overlay = createOverlay();
        VBox dialog = createDialogBox();

        FontIcon icon = new FontIcon(iconLiteral);
        icon.getStyleClass().addAll("custom-dialog-icon", iconStyleClass);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("custom-dialog-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("custom-dialog-message");
        messageLabel.setWrapText(true);

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("custom-dialog-btn-confirm");

        HBox buttons = new HBox(okBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        dialog.getChildren().addAll(icon, titleLabel, messageLabel, buttons);
        overlay.getChildren().add(dialog);

        okBtn.setOnAction(e -> closeDialog(owner, overlay, null));

        // Clic sur overlay ferme les dialogs simples
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeDialog(owner, overlay, null);
            }
        });

        overlay.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE || e.getCode() == KeyCode.ENTER) {
                closeDialog(owner, overlay, null);
            }
        });

        owner.getChildren().add(overlay);
        overlay.requestFocus();
        okBtn.requestFocus();
        AnimationUtils.scaleIn(dialog);
    }

    private static StackPane createOverlay() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("custom-dialog-overlay");
        overlay.setAlignment(Pos.CENTER);
        return overlay;
    }

    private static VBox createDialogBox() {
        VBox dialog = new VBox(16);
        dialog.getStyleClass().add("custom-dialog");
        dialog.setAlignment(Pos.CENTER_LEFT);
        dialog.setMaxWidth(420);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setPadding(new Insets(30));
        return dialog;
    }

    private static void closeDialog(StackPane owner, StackPane overlay, Runnable callback) {
        if (!overlay.getChildren().isEmpty()) {
            AnimationUtils.scaleOut(overlay.getChildren().get(0), () -> {
                owner.getChildren().remove(overlay);
                if (callback != null) callback.run();
            });
        } else {
            owner.getChildren().remove(overlay);
            if (callback != null) callback.run();
        }
    }
}
