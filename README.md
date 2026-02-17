# Analyse des Besoins - SGPA / ApotiCare

## Systeme de Gestion de Pharmacie Avancee

**Version :** 1.0
**Date :** 17 fevrier 2026
**Technologies :** Java 21 - JavaFX 21 - MySQL 8 - Architecture MVC + DAO

---

## Table des matieres

1. [Contexte et objectifs](#1-contexte-et-objectifs)
2. [Perimetre du systeme](#2-perimetre-du-systeme)
3. [Acteurs](#3-acteurs)
4. [Besoins fonctionnels](#4-besoins-fonctionnels)
5. [Besoins non-fonctionnels](#5-besoins-non-fonctionnels)
6. [Diagrammes UML](#6-diagrammes-uml)
7. [Contraintes techniques](#7-contraintes-techniques)
8. [Glossaire](#8-glossaire)

---

## 1. Contexte et objectifs

### 1.1 Contexte

La gestion d'une pharmacie implique la manipulation de milliers de references medicamenteuses, de dizaines de fournisseurs, et un imperatif legal et sanitaire fort : ne jamais vendre un medicament perime et assurer la tracabilite complete de chaque unite vendue.

Les systemes existants presentent des lacunes : absence de gestion automatique de l'ordre de destockage (FEFO), faible tracabilite des lots, et absence d'outils d'aide a la decision pour le reapprovisionnement.

SGPA (Systeme de Gestion de Pharmacie Avancee), egalement nomme **ApotiCare**, est une application desktop JavaFX qui repond a ces besoins avec une architecture logicielle stricte (MVC + DAO) et un ensemble de fonctionnalites metier avancees.

### 1.2 Objectifs

- Automatiser le point de vente avec tracabilite par lot
- Imposer l'algorithme FEFO (First Expired, First Out) pour eliminer le risque de vente de produits perimes
- Fournir une surveillance en temps reel des stocks et des peremptions
- Offrir aux pharmaciens des outils d'analyse (statistiques, predictions) pour anticiper les ruptures
- Garantir la securite des donnees et l'auditabilite de toutes les actions

---

## 2. Perimetre du systeme

### Le systeme couvre :

- L'authentification et la gestion des droits d'acces par role
- Le point de vente (ventes, retours, historique)
- La gestion du stock physique (lots, inventaire avec ecarts)
- Le catalogue medicaments et les commandes fournisseurs
- Les alertes automatiques (stock bas, peremption proche, lots perimes)
- Les predictions de reapprovisionnement par algorithme
- La generation de rapports PDF et exports CSV/Excel
- L'administration (utilisateurs, journal d'audit, sauvegardes, configuration)

### Le systeme ne couvre pas :

- La gestion de la facturation client (tiers-payant, assurances)
- L'integration avec des systemes externes (ordonnances electroniques, systemes hospitaliers)
- La gestion multi-sites (plusieurs pharmacies)
- La vente en ligne

---

## 3. Acteurs

| Acteur | Type | Description |
|--------|------|-------------|
| **Preparateur** | Humain (principal) | Operateur de vente. Acces limite : ventes, historique, consultation des alertes. |
| **Pharmacien** | Humain (etendu) | Responsable de la pharmacie. Acces complet au systeme. Herite de tous les droits du Preparateur. |
| **Systeme** | Automatique | Processus internes : verification des seuils de stock, detection des peremptions, calcul des predictions de reapprovisionnement. Declenche a l'ouverture des vues ou a la demande. |

> **Note :** Le Pharmacien **etend** le role Preparateur : tout ce qu'un Preparateur peut faire, un Pharmacien peut le faire aussi, en plus de ses droits supplementaires.

---

## 4. Besoins fonctionnels

### 4.1 Authentification (BF-01 a BF-03)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-01 | Se connecter avec identifiants (login + mot de passe) | Preparateur, Pharmacien | Essentiel |
| BF-02 | Verifier le mot de passe via hachage BCrypt | Systeme | Essentiel |
| BF-03 | Se deconnecter | Preparateur, Pharmacien | Essentiel |

**Regle metier :** Les mots de passe sont haches avec jBCrypt (sel automatique). Aucun mot de passe en clair n'est stocke en base de donnees.

### 4.2 Point de Vente (BF-04 a BF-09)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-04 | Composer un panier et enregistrer une vente | Preparateur | Essentiel |
| BF-05 | Appliquer l'algorithme FEFO automatiquement lors de la vente | Systeme | Essentiel |
| BF-06 | Imprimer un ticket de caisse (PDF) apres vente | Preparateur | Important |
| BF-07 | Consulter l'historique des ventes avec filtres (date, utilisateur) | Preparateur | Essentiel |
| BF-08 | Exporter l'historique des ventes en CSV | Preparateur | Utile |
| BF-09 | Enregistrer un retour produit avec motif et option de reintegration | Pharmacien | Important |

**Regle metier FEFO :** Lors d'une vente, les lots dont la date de peremption est la plus proche sont destockes en premier. Les lots perimes sont ignores automatiquement.

```
// Pseudo-code de l'algorithme FEFO
List<Lot> lots = lotDAO.findByMedicamentSortedByExpiration(medicamentId);
for (Lot lot : lots) {
    if (quantiteRestante <= 0) break;
    if (lot.isPerime()) continue;
    int aDeduire = Math.min(lot.getQuantiteStock(), quantiteRestante);
    lot.deduireStock(aDeduire);
    quantiteRestante -= aDeduire;
}
```

### 4.3 Gestion Stock et Catalogue (BF-10 a BF-15)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-10 | Creer, modifier, desactiver un medicament dans le catalogue | Pharmacien | Essentiel |
| BF-11 | Creer et gerer des lots de stock (avec numero de lot, date de peremption, fournisseur) | Pharmacien | Essentiel |
| BF-12 | Exporter le stock complet en CSV | Pharmacien | Utile |
| BF-13 | Demarrer une session d'inventaire physique | Pharmacien | Important |
| BF-14 | Saisir les comptages physiques avec calcul automatique des ecarts | Pharmacien | Important |
| BF-15 | Regulariser le stock a la fin de l'inventaire (avec motif obligatoire si ecart) | Pharmacien | Important |

**Motifs d'ecart possibles :** PERTE, CASSE, VOL, PEREMPTION, AJUSTEMENT, AUTRE.

### 4.4 Commandes Fournisseurs (BF-16 a BF-18)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-16 | Creer une commande fournisseur avec lignes de commande | Pharmacien | Essentiel |
| BF-17 | Receptionner une commande (mise a jour automatique du stock) | Pharmacien | Essentiel |
| BF-18 | Generer un bon de commande au format PDF | Pharmacien | Important |

**Cycle de vie d'une commande :** EN_ATTENTE -> RECUE ou ANNULEE.

### 4.5 Alertes et Surveillance (BF-19 a BF-22)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-19 | Afficher les medicaments en stock bas (sous le seuil minimum) | Preparateur | Essentiel |
| BF-20 | Afficher les lots a peremption proche (moins de 90 jours) | Preparateur | Essentiel |
| BF-21 | Afficher les lots perimes | Preparateur | Essentiel |
| BF-22 | Exporter les alertes au format PDF | Pharmacien | Utile |

**Regles metier :**
- Stock bas : `SUM(lots.quantite_stock) < medicament.seuil_min`
- Peremption proche : `lot.date_peremption < MAINTENANT + 90 jours`
- Lot perime : `lot.date_peremption < MAINTENANT`

### 4.6 Predictions et Statistiques (BF-23 a BF-26)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-23 | Calculer les predictions de reapprovisionnement basees sur l'historique des ventes | Systeme | Important |
| BF-24 | Afficher la date de rupture previsionnelle et la quantite suggeree par medicament | Pharmacien | Important |
| BF-25 | Afficher les graphiques statistiques de ventes (evolution, top medicaments) | Pharmacien | Utile |
| BF-26 | Exporter les predictions en PDF | Pharmacien | Utile |

**Algorithme de prediction :**
- Consommation journaliere moyenne calculee sur une periode configurable (defaut : 90 jours)
- Niveaux d'urgence determines automatiquement :
  - **RUPTURE** : stock deja epuise (0 jours)
  - **CRITIQUE** : rupture dans 7 jours ou moins
  - **URGENT** : rupture entre 8 et 14 jours
  - **ATTENTION** : rupture entre 15 et 28 jours
  - **OK** : rupture au-dela de 28 jours

### 4.7 Administration (BF-27 a BF-34)

| ID | Intitule | Acteur | Priorite |
|----|----------|--------|----------|
| BF-27 | Creer, modifier, desactiver un compte utilisateur | Pharmacien | Essentiel |
| BF-28 | Changer le mot de passe d'un utilisateur | Pharmacien | Essentiel |
| BF-29 | Consulter le journal d'audit avec filtres (date, utilisateur, type d'action) | Pharmacien | Important |
| BF-30 | Exporter le journal d'audit en CSV | Pharmacien | Utile |
| BF-31 | Sauvegarder la base de donnees (via mysqldump) | Pharmacien | Important |
| BF-32 | Restaurer une sauvegarde | Pharmacien | Important |
| BF-33 | Configurer les parametres de l'application (nom pharmacie, seuils, repertoires) | Pharmacien | Important |
| BF-34 | Generer des exports de donnees en CSV/Excel | Pharmacien | Utile |

---

## 5. Besoins non-fonctionnels

### 5.1 Performance

| ID | Exigence |
|----|----------|
| BNF-01 | La recherche de medicaments doit retourner des resultats en moins de 500 ms |
| BNF-02 | L'enregistrement d'une vente (incluant FEFO et mise a jour des lots) doit se faire en moins de 2 secondes |
| BNF-03 | La generation des rapports PDF se fait de maniere asynchrone sans bloquer l'interface |
| BNF-04 | Le pool de connexions HikariCP limite les connexions simultanees a 10 |

### 5.2 Securite

| ID | Exigence |
|----|----------|
| BNF-05 | Tous les mots de passe sont haches avec BCrypt (facteur de cout >= 10) |
| BNF-06 | Toutes les requetes SQL utilisent des PreparedStatement (protection contre l'injection SQL) |
| BNF-07 | L'acces aux fonctionnalites d'administration est controle par le role PHARMACIEN |
| BNF-08 | Toutes les actions importantes sont tracees dans le journal d'audit (utilisateur, horodatage, entite, description) |

### 5.3 Fiabilite et coherence des donnees

| ID | Exigence |
|----|----------|
| BNF-09 | Les operations multi-tables (vente + mise a jour des lots) sont encapsulees dans des transactions ACID avec commit/rollback explicite |
| BNF-10 | Tous les montants financiers sont stockes et traites avec `BigDecimal` (jamais `double` ni `float`) |
| BNF-11 | Les dates utilisent `LocalDate` / `LocalDateTime` (jamais `java.util.Date`) |

### 5.4 Maintenabilite

| ID | Exigence |
|----|----------|
| BNF-12 | Architecture MVC stricte : les controleurs n'appellent jamais les DAO directement |
| BNF-13 | Le flux de donnees suit le patron : Controller -> Service -> DAO -> Base de donnees |
| BNF-14 | Le logging utilise SLF4J + Logback pour la tracabilite des erreurs |
| BNF-15 | La configuration est externalisee dans `database.properties` et `config.properties` |

### 5.5 Utilisabilite

| ID | Exigence |
|----|----------|
| BNF-16 | Interface JavaFX avec animations de transition entre les vues |
| BNF-17 | Tous les dialogues de confirmation/erreur sont standardises via une classe utilitaire `DialogHelper` |
| BNF-18 | Les operations longues (exports, sauvegardes) s'executent dans un thread separe pour ne pas bloquer l'interface |

---

## 6. Diagrammes UML

### 6.1 Diagramme de classes (Modele Domaine)

Le diagramme de classes illustre la structure statique du modele domaine du systeme :

- **13 entites** : Utilisateur, Medicament, Lot, Fournisseur, Vente, LigneVente, Commande, LigneCommande, Retour, SessionInventaire, ComptageInventaire, Regularisation, AuditLog
- **5 enumerations** : Role, StatutCommande, StatutInventaire, MotifEcart, TypeAction
- **1 DTO** : PredictionReapprovisionnement (transport des donnees de prediction entre couches)
- **Relations** : compositions (Vente/LigneVente, Commande/LigneCommande, Medicament/Lot, Session/Comptage) et associations (FK vers Utilisateur, Lot, Fournisseur)

> Voir : [diagramme_classes.puml](diagramme_classes.puml)

### 6.2 Diagramme d'architecture en couches

Le diagramme d'architecture presente l'organisation du code en 4 couches selon le patron MVC + DAO :

- **Couche Controleur** : Classe abstraite `BaseController` et 16 controleurs JavaFX heritant, chacun lie a une vue FXML
- **Couche Service** : 14 services encapsulant la logique metier (algorithme FEFO, alertes, predictions, rapports PDF, exports CSV, sauvegardes)
- **Couche DAO** : Interface generique `GenericDAO<T, ID>` avec 7 methodes CRUD et 11 interfaces specialisees, implementees en JDBC
- **Base de donnees** : MySQL 8 avec 13 tables, connectee via le pool HikariCP

> Voir : [diagramme_architecture.puml](diagramme_architecture.puml)

### 6.3 Diagramme des cas d'utilisation

Le diagramme des cas d'utilisation presente les 34 besoins fonctionnels organises en 7 domaines fonctionnels :

- La hierarchie entre les acteurs (Pharmacien herite des droits du Preparateur)
- Les processus automatiques du Systeme (FEFO, alertes, predictions)
- Les relations `<<include>>` pour les comportements obligatoires (verification BCrypt, algorithme FEFO, calcul predictions)
- Les relations `<<extend>>` pour les comportements optionnels (impression PDF, export CSV)

> Voir : [diagramme_cas_utilisation.puml](diagramme_cas_utilisation.puml)

---

## 7. Contraintes techniques

| Contrainte | Detail |
|-----------|--------|
| **Langage** | Java 21 LTS |
| **Interface** | JavaFX 21.0.2 (avec ControlsFX 11.2.0 et Ikonli FontAwesome5 12.3.1) |
| **Base de donnees** | MySQL 8.0 (schema fourni dans `sql/schema.sql`) |
| **Pool de connexions** | HikariCP 5.1.0 (max 10 connexions simultanees) |
| **Hachage** | jBCrypt 0.4 |
| **PDF** | iText 7.2.5 - Repertoire de sortie : `~/SGPA_Rapports/` |
| **Excel** | Apache POI 5.2.5 (OOXML) |
| **Logging** | SLF4J 2.0.9 + Logback 1.4.12 |
| **Build** | Maven avec plugins javafx-maven-plugin et maven-shade-plugin |
| **Sauvegardes** | `mysqldump` / `mysql` en ligne de commande - Repertoire : `~/SGPA_Backups/` |
| **Configuration** | Fichier `database.properties` a renseigner avant le premier lancement |

---

## 8. Glossaire

| Terme | Definition |
|-------|-----------|
| **FEFO** | First Expired, First Out - Strategie de destockage qui consomme en priorite les lots dont la date de peremption est la plus proche |
| **Lot** | Unite physique de stock d'un medicament, identifiee par un numero de lot fabricant et une date de peremption |
| **Seuil minimum** | Quantite en dessous de laquelle une alerte de stock bas est declenchee pour un medicament |
| **Session d'inventaire** | Operation de comptage physique des stocks, permettant de comparer les quantites theoriques et reelles et de regulariser les ecarts |
| **Regularisation** | Ajustement du stock apres inventaire pour corriger un ecart constate, avec motif obligatoire |
| **BCrypt** | Algorithme de hachage de mots de passe avec sel aleatoire, resistant aux attaques par force brute |
| **PreparedStatement** | Instruction SQL parametree empechant les injections SQL |
| **MVC** | Model-View-Controller - Patron d'architecture separant les donnees, la logique et la presentation |
| **DAO** | Data Access Object - Couche d'abstraction isolant la logique SQL du code metier |
| **DTO** | Data Transfer Object - Objet de transport de donnees entre couches sans logique metier propre |
| **HikariCP** | Bibliotheque de pool de connexions JDBC haute performance |
| **Audit** | Journal automatique de toutes les actions importantes (connexions, ventes, modifications) avec horodatage et utilisateur |
| **PHARMACIEN** | Role avec acces complet au systeme, incluant l'administration et les rapports |
| **PREPARATEUR** | Role avec acces restreint aux operations de vente et consultation uniquement |
| **BigDecimal** | Type Java pour les calculs financiers precis, evitant les erreurs d'arrondi des flottants |
