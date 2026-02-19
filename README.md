# APOTICARE - Gestion de Pharmacie

Bienvenue sur le projet ApotiCare.

## COMMENT LANCER L'APPLICATION ?

Nous avons créé un script automatique pour vous simplifier la vie.

1.  Assurez-vous d'avoir les outils suivants installés :
    -   **Java 21**
    -   **Maven**
    -   **MySQL Server**
2.  Double-cliquez simplement sur le fichier :
    👉 **`lanceur_auto.bat`**
3.  Suivez les instructions à l'écran (mot de passe MySQL, etc.).

Le script va tout faire : configurer la base de données, importer les données de test, et lancer l'application.

## AVANCÉ (MANUEL)
Si vous préférez la ligne de commande :
```bash
# Configuration BDD (si besoin)
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/seed_data.sql

# Lancement
mvn clean javafx:run
```
