package ui.components;

import javax.swing.*;
import java.awt.*;

/**
 * Conteneur Swing utilisant CardLayout, qui intercepte les basculements d'écrans
 * pour appliquer une transition animée en fondu d'opacité (Fade-in).
 */
public class FadeContainerPanel extends JPanel {

    private float alpha = 1.0f;
    private Timer timerFondu;
    private final CardLayout cardLayout;

    public FadeContainerPanel(CardLayout cardLayout) {
        this.cardLayout = cardLayout;
        setLayout(cardLayout);
        setOpaque(false);
    }

    /**
     * Change l'onglet actif et démarre la transition de fondu de 0.0 à 1.0.
     */
    public void basculerVersOnglet(String nomOnglet) {
        if (timerFondu != null && timerFondu.isRunning()) {
            timerFondu.stop();
        }

        alpha = 0.0f;
        cardLayout.show(this, nomOnglet);

        timerFondu = new Timer(15, e -> {
            alpha += 0.08f; // Transition d'environ 180ms
            if (alpha >= 1.0f) {
                alpha = 1.0f;
                timerFondu.stop();
            }
            repaint();
        });
        timerFondu.start();
    }

    @Override
    public void paint(Graphics g) {
        if (alpha < 1.0f) {
            Graphics2D g2 = (Graphics2D) g.create();
            // Appliquer l'opacité sur l'ensemble du rendu enfant
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paint(g2);
            g2.dispose();
        } else {
            super.paint(g);
        }
    }
}
