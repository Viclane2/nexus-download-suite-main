package db;

import model.Utilisateur;

import java.security.MessageDigest;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * DAO pour la table `utilisateurs`.
 * Gère l'inscription, la connexion et la création de table.
 *
 * Les mots de passe sont hachés en SHA-256 avant stockage.
 * Ne jamais stocker de mots de passe en clair.
 */
public class UtilisateurDAO {

    private static final String SQL_CREATE_TABLE = """
        CREATE TABLE IF NOT EXISTS utilisateurs (
            id                INT AUTO_INCREMENT  PRIMARY KEY,
            nom_utilisateur   VARCHAR(50)         NOT NULL UNIQUE,
            mot_de_passe      VARCHAR(64)         NOT NULL  COMMENT 'SHA-256',
            date_creation     DATETIME            NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """;

    private static final String SQL_INSERT = """
        INSERT INTO utilisateurs (nom_utilisateur, mot_de_passe, date_creation)
        VALUES (?, ?, ?);
        """;

    private static final String SQL_FIND_BY_LOGIN = """
        SELECT id, nom_utilisateur, date_creation
        FROM utilisateurs
        WHERE nom_utilisateur = ? AND mot_de_passe = ?;
        """;

    private static final String SQL_EXISTS = """
        SELECT COUNT(*) FROM utilisateurs WHERE nom_utilisateur = ?;
        """;

    // ─────────────────────────────────────────────────────────────────────────

    /** Crée la table si elle n'existe pas. */
    public void initialiserTable() {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(SQL_CREATE_TABLE);
            System.out.println("[DAO] Table 'utilisateurs' prête.");
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur création table utilisateurs : " + e.getMessage());
        }
    }

    /**
     * Inscrit un nouvel utilisateur.
     * @return true si l'inscription a réussi, false si le nom existe déjà.
     */
    public boolean inscrire(String nomUtilisateur, String motDePasse) {
        if (nomUtilisateur == null || nomUtilisateur.isBlank()
                || motDePasse == null || motDePasse.length() < 4) {
            return false;
        }
        if (nomExiste(nomUtilisateur)) return false;

        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return false;

        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setString(1, nomUtilisateur.trim());
            ps.setString(2, haserSha256(motDePasse));
            ps.setObject(3, LocalDateTime.now());
            ps.executeUpdate();
            System.out.println("[AUTH] Utilisateur inscrit : " + nomUtilisateur);
            return true;
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur inscription : " + e.getMessage());
            return false;
        }
    }

    /**
     * Tente de connecter un utilisateur.
     * @return l'objet Utilisateur si les identifiants sont corrects, null sinon.
     */
    public Utilisateur connecter(String nomUtilisateur, String motDePasse) {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return null;

        try (PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_LOGIN)) {
            ps.setString(1, nomUtilisateur.trim());
            ps.setString(2, haserSha256(motDePasse));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Utilisateur(
                            rs.getInt("id"),
                            rs.getString("nom_utilisateur"),
                            rs.getObject("date_creation", LocalDateTime.class)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("[DAO] Erreur connexion : " + e.getMessage());
        }
        return null;
    }

    /** Vérifie si un nom d'utilisateur est déjà pris. */
    public boolean nomExiste(String nomUtilisateur) {
        Connection conn = DatabaseManager.getInstance().getConnection();
        if (conn == null) return false;
        try (PreparedStatement ps = conn.prepareStatement(SQL_EXISTS)) {
            ps.setString(1, nomUtilisateur.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /** Hache un mot de passe en SHA-256 (retourne la représentation hexadécimale). */
    public static String haserSha256(String texte) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(texte.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 indisponible", e);
        }
    }
}
