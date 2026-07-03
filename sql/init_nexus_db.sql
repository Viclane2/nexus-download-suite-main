============================================================
--  NEXUS Download Suite — Script d'initialisation MySQL v3
--  Nouveautés : table utilisateurs + catégorisation
-- ============================================================

CREATE DATABASE IF NOT EXISTS nexus_download
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nexus_download;

-- ── Table des utilisateurs ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS utilisateurs (
     id                INT AUTO_INCREMENT  PRIMARY KEY,
    nom_utilisateur   VARCHAR(50)         NOT NULL UNIQUE,
    mot_de_passe      VARCHAR(64)         NOT NULL COMMENT 'SHA-256',
    date_creation     DATETIME            NOT NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table de l'historique des téléchargements ─────────────────────────────
CREATE TABLE IF NOT EXISTS historique_telechargements (
    id                  VARCHAR(36)     PRIMARY KEY,
    utilisateur_id      INT             NOT NULL DEFAULT -1,
    nom_fichier         VARCHAR(512)    NOT NULL,
    url_source          TEXT            NOT NULL,
    chemin_destination  VARCHAR(512)    NOT NULL,
    taille_mo           DOUBLE          DEFAULT -1,
    progression         DOUBLE          DEFAULT 0,
    octets_recus        BIGINT          DEFAULT 0,
    statut              VARCHAR(20)     NOT NULL,
    hash_sha256         VARCHAR(64)     DEFAULT NULL,
    date_debut          DATETIME,
    date_fin            DATETIME,
    INDEX idx_statut    (statut),
    INDEX idx_date      (date_debut),
    INDEX idx_user      (utilisateur_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Migration : si les tables existaient déjà, ajouter les colonnes manquantes
ALTER TABLE historique_telechargements
    ADD COLUMN IF NOT EXISTS octets_recus    BIGINT      DEFAULT 0    COMMENT 'Octets reçus (reprise)',
    ADD COLUMN IF NOT EXISTS hash_sha256     VARCHAR(64) DEFAULT NULL COMMENT 'SHA-256 fichier final',
    ADD COLUMN IF NOT EXISTS utilisateur_id  INT         DEFAULT -1   COMMENT 'ID utilisateur propriétaire';

SELECT 'Base nexus_download v3 prête !' AS message;
SELECT 'Tables : utilisateurs, historique_telechargements' AS info;