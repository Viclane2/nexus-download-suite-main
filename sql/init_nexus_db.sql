-- ============================================================
--  NEXUS Download Suite — Script d'initialisation MySQL v2
-- ============================================================

CREATE DATABASE IF NOT EXISTS nexus_download
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nexus_download;

-- Supprimer l'ancienne table si elle existe (migration)
-- DROP TABLE IF EXISTS historique_telechargements;

CREATE TABLE IF NOT EXISTS historique_telechargements (
                                                          id                  VARCHAR(36)     PRIMARY KEY             COMMENT 'UUID unique',
    nom_fichier         VARCHAR(512)    NOT NULL                COMMENT 'Nom du fichier',
    url_source          TEXT            NOT NULL                COMMENT 'URL source',
    chemin_destination  VARCHAR(512)    NOT NULL                COMMENT 'Dossier destination',
    taille_mo           DOUBLE          DEFAULT -1              COMMENT 'Taille en Mo',
    progression         DOUBLE          DEFAULT 0               COMMENT '0-100%',
    octets_recus        BIGINT          DEFAULT 0               COMMENT 'Octets déjà téléchargés (reprise)',
    statut              VARCHAR(20)     NOT NULL                COMMENT 'EN_ATTENTE|EN_COURS|TERMINE|ERREUR|ANNULE',
    hash_sha256         VARCHAR(64)     DEFAULT NULL            COMMENT 'SHA-256 du fichier final',
    date_debut          DATETIME                                COMMENT 'Début du téléchargement',
    date_fin            DATETIME                                COMMENT 'Fin du téléchargement',
    INDEX idx_statut    (statut),
    INDEX idx_date      (date_debut)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Si la table existait déjà sans les nouvelles colonnes, les ajouter :
ALTER TABLE historique_telechargements
    ADD COLUMN IF NOT EXISTS octets_recus BIGINT  DEFAULT 0    COMMENT 'Octets reçus (reprise)',
    ADD COLUMN IF NOT EXISTS hash_sha256  VARCHAR(64) DEFAULT NULL COMMENT 'SHA-256 fichier final';

SELECT 'Base nexus_download prête !' AS message;
