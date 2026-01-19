-- =============================================================================
-- SGPA - Systeme de Gestion Pharmacie Avance
-- Script de creation de la base de donnees
-- =============================================================================

CREATE DATABASE IF NOT EXISTS sgpa_pharmacie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE sgpa_pharmacie;

-- =============================================================================
-- Table: utilisateurs
-- Gestion des comptes utilisateurs avec roles
-- =============================================================================
CREATE TABLE IF NOT EXISTS utilisateurs (
    id_utilisateur INT AUTO_INCREMENT PRIMARY KEY,
    nom_utilisateur VARCHAR(50) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,  -- Hash BCrypt
    role ENUM('PHARMACIEN', 'PREPARATEUR') NOT NULL,
    nom_complet VARCHAR(100),
    actif BOOLEAN DEFAULT TRUE,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    derniere_connexion DATETIME,
    INDEX idx_utilisateur_role (role),
    INDEX idx_utilisateur_actif (actif)
) ENGINE=InnoDB;

-- =============================================================================
-- Table: fournisseurs
-- Informations sur les fournisseurs de medicaments
-- =============================================================================
CREATE TABLE IF NOT EXISTS fournisseurs (
    id_fournisseur INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    contact VARCHAR(100),
    adresse TEXT,
    telephone VARCHAR(20),
    email VARCHAR(100),
    actif BOOLEAN DEFAULT TRUE,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fournisseur_nom (nom),
    INDEX idx_fournisseur_actif (actif)
) ENGINE=InnoDB;

-- =============================================================================
-- Table: medicaments
-- Fiche produit des medicaments (sans stock physique)
-- =============================================================================
CREATE TABLE IF NOT EXISTS medicaments (
    id_medicament INT AUTO_INCREMENT PRIMARY KEY,
    nom_commercial VARCHAR(100) NOT NULL,
    principe_actif VARCHAR(100),
    forme_galenique VARCHAR(50),
    dosage VARCHAR(50),
    prix_public DECIMAL(10, 2) NOT NULL,
    necessite_ordonnance BOOLEAN DEFAULT FALSE,
    seuil_min INT DEFAULT 10,
    description TEXT,
    actif BOOLEAN DEFAULT TRUE,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_modification DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_medicament_nom (nom_commercial),
    INDEX idx_medicament_principe (principe_actif),
    INDEX idx_medicament_ordonnance (necessite_ordonnance),
    INDEX idx_medicament_actif (actif)
) ENGINE=InnoDB;

-- =============================================================================
-- Table: lots
-- Stock physique avec gestion de la peremption (FEFO)
-- Separation du stock physique de la fiche produit pour tracabilite
-- =============================================================================
CREATE TABLE IF NOT EXISTS lots (
    id_lot INT AUTO_INCREMENT PRIMARY KEY,
    id_medicament INT NOT NULL,
    id_fournisseur INT,
    numero_lot VARCHAR(50),  -- Numero de lot fabricant pour tracabilite
    date_peremption DATE NOT NULL,
    date_fabrication DATE,
    date_reception DATETIME DEFAULT CURRENT_TIMESTAMP,
    quantite_stock INT NOT NULL DEFAULT 0,
    prix_achat DECIMAL(10, 2),
    FOREIGN KEY (id_medicament) REFERENCES medicaments(id_medicament) ON DELETE RESTRICT,
    FOREIGN KEY (id_fournisseur) REFERENCES fournisseurs(id_fournisseur) ON DELETE SET NULL,
    INDEX idx_lot_medicament (id_medicament),
    INDEX idx_lot_peremption (date_peremption),
    INDEX idx_lot_fournisseur (id_fournisseur),
    INDEX idx_lot_medicament_peremption (id_medicament, date_peremption)  -- Index composite pour FEFO
) ENGINE=InnoDB;

-- =============================================================================
-- Table: ventes
-- En-tete des ventes
-- =============================================================================
CREATE TABLE IF NOT EXISTS ventes (
    id_vente INT AUTO_INCREMENT PRIMARY KEY,
    date_vente DATETIME DEFAULT CURRENT_TIMESTAMP,
    montant_total DECIMAL(10, 2) NOT NULL,
    est_sur_ordonnance BOOLEAN DEFAULT FALSE,
    numero_ordonnance VARCHAR(50),
    id_utilisateur INT,
    notes TEXT,
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur) ON DELETE SET NULL,
    INDEX idx_vente_date (date_vente),
    INDEX idx_vente_utilisateur (id_utilisateur),
    INDEX idx_vente_ordonnance (est_sur_ordonnance)
) ENGINE=InnoDB;

