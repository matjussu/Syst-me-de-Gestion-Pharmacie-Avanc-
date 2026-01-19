-- =============================================================================
-- SGPA - Table d'audit des actions
-- Script a executer pour ajouter le journal d'audit
-- =============================================================================

USE sgpa_pharmacie;

-- =============================================================================
-- Table: audit_log
-- Journal d'audit pour tracer toutes les actions importantes
-- =============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id_audit INT AUTO_INCREMENT PRIMARY KEY,
    date_action DATETIME DEFAULT CURRENT_TIMESTAMP,
    id_utilisateur INT,
    nom_utilisateur VARCHAR(50),
    type_action ENUM('CONNEXION', 'DECONNEXION', 'CREATION', 'MODIFICATION', 'SUPPRESSION', 'VENTE', 'COMMANDE', 'RECEPTION', 'AUTRE') NOT NULL,
    entite VARCHAR(50),  -- Type d'entite concernee (MEDICAMENT, LOT, UTILISATEUR, VENTE, COMMANDE, etc.)
    id_entite INT,       -- ID de l'entite concernee
    description TEXT,    -- Description detaillee de l'action
    details_json TEXT,   -- Details supplementaires en JSON (ancien/nouveau valeur, etc.)
    adresse_ip VARCHAR(45),
    FOREIGN KEY (id_utilisateur) REFERENCES utilisateurs(id_utilisateur) ON DELETE SET NULL,
    INDEX idx_audit_date (date_action),
    INDEX idx_audit_utilisateur (id_utilisateur),
    INDEX idx_audit_type (type_action),
    INDEX idx_audit_entite (entite, id_entite)
) ENGINE=InnoDB;

-- Index composite pour les recherches frequentes
CREATE INDEX idx_audit_recherche ON audit_log (date_action, type_action, entite);
