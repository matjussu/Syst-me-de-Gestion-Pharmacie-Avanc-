-- =============================================================================
-- SGPA - Tables pour la gestion des promotions
-- =============================================================================

-- Table principale des promotions
CREATE TABLE IF NOT EXISTS promotions (
    id_promotion INT AUTO_INCREMENT PRIMARY KEY,
    code_promo VARCHAR(50) UNIQUE,
    nom VARCHAR(100) NOT NULL,
    description TEXT,
    type_promotion ENUM('POURCENTAGE', 'MONTANT_FIXE', 'OFFRE_GROUPEE', 'PRIX_SPECIAL') NOT NULL,
    valeur DECIMAL(10,2) NOT NULL,
    quantite_requise INT DEFAULT 1 COMMENT 'Pour offre groupee: quantite a acheter',
    quantite_offerte INT DEFAULT 0 COMMENT 'Pour offre groupee: quantite gratuite',
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    actif BOOLEAN DEFAULT TRUE,
    usage_unique BOOLEAN DEFAULT FALSE COMMENT 'Si true, utilisable une seule fois par client',
    cumulable BOOLEAN DEFAULT FALSE COMMENT 'Si true, peut se cumuler avec autres promos',
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cree_par INT,
    FOREIGN KEY (cree_par) REFERENCES utilisateurs(id_utilisateur)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table de liaison promotions-medicaments
CREATE TABLE IF NOT EXISTS promotion_medicaments (
    id_promotion INT NOT NULL,
    id_medicament INT NOT NULL,
    PRIMARY KEY (id_promotion, id_medicament),
    FOREIGN KEY (id_promotion) REFERENCES promotions(id_promotion) ON DELETE CASCADE,
    FOREIGN KEY (id_medicament) REFERENCES medicaments(id_medicament) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table de tracabilite des utilisations
CREATE TABLE IF NOT EXISTS utilisation_promotions (
    id_utilisation INT AUTO_INCREMENT PRIMARY KEY,
    id_promotion INT NOT NULL,
    id_vente INT NOT NULL,
    id_ligne_vente INT,
    montant_reduction DECIMAL(10,2) NOT NULL,
    date_utilisation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_promotion) REFERENCES promotions(id_promotion),
    FOREIGN KEY (id_vente) REFERENCES ventes(id_vente),
    FOREIGN KEY (id_ligne_vente) REFERENCES ligne_ventes(id_ligne_vente)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Modification de la table ligne_ventes pour ajouter la tracabilite des promotions
ALTER TABLE ligne_ventes
    ADD COLUMN IF NOT EXISTS id_promotion INT NULL,
    ADD COLUMN IF NOT EXISTS montant_remise DECIMAL(10,2) DEFAULT 0;

-- Ajout de la cle etrangere si elle n'existe pas
-- Note: Verifier si la contrainte existe avant de l'ajouter
SET @constraint_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'ligne_ventes'
    AND CONSTRAINT_NAME = 'fk_ligne_ventes_promotion'
);

SET @sql = IF(@constraint_exists = 0,
    'ALTER TABLE ligne_ventes ADD CONSTRAINT fk_ligne_ventes_promotion FOREIGN KEY (id_promotion) REFERENCES promotions(id_promotion)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Index pour optimiser les recherches
CREATE INDEX IF NOT EXISTS idx_promotions_dates ON promotions(date_debut, date_fin);
CREATE INDEX IF NOT EXISTS idx_promotions_actif ON promotions(actif);
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code_promo);
CREATE INDEX IF NOT EXISTS idx_utilisation_vente ON utilisation_promotions(id_vente);
CREATE INDEX IF NOT EXISTS idx_utilisation_promotion ON utilisation_promotions(id_promotion);