-- =============================================================================
-- Table: ligne_ventes
-- Details des ventes avec tracabilite par lot
-- =============================================================================
CREATE TABLE IF NOT EXISTS ligne_ventes (
    id_ligne INT AUTO_INCREMENT PRIMARY KEY,
    id_vente INT NOT NULL,
    id_lot INT NOT NULL,
    quantite INT NOT NULL,
    prix_unitaire_applique DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (id_vente) REFERENCES ventes(id_vente) ON DELETE CASCADE,
    FOREIGN KEY (id_lot) REFERENCES lots(id_lot) ON DELETE RESTRICT,
    INDEX idx_ligne_vente (id_vente),
    INDEX idx_ligne_lot (id_lot)
) ENGINE=InnoDB;

-- =============================================================================
-- Table: commandes
-- En-tete des commandes fournisseurs
-- =============================================================================
CREATE TABLE IF NOT EXISTS commandes (
    id_commande INT AUTO_INCREMENT PRIMARY KEY,
    date_creation DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_reception DATETIME,
    statut ENUM('EN_ATTENTE', 'RECUE', 'ANNULEE') DEFAULT 'EN_ATTENTE',
    id_fournisseur INT NOT NULL,
    notes TEXT,
    FOREIGN KEY (id_fournisseur) REFERENCES fournisseurs(id_fournisseur) ON DELETE RESTRICT,
    INDEX idx_commande_statut (statut),
    INDEX idx_commande_fournisseur (id_fournisseur),
    INDEX idx_commande_date (date_creation)
) ENGINE=InnoDB;

-- =============================================================================
-- Table: ligne_commandes
-- Details des commandes
-- =============================================================================
CREATE TABLE IF NOT EXISTS ligne_commandes (
    id_ligne_cmd INT AUTO_INCREMENT PRIMARY KEY,
    id_commande INT NOT NULL,
    id_medicament INT NOT NULL,
    quantite_commandee INT NOT NULL,
    quantite_recue INT DEFAULT 0,
    prix_unitaire DECIMAL(10, 2),
    FOREIGN KEY (id_commande) REFERENCES commandes(id_commande) ON DELETE CASCADE,
    FOREIGN KEY (id_medicament) REFERENCES medicaments(id_medicament) ON DELETE RESTRICT,
    INDEX idx_ligne_cmd_commande (id_commande),
    INDEX idx_ligne_cmd_medicament (id_medicament)
) ENGINE=InnoDB;

-- =============================================================================
-- Donnees de test initiales
-- =============================================================================

-- Utilisateur admin par defaut (mot de passe: admin123 - hash BCrypt)
-- Mot de passe: admin123 (hash BCrypt)
INSERT INTO utilisateurs (nom_utilisateur, mot_de_passe, role, nom_complet) VALUES
('admin', '$2a$10$yq8eXehbiYjmAKnwAN3CiOus71f7VMnjuJJSxFG2b.mFPpR2Zn8eS', 'PHARMACIEN', 'Administrateur'),
('preparateur1', '$2a$10$yq8eXehbiYjmAKnwAN3CiOus71f7VMnjuJJSxFG2b.mFPpR2Zn8eS', 'PREPARATEUR', 'Jean Preparateur');

-- Fournisseurs
INSERT INTO fournisseurs (nom, contact, telephone, email) VALUES
('Pharma Distribution', 'Service Commercial', '01 23 45 67 89', 'contact@pharmadist.fr'),
('MediSupply', 'Jean Dupont', '01 98 76 54 32', 'jdupont@medisupply.fr'),
('Grossiste Medical', 'Marie Martin', '01 11 22 33 44', 'mmartin@grossistemedical.fr');

-- Medicaments
INSERT INTO medicaments (nom_commercial, principe_actif, forme_galenique, dosage, prix_public, necessite_ordonnance, seuil_min) VALUES
('Doliprane', 'Paracetamol', 'Comprime', '1000mg', 2.50, FALSE, 20),
('Advil', 'Ibuprofene', 'Comprime', '400mg', 4.50, FALSE, 15),
('Amoxicilline Biogaran', 'Amoxicilline', 'Gelule', '500mg', 6.80, TRUE, 10),
('Ventoline', 'Salbutamol', 'Aerosol', '100µg/dose', 3.50, TRUE, 5),
('Levothyrox', 'Levothyroxine', 'Comprime', '50µg', 2.30, TRUE, 10),
('Spasfon', 'Phloroglucinol', 'Comprime', '80mg', 3.20, FALSE, 15),
('Gaviscon', 'Alginate de sodium', 'Suspension', '500mg/10ml', 5.90, FALSE, 10),
('Kardegic', 'Acetylsalicylique', 'Sachet', '75mg', 2.80, TRUE, 10);

