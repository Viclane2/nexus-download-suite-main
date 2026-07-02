package ui.components;

import ui.Theme;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Personnalisation esthétique de la barre de défilement (scrollbar) pour l'application.
 * Supprime les boutons fléchés encombrants et dessine un curseur (thumb) arrondi
 * et discret qui s'illumine au survol.
 */
public class ModernScrollBarUI extends BasicScrollBarUI {

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return creerBoutonVide();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return creerBoutonVide();
    }

    private JButton creerBoutonVide() {
        JButton bouton = new JButton();
        bouton.setPreferredSize(new Dimension(0, 0));
        bouton.setMinimumSize(new Dimension(0, 0));
        bouton.setMaximumSize(new Dimension(0, 0));
        return bouton;
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        // Fond de la glissière identique au fond de l'application
        g2.setColor(Theme.FOND_APPLICATION);
        g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        
        // Fine ligne de démarcation
        g2.setColor(new Color(30, 41, 59, 100));
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            g2.drawLine(bounds.x, bounds.y, bounds.x, bounds.y + bounds.height);
        } else {
            g2.drawLine(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y);
        }
        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
        if (bounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean survol = isThumbRollover();
        boolean drag = isDragging;

        Color couleurThumb;
        if (drag) {
            couleurThumb = Theme.ACCENT_MAGENTA;
        } else if (survol) {
            couleurThumb = Theme.ACCENT_CYAN;
        } else {
            couleurThumb = new Color(51, 65, 85, 180); // Gris ardoise discret
        }

        g2.setColor(couleurThumb);

        // Ajuster l'épaisseur et la marge selon l'orientation
        int x, y, w, h;
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            x = bounds.x + 4;
            y = bounds.y + 2;
            w = bounds.width - 8;
            h = bounds.height - 4;
        } else {
            x = bounds.x + 2;
            y = bounds.y + 4;
            w = bounds.width - 4;
            h = bounds.height - 8;
        }

        g2.fillRoundRect(x, y, w, h, 6, 6);
        g2.dispose();
    }
}
