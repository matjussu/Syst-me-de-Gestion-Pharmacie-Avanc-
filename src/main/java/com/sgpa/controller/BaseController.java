package com.sgpa.controller;

import com.sgpa.model.Utilisateur;

/**
 * Classe de base pour tous les controleurs de vues.
 * Fournit les methodes communes pour la gestion de l'utilisateur
 * et la reference au dashboard.
 *
 * @author SGPA Team
 * @version 1.0
 */
public abstract class BaseController {

    protected Utilisateur currentUser;
    protected DashboardController dashboardController;

    /**
     * Definit l'utilisateur actuellement connecte.
     *
     * @param user l'utilisateur connecte
     */
    public void setCurrentUser(Utilisateur user) {
        this.currentUser = user;
        onUserSet();
    }

    /**
     * Definit la reference au controleur du dashboard.
     *
     * @param controller le controleur du dashboard
     */
    public void setDashboardController(DashboardController controller) {
        this.dashboardController = controller;
    }

    /**
     * Methode appelee apres que l'utilisateur a ete defini.
     * Les sous-classes peuvent la surcharger pour initialiser les donnees.
     */
    protected void onUserSet() {
        // Implementation par defaut vide
    }

    /**
     * Retourne l'utilisateur actuellement connecte.
     *
     * @return l'utilisateur connecte
     */
    public Utilisateur getCurrentUser() {
        return currentUser;
    }
}
