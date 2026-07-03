import db.DatabaseManager;
import ui.LoginFrame;
import ui.MainFrame;
import ui.SplashScreen;

import javax.swing.*;
import java.io.File;

/**
 * Point d'entrée de l'application.
 *
 * Séquence de démarrage :
 *   1. SplashScreen (animation d'intro)
 *   2. LoginFrame   (connexion / inscription)
 *   3. MainFrame    (application principale)
 */
public class Main {
    public static void main(String[] args) {
        new File("data").mkdirs();

        SwingUtilities.invokeLater(() ->
                new SplashScreen(() ->
                        SwingUtilities.invokeLater(() ->
                                // Après le splash, afficher la fenêtre de connexion
                                new LoginFrame(() ->
                                        SwingUtilities.invokeLater(MainFrame::new)
                                )
                        )
                )
        );
    }
}