-- Lots avec dates de peremption variees
INSERT INTO lots (id_medicament, id_fournisseur, numero_lot, date_peremption, date_fabrication, quantite_stock, prix_achat) VALUES
-- Doliprane - plusieurs lots avec differentes dates
(1, 1, 'DOL2024001', DATE_ADD(CURDATE(), INTERVAL 6 MONTH), DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 50, 1.20),
(1, 1, 'DOL2024002', DATE_ADD(CURDATE(), INTERVAL 12 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 100, 1.25),
(1, 2, 'DOL2024003', DATE_ADD(CURDATE(), INTERVAL 2 MONTH), DATE_SUB(CURDATE(), INTERVAL 10 MONTH), 25, 1.15),
-- Advil
(2, 1, 'ADV2024001', DATE_ADD(CURDATE(), INTERVAL 8 MONTH), DATE_SUB(CURDATE(), INTERVAL 4 MONTH), 40, 2.10),
(2, 2, 'ADV2024002', DATE_ADD(CURDATE(), INTERVAL 4 MONTH), DATE_SUB(CURDATE(), INTERVAL 8 MONTH), 30, 2.00),
-- Amoxicilline
(3, 1, 'AMX2024001', DATE_ADD(CURDATE(), INTERVAL 10 MONTH), DATE_SUB(CURDATE(), INTERVAL 2 MONTH), 60, 3.50),
(3, 3, 'AMX2024002', DATE_ADD(CURDATE(), INTERVAL 1 MONTH), DATE_SUB(CURDATE(), INTERVAL 11 MONTH), 15, 3.40),
-- Ventoline
(4, 2, 'VEN2024001', DATE_ADD(CURDATE(), INTERVAL 18 MONTH), DATE_SUB(CURDATE(), INTERVAL 6 MONTH), 20, 1.80),
-- Levothyrox
(5, 1, 'LEV2024001', DATE_ADD(CURDATE(), INTERVAL 9 MONTH), DATE_SUB(CURDATE(), INTERVAL 3 MONTH), 45, 1.10),
-- Spasfon
(6, 3, 'SPA2024001', DATE_ADD(CURDATE(), INTERVAL 15 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 70, 1.60),
-- Gaviscon
(7, 2, 'GAV2024001', DATE_ADD(CURDATE(), INTERVAL 7 MONTH), DATE_SUB(CURDATE(), INTERVAL 5 MONTH), 35, 3.00),
-- Kardegic
(8, 1, 'KAR2024001', DATE_ADD(CURDATE(), INTERVAL 11 MONTH), DATE_SUB(CURDATE(), INTERVAL 1 MONTH), 55, 1.40);

-- =============================================================================
-- Vue pour les alertes de stock bas
-- =============================================================================
CREATE OR REPLACE VIEW v_stock_bas AS
SELECT
    m.id_medicament,
    m.nom_commercial,
    m.seuil_min,
    COALESCE(SUM(l.quantite_stock), 0) AS stock_total
FROM medicaments m
LEFT JOIN lots l ON m.id_medicament = l.id_medicament
WHERE m.actif = TRUE
GROUP BY m.id_medicament, m.nom_commercial, m.seuil_min
HAVING stock_total < m.seuil_min;

-- =============================================================================
-- Vue pour les lots proches de la peremption (< 3 mois)
-- =============================================================================
CREATE OR REPLACE VIEW v_lots_peremption_proche AS
SELECT
    l.id_lot,
    l.numero_lot,
    m.nom_commercial,
    l.date_peremption,
    l.quantite_stock,
    DATEDIFF(l.date_peremption, CURDATE()) AS jours_restants
FROM lots l
JOIN medicaments m ON l.id_medicament = m.id_medicament
WHERE l.date_peremption < DATE_ADD(CURDATE(), INTERVAL 3 MONTH)
  AND l.quantite_stock > 0
ORDER BY l.date_peremption ASC;
