-- =====================================================
-- SGPA - Tables pour la gestion des inventaires
-- =====================================================

-- Table des sessions d'inventaire
CREATE TABLE IF NOT EXISTS sessions_inventaire (
    id_session INT PRIMARY KEY AUTO_INCREMENT,
    date_debut DATETIME DEFAULT CURRENT_TIMESTAMP,
    date_fin DATETIME NULL,
    statut ENUM('EN_COURS', 'TERMINEE', 'ANNULEE') DEFAULT 'EN_COURS',
    id_utilisateur INT NOT NULL,
    notes TEXT,
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur),
    INDEX idx_session_statut (statut),
    INDEX idx_session_date (date_debut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table des comptages physiques
CREATE TABLE IF NOT EXISTS comptages_inventaire (
    id_comptage INT PRIMARY KEY AUTO_INCREMENT,
    id_session INT NOT NULL,
    id_lot INT NOT NULL,
    quantite_theorique INT NOT NULL,
    quantite_physique INT NOT NULL,
    ecart INT NOT NULL,
    motif_ecart VARCHAR(50),
    commentaire TEXT,
    date_comptage DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_utilisateur INT NOT NULL,
    FOREIGN KEY (id_session) REFERENCES sessions_inventaire(id_session) ON DELETE CASCADE,
    FOREIGN KEY (id_lot) REFERENCES lots(id_lot),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur),
    UNIQUE KEY idx_session_lot (id_session, id_lot),
    INDEX idx_comptage_session (id_session),
    INDEX idx_comptage_ecart (ecart)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table des regularisations de stock
CREATE TABLE IF NOT EXISTS regularisations (
    id_regularisation INT PRIMARY KEY AUTO_INCREMENT,
    id_session INT NOT NULL,
    id_lot INT NOT NULL,
    quantite_ancienne INT NOT NULL,
    quantite_nouvelle INT NOT NULL,
    raison VARCHAR(50) NOT NULL,
    justificatif TEXT,
    date_regularisation DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_utilisateur INT NOT NULL,
    FOREIGN KEY (id_session) REFERENCES sessions_inventaire(id_session) ON DELETE CASCADE,
    FOREIGN KEY (id_lot) REFERENCES lots(id_lot),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur),
    INDEX idx_regularisation_session (id_session),
    INDEX idx_regularisation_lot (id_lot),
    INDEX idx_regularisation_raison (raison)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
