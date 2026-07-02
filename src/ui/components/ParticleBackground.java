package ui.components;

import ui.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Arrière-plan dynamique cyberpunk affichant une dérive de poussières de données lumineuses (particules).
 * La vitesse des particules s'ajuste dynamiquement en fonction du débit réseau global.
 */
public class ParticleBackground extends JPanel {

    private static class Particule {
        float x, y;
        float vx, vy;
        int taille;
        float alpha;
        Color couleur;
    }

    private final List<Particule> particules = new ArrayList<>();
    private final Random rand = new Random();
    private static final int NB_PARTICULES = 40;
    private double multiplicateurVitesse = 1.0;

    public ParticleBackground() {
        setOpaque(false);
        // Initialisation des particules
        for (int i = 0; i < NB_PARTICULES; i++) {
            particules.add(creerParticule(true));
        }
    }

    private Particule creerParticule(boolean positionYAleatoire) {
        Particule p = new Particule();
        p.x = rand.nextFloat() * 1000;
        // Si initialisation, éparpiller sur Y, sinon démarrer tout en bas de l'écran
        p.y = positionYAleatoire ? rand.nextFloat() * 800 : 800;
        p.taille = rand.nextInt(3) + 2; // Taille entre 2 et 4 pixels
        p.vx = (rand.nextFloat() - 0.5f) * 0.2f; // Dérive latérale douce
        p.vy = -(rand.nextFloat() * 0.5f + 0.2f); // Mouvement vers le haut
        p.alpha = rand.nextFloat() * 0.5f + 0.2f; // Opacité variable
        
        // Alternance de couleurs Cyan et Magenta
        p.couleur = rand.nextBoolean() ? Theme.ACCENT_CYAN : Theme.ACCENT_MAGENTA;
        return p;
    }

    /**
     * Calcule et met à jour les coordonnées des particules en fonction de la vitesse globale.
     */
    public void mettreAJour(double vitesseGlobaleMoS) {
        // Vitesse de dérive : 1.0x au repos, augmente jusqu'à 5.0x sous charge réseau
        double vitesseCible = 1.0 + Math.min(4.0, vitesseGlobaleMoS / 2.0);
        // Interpolation pour lisser l'accélération
        multiplicateurVitesse = multiplicateurVitesse * 0.95 + vitesseCible * 0.05;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        for (int i = 0; i < particules.size(); i++) {
            Particule p = particules.get(i);
            
            p.x += p.vx * multiplicateurVitesse;
            p.y += p.vy * multiplicateurVitesse;

            // Recréer la particule en bas si elle sort de l'écran
            if (p.y < 0 || p.x < 0 || p.x > w) {
                particules.set(i, creerParticule(false));
                particules.get(i).x = rand.nextFloat() * w;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Dégradé de fond sombre (espace profond)
        GradientPaint fondDegrade = new GradientPaint(
                0, 0, Theme.FOND_APPLICATION,
                0, h, new Color(6, 8, 14)
        );
        g2.setPaint(fondDegrade);
        g2.fillRect(0, 0, w, h);

        // 2. Grille réseau subreptice
        g2.setColor(new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 6));
        int pasGrille = 40;
        for (int x = 0; x < w; x += pasGrille) {
            g2.drawLine(x, 0, x, h);
        }
        for (int y = 0; y < h; y += pasGrille) {
            g2.drawLine(0, y, w, y);
        }

        // 3. Dessin des particules de données
        for (Particule p : particules) {
            Color col = new Color(p.couleur.getRed(), p.couleur.getGreen(), p.couleur.getBlue(), (int)(255 * p.alpha));
            g2.setColor(col);
            
            // Effet de halo flou radial pour les plus grosses particules
            if (p.taille > 2) {
                float[] fractions = {0.0f, 1.0f};
                Color[] couleurs = {col, new Color(p.couleur.getRed(), p.couleur.getGreen(), p.couleur.getBlue(), 0)};
                Point2D centre = new Point2D.Float(p.x, p.y);
                float rayon = p.taille * 2.2f;
                RadialGradientPaint rgp = new RadialGradientPaint(centre, rayon, fractions, couleurs);
                g2.setPaint(rgp);
                g2.fill(new Ellipse2D.Float(p.x - rayon, p.y - rayon, rayon * 2, rayon * 2));
            } else {
                g2.fill(new Ellipse2D.Float(p.x - p.taille, p.y - p.taille, p.taille * 2, p.taille * 2));
            }
        }

        g2.dispose();
    }
}
