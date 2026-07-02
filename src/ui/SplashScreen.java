package ui;

import ui.Theme;
import ui.components.LogoPainter;
import ui.components.FuturisticProgressBar;

import javax.swing.*;
import java.awt.*;

/**
 * Écran de démarrage (Splash Screen) sans bordure.
 * Affiche le logo vectoriel, une barre de chargement animée et les étapes
 * d'initialisation de l'application avant d'afficher la console.
 */
public class SplashScreen extends JWindow {

    private final FuturisticProgressBar barreProgression;
    private final JLabel labelEtape;
    private int progression = 0;
    private Timer timerChargement;

    public SplashScreen(Runnable callbackFin) {
        setSize(480, 320);
        setLocationRelativeTo(null);
        
        // Permet d'avoir des coins arrondis transparents sur la fenêtre
        setBackground(new Color(0, 0, 0, 0));

        JPanel panelContenu = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Remplissage du fond sombre arrondi
                g2.setColor(Theme.FOND_APPLICATION);
                g2.fillRoundRect(0, 0, w, h, 14, 14);

                // Lueur néon sur tout le cadre
                Theme.dessinerLueur(g2, new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 14, 14), Theme.ACCENT_MAGENTA, 4);

                // Trame de grille cyber subreptice
                g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 8));
                int grille = 16;
                for (int x = 0; x < w; x += grille) {
                    g2.drawLine(x, 0, x, h);
                }
                for (int y = 0; y < h; y += grille) {
                    g2.drawLine(0, y, w, y);
                }

                // Dessin du logo vectoriel
                LogoPainter.dessinerLogo(g2, (w - 110) / 2, 40, 110, Theme.ACCENT_MAGENTA);

                // Version textuelle
                g2.setFont(Theme.POLICE_PETITE);
                g2.setColor(Theme.TEXTE_SECONDAIRE);
                g2.drawString("v2.0.0 Build 10", w - 100, h - 16);

                g2.dispose();
            }
        };
        panelContenu.setLayout(null);
        panelContenu.setOpaque(false);

        // Titre de l'application
        JLabel labelTitre = new JLabel("DOWNLOAD MANAGER", SwingConstants.CENTER);
        labelTitre.setFont(new Font("Segoe UI", Font.BOLD, 22));
        labelTitre.setForeground(Color.WHITE);
        labelTitre.setBounds(20, 160, 440, 30);
        panelContenu.add(labelTitre);

        // Libellé de l'étape en cours
        labelEtape = new JLabel("Initialisation...", SwingConstants.CENTER);
        labelEtape.setFont(Theme.POLICE_SOUS_TITRE);
        labelEtape.setForeground(Theme.TEXTE_SECONDAIRE);
        labelEtape.setBounds(20, 195, 440, 20);
        panelContenu.add(labelEtape);

        // Barre de chargement
        barreProgression = new FuturisticProgressBar();
        barreProgression.setBounds(60, 230, 360, 14);
        panelContenu.add(barreProgression);

        setContentPane(panelContenu);

        // Timer de simulation de chargement des modules
        timerChargement = new Timer(25, e -> {
            progression += 1;
            barreProgression.setValue(progression);
            barreProgression.incrementAnimation();

            // Séquence textuelle de chargement
            if (progression < 25) {
                labelEtape.setText("Initialisation des liaisons multithreads...");
            } else if (progression < 55) {
                labelEtape.setText("Chargement des fichiers d'historique de persistance...");
            } else if (progression < 80) {
                labelEtape.setText("Régulation des files d'attente réseau...");
            } else if (progression < 98) {
                labelEtape.setText("Démarrage de la console de contrôle cyberpunk...");
            } else {
                labelEtape.setText("Prêt !");
            }

            if (progression >= 100) {
                timerChargement.stop();
                setVisible(false);
                dispose();
                callbackFin.run(); // Déclenche l'affichage de la console principale
            }
        });
        
        setVisible(true);
        timerChargement.start();
    }
}
