package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Bandeau d'en-tête de style futuriste cyberpunk.
 * Affiche le titre et le sous-titre de l'application avec un arrière-plan
 * de grille technologique et une ligne de séparation néon brillante.
 */
public class HeaderPanel extends JPanel {

    public HeaderPanel(String titre, String sousTitre) {
        setPreferredSize(new Dimension(100, 72));
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel labelTitre = new JLabel(titre);
        labelTitre.setFont(Theme.POLICE_TITRE);
        labelTitre.setForeground(Color.WHITE);
        labelTitre.setBorder(BorderFactory.createEmptyBorder(12, 20, 0, 20));

        JLabel labelSousTitre = new JLabel(sousTitre);
        labelSousTitre.setFont(Theme.POLICE_SOUS_TITRE);
        labelSousTitre.setForeground(Theme.TEXTE_SECONDAIRE);
        labelSousTitre.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));

        JPanel texte = new JPanel();
        texte.setOpaque(false);
        texte.setLayout(new BoxLayout(texte, BoxLayout.Y_AXIS));
        labelTitre.setAlignmentX(LEFT_ALIGNMENT);
        labelSousTitre.setAlignmentX(LEFT_ALIGNMENT);
        texte.add(labelTitre);
        texte.add(labelSousTitre);

        add(texte, BorderLayout.WEST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();

        // 1. Fond translucide sombre (laisse entrevoir les particules d'arrière-plan)
        g2.setColor(new Color(Theme.FOND_APPLICATION.getRed(), Theme.FOND_APPLICATION.getGreen(), Theme.FOND_APPLICATION.getBlue(), 180));
        g2.fillRect(0, 0, w, h);

        // 2. Grille technologique cyber (lignes fines à faible opacité)
        g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 12));
        g2.setStroke(new BasicStroke(1.0f));
        int tailleGrille = 14;
        for (int x = 0; x < w; x += tailleGrille) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += tailleGrille) {
            g2.drawLine(0, y, w, y);
        }

        // 3. Ligne néon magenta de séparation en bas avec effet de lueur
        Line2D ligneBas = new Line2D.Double(0, h - 2, w, h - 2);
        Theme.dessinerLueur(g2, ligneBas, Theme.ACCENT_MAGENTA, 4);
        
        g2.setColor(Theme.ACCENT_MAGENTA);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(ligneBas);

        g2.dispose();
    }
}

