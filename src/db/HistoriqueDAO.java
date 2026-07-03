package db;

import core.StatutTache;
import core.TacheTelechargement;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) pour la table `historique_telechargements`.
 *
 * Opérations disponibles :
 *   - initialiserTable()     → crée la table si elle n'existe pas
 *   - sauvegarder(tache)     → INSERT ou UPDATE selon si l'id existe déjà
 *   - chargerTous()          → SELECT * de tout l'historique
 *   - supprimerParId(id)     → DELETE d'une tâche par son UUID
 *   - supprimerTout()        → DELETE de tout l'historique
 */
public class HistoriqueDAO {

    private static final String TABLE = "historique_telechargements";

    private static final String SQL_CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS historique_telechargements (
            id                  VARCHAR(36)     PRIMARY KEY,
            nom_fichier         VARCHAR(512)    NOT NULL,
            url_source          TEXT            NOT NULL,
            chemin_destination  VARCHAR(512)    NOT NULL,
            taille_mo           DOUBLE          DEFAULT -1,
            progression         DOUBLE          DEFAULT 0,
            statut              VARCHAR(20)     NOT NULL,
            date_debut          DATETIME,
            date_fin            DATETIME
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

    private static final String SQL_UPSERT = """
        INSERT INTO historique_telechargements
            (id, nom_fichier, url_source, chemin_destination, taille_mo, progression, statut, date_debut, date_fin)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
            taille_mo          = VALUES(taille_mo),
            progression        = VALUES(progression),
            statut             = VALUES(statut),
            date_debut         = VALUES(date_debut),
            date_fin           = VALUES(date_fin);
        """;

    private static final String SQL_SELECT_ALL = """
        SELECT id, nom_fichier, url_source, chemin_destination, taille_mo, progression, statut, date_debut, date_fin
        FROM historique_telechargements
        ORDER BY date_debut DESC;
        """;

    private static final String SQL_DELETE_BY_ID  = "DELETE FROM " + TABLE + " WHERE id = ?;";
    private static final String SQL_DELETE_ALL    = "DELETE FROM " + TABLE + ";";

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée la table si elle n'existe pas encore.
     * À appeler au démarrage de l'application.
     */
    public void initialiserTable() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) {
            System.err.println("[DAO] Initialisation ignorée : pas de connexion MySQL.");
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(SQL_CREATE_TABLE);
            System.out.println("[DAO] Table '" + TABLE + "' prête.");
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur création table : " + e.getMessage());
        }
    }

    /**
     * Sauvegarde ou met à jour une tâche dans la base.
     * Utilise ON DUPLICATE KEY UPDATE pour gérer INSERT + UPDATE en une seule requête.
     */
    public void sauvegarder(TacheTelechargement tache) {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return;

        try (PreparedStatement ps = conn.prepareStatement(SQL_UPSERT)) {
            ps.setString(1, tache.getId());
            ps.setString(2, tache.getNomFichier());
            ps.setString(3, tache.getUrlSource());
            ps.setString(4, tache.getCheminDestination());
            ps.setDouble(5, tache.getTailleTotaleMo());
            ps.setDouble(6, tache.getProgression());
            ps.setString(7, tache.getStatut().name());
            ps.setObject(8, tache.getDateDebut());   // LocalDateTime → DATETIME
            ps.setObject(9, tache.getDateFin());     // null si pas encore fini
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur sauvegarder : " + e.getMessage());
        }
    }

    /**
     * Charge tout l'historique depuis la base.
     * Retourne une liste vide si la connexion est indisponible.
     */
    public List<TacheTelechargement> chargerTous() {
        List<TacheTelechargement> liste = new ArrayList<>();
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return liste;

        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(SQL_SELECT_ALL)) {

            while (rs.next()) {
                String id               = rs.getString("id");
                String nomFichier       = rs.getString("nom_fichier");
                String urlSource        = rs.getString("url_source");
                String cheminDest       = rs.getString("chemin_destination");
                double tailleMo         = rs.getDouble("taille_mo");
                double progression      = rs.getDouble("progression");
                String statutStr        = rs.getString("statut");
                LocalDateTime dateDebut = rs.getObject("date_debut", LocalDateTime.class);
                LocalDateTime dateFin   = rs.getObject("date_fin",   LocalDateTime.class);

                StatutTache statut;
                try {
                    statut = StatutTache.valueOf(statutStr);
                } catch (IllegalArgumentException e) {
                    statut = StatutTache.ERREUR;
                }

                TacheTelechargement t = TacheTelechargement.depuisBD(
                        id, nomFichier, urlSource, cheminDest,
                        tailleMo, progression, statut, dateDebut, dateFin
                );
                liste.add(t);
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur chargerTous : " + e.getMessage());
        }
        return liste;
    }

    /**
     * Supprime une tâche de la base par son UUID.
     */
    public void supprimerParId(String id) {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return;

        try (PreparedStatement ps = conn.prepareStatement(SQL_DELETE_BY_ID)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur supprimerParId : " + e.getMessage());
        }
    }

    /**
     * Vide entièrement l'historique dans la base.
     */
    public void supprimerTout() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return;

        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(SQL_DELETE_ALL);
            System.out.println("[DAO] Historique MySQL vidé.");
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur supprimerTout : " + e.getMessage());
        }
    }
}