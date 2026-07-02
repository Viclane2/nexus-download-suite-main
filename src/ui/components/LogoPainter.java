package ui.components;

import ui.Theme;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/**
 * Utilitaire de dessin vectoriel pour l'identité visuelle de l'application.
 * Génère le logo cyberpunk (nuage réseau + flèche néon + circuits de données)
 * et permet de générer des icônes d'application à la volée.
 */
public final class LogoPainter {

    /**
     * Dessine le logo vectoriel cyberpunk.
     */
    public static void dessinerLogo(Graphics2D g2, int x, int y, int taille, Color couleurAccent) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Facteur d'échelle basé sur une grille de 64x64
        double echelle = taille / 64.0;
        g2.translate(x, y);
        g2.scale(echelle, echelle);

        // 1. Dessiner les lignes de circuit en arrière-plan
        g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 40));
        g2.setStroke(new BasicStroke(1.0f));
        // Ligne de circuit gauche
        g2.drawLine(4, 32, 14, 32);
        g2.drawLine(14, 32, 20, 20);
        g2.fillOval(18, 18, 4, 4);
        
        // Ligne de circuit droite
        g2.drawLine(60, 32, 50, 32);
        g2.drawLine(50, 32, 44, 44);
        g2.fillOval(42, 42, 4, 4);

        // 2. Dessiner la forme de nuage (Contour externe)
        Path2D nuage = new Path2D.Double();
        nuage.moveTo(18, 42);
        nuage.curveTo(10, 42, 10, 30, 18, 28);
        nuage.curveTo(18, 16, 32, 12, 38, 20);
        nuage.curveTo(46, 16, 54, 24, 52, 32);
        nuage.curveTo(58, 34, 58, 42, 50, 42);
        nuage.closePath();

        // Effet de lueur néon autour du nuage
        Theme.dessinerLueur(g2, nuage, couleurAccent, 4);

        // Remplissage intérieur en verre fumé sombre
        g2.setColor(new Color(11, 16, 27, 210));
        g2.fill(nuage);

        // Dessin de la bordure solide
        g2.setColor(couleurAccent);
        g2.setStroke(new BasicStroke(1.8f));
        g2.draw(nuage);

        // 3. Dessiner la flèche néon descendante
        Path2D fleche = new Path2D.Double();
        // Tige
        fleche.moveTo(32, 20);
        fleche.lineTo(32, 36);
        // Tête gauche
        fleche.moveTo(25, 30);
        fleche.lineTo(32, 37);
        // Tête droite
        fleche.lineTo(39, 30);
        
        // Lueur sur la flèche
        Theme.dessinerLueur(g2, fleche, Theme.ACCENT_CYAN, 3);
        
        // Flèche solide cyan
        g2.setColor(Theme.ACCENT_CYAN);
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(fleche);

        // Rétablir la transformation d'origine
        g2.scale(1.0 / echelle, 1.0 / echelle);
        g2.translate(-x, -y);
    }

    /**
     * Génère une BufferedImage contenant le logo, utilisable comme icône d'application.
     */
    public static BufferedImage genererIconeApplication(int taille) {
        BufferedImage image = new BufferedImage(taille, taille, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fond de l'icône (Cercle sombre)
        g2.setColor(new Color(10, 14, 23));
        g2.fillOval(0, 0, taille, taille);
        
        g2.setColor(Theme.BORDURE_CARTE);
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawOval(0, 0, taille - 1, taille - 1);
        
        // Dessin du logo centré
        int marge = taille / 8;
        int tailleLogo = taille - 2 * marge;
        dessinerLogo(g2, marge, marge, tailleLogo, Theme.ACCENT_MAGENTA);
        
        g2.dispose();
        return image;
    }
}
