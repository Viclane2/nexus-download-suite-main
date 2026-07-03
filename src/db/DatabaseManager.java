package db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestionnaire de connexion MySQL — Pattern Singleton.
 *
 * Charge les paramètres depuis src/resources/db.properties
 * et fournit une unique connexion partagée à toute l'application.
 *
 * En cas d'échec de connexion, l'application continue de fonctionner
 * en mode dégradé (sans persistance MySQL).
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private String url;
    private String user;
    private String password;

    private DatabaseManager() {
        chargerConfiguration();
    }

    /** Retourne l'instance unique du gestionnaire. */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Charge les paramètres depuis db.properties.
     */
    private void chargerConfiguration() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("resources/db.properties")) {
            if (is == null) {
                System.err.println("[DB] Fichier db.properties introuvable dans resources/");
                return;
            }
            props.load(is);
            this.url      = props.getProperty("db.url");
            this.user     = props.getProperty("db.user");
            this.password = props.getProperty("db.password", "");
        } catch (IOException e) {
            System.err.println("[DB] Erreur lecture db.properties : " + e.getMessage());
        }
    }

    /**
     * Retourne une connexion valide, en la rouvrant si nécessaire.
     * Retourne null si la connexion est impossible (mode dégradé).
     */
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                if (url == null) {
                    return null;
                }
                connection = DriverManager.getConnection(url, user, password);
                System.out.println("[DB] Connexion MySQL établie : " + url);
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("[DB] Impossible de se connecter à MySQL : " + e.getMessage());
            System.err.println("[DB] Vérifiez que XAMPP (MySQL) est démarré et que db.properties est correct.");
            return null;
        }
    }

    /**
     * Ferme proprement la connexion (à appeler à la fermeture de l'appli).
     */
    public void fermer() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[DB] Connexion MySQL fermée.");
            } catch (SQLException e) {
                System.err.println("[DB] Erreur fermeture connexion : " + e.getMessage());
            }
        }
    }
}