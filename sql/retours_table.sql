-- =============================================================================
-- SGPA - Table des retours produits
-- =============================================================================

-- Table des retours
CREATE TABLE IF NOT EXISTS retours (
    id_retour INT PRIMARY KEY AUTO_INCREMENT,
    id_vente INT NOT NULL,
    id_lot INT NOT NULL,
    id_utilisateur INT NOT NULL,
    quantite INT NOT NULL,
    motif VARCHAR(255) NOT NULL,
    date_retour DATETIME DEFAULT CURRENT_TIMESTAMP,
    reintegre BOOLEAN DEFAULT FALSE,
    commentaire TEXT,
    FOREIGN KEY (id_vente) REFERENCES ventes(id_vente),
    FOREIGN KEY (id_lot) REFERENCES lots(id_lot),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur),
    INDEX idx_retours_vente (id_vente),
    INDEX idx_retours_date (date_retour)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Commentaires
ALTER TABLE retours COMMENT = 'Retours de produits par les clients';
