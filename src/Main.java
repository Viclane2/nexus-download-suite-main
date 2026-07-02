
import ui.MainFrame;

import javax.swing.*;
import java.io.File;

/**
 * Point d'entrée de l'application.
 * Garantit que le dossier de données existe avant de démarrer l'IHM,
 * et lance la fenêtre principale sur l'Event Dispatch Thread (EDT),
 * comme l'exige toute application Swing correctement construite.
 */
public class Main {
    public static void main(String[] args) {
        new File("data").mkdirs();
        SwingUtilities.invokeLater(() -> new ui.SplashScreen(() -> SwingUtilities.invokeLater(MainFrame::new)));
    }
}
