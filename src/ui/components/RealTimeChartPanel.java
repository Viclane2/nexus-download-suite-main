package ui.components;

import ui.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/**
 * Graphique de trafic réseau en temps réel.
 * Enregistre l'historique des vitesses de téléchargement et dessine une vague
 * lumineuse continue (spline Bézier) avec lueur néon et dégradé transparent.
 */
public class RealTimeChartPanel extends JPanel {

    private final List<Double> historiqueVitesse = new ArrayList<>();
    private static final int NB_POINTS_MAX = 30; // Historique sur 30 secondes

    public RealTimeChartPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(160, 60));
        // Initialiser avec des zéros
        for (int i = 0; i < NB_POINTS_MAX; i++) {
            historiqueVitesse.add(0.0);
        }
    }

    /**
     * Ajoute une nouvelle mesure de vitesse globale et redessine la courbe.
     */
    public synchronized void ajouterPoint(double vitesseGlobaleMoS) {
        historiqueVitesse.remove(0);
        historiqueVitesse.add(vitesseGlobaleMoS);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Déterminer la vitesse maximale pour adapter l'échelle verticale (au moins 1.0 Mo/s)
        double vitesseMax = 1.0;
        synchronized (this) {
            for (double v : historiqueVitesse) {
                if (v > vitesseMax) {
                    vitesseMax = v;
                }
            }
        }

        // 1. Dessiner la grille de radar cyberpunk (lignes horizontales)
        g2.setColor(new Color(30, 41, 70, 80));
        int divisions = 3;
        for (int i = 1; i < divisions; i++) {
            int y = (h * i) / divisions;
            g2.drawLine(0, y, w, y);
        }

        int nbPoints = historiqueVitesse.size();
        if (nbPoints < 2) {
            g2.dispose();
            return;
        }

        // Calculer les coordonnées de chaque point
        float ecartX = (float) w / (nbPoints - 1);
        float[] xCoords = new float[nbPoints];
        float[] yCoords = new float[nbPoints];

        synchronized (this) {
            for (int i = 0; i < nbPoints; i++) {
                double vitesse = historiqueVitesse.get(i);
                xCoords[i] = i * ecartX;
                // Inversion Y : vitesse = max -> y = 5, vitesse = 0 -> y = h - 5
                yCoords[i] = (float) (h - 5 - ((vitesse / vitesseMax) * (h - 10)));
            }
        }

        // 2. Construire la spline fluide (Courbes de Bézier)
        GeneralPath cheminCourbe = new GeneralPath();
        cheminCourbe.moveTo(xCoords[0], yCoords[0]);
        for (int i = 1; i < nbPoints; i++) {
            float x1 = xCoords[i - 1];
            float y1 = yCoords[i - 1];
            float x2 = xCoords[i];
            float y2 = yCoords[i];
            float ctrlX1 = x1 + (x2 - x1) / 2.0f;
            float ctrlY1 = y1;
            float ctrlX2 = x1 + (x2 - x1) / 2.0f;
            float ctrlY2 = y2;
            cheminCourbe.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x2, y2);
        }

        // 3. Remplir la zone sous la courbe (Dégradé translucide cyan vers transparent)
        GeneralPath zoneRemplissage = (GeneralPath) cheminCourbe.clone();
        zoneRemplissage.lineTo(w, h);
        zoneRemplissage.lineTo(0, h);
        zoneRemplissage.closePath();

        GradientPaint degradreRempli = new GradientPaint(
                0, 0, new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 50),
                0, h, new Color(Theme.ACCENT_CYAN.getRed(), Theme.ACCENT_CYAN.getGreen(), Theme.ACCENT_CYAN.getBlue(), 0)
        );
        g2.setPaint(degradreRempli);
        g2.fill(zoneRemplissage);

        // 4. Dessiner la lueur néon sous le tracé
        Theme.dessinerLueur(g2, cheminCourbe, Theme.ACCENT_CYAN, 3);

        // 5. Dessiner la ligne pleine de la courbe
        g2.setColor(Theme.ACCENT_CYAN);
        g2.setStroke(new BasicStroke(2.0f));
        g2.draw(cheminCourbe);

        // 6. Afficher l'indicateur d'échelle de vitesse
        g2.setFont(Theme.POLICE_PETITE);
        g2.setColor(Theme.TEXTE_SECONDAIRE);
        String label = String.format("Débit max: %.1f Mo/s", vitesseMax);
        g2.drawString(label, 6, 12);

        g2.dispose();
    }
}
