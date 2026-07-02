package ui.components;

import ui.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Barre de progression personnalisée arborant un design futuriste cyberpunk.
 * Gère une animation de pulsation lumineuse qui parcourt la barre de gauche à droite
 * pour symboliser le flux de données en cours de téléchargement.
 */
public class FuturisticProgressBar extends JProgressBar {

    private float animationOffset = 0.0f; // Position de la vague d'animation (de 0.0 à 1.0)
    private Color accentColor = Theme.ACCENT_CYAN;

    public FuturisticProgressBar() {
        super(0, 100);
        setOpaque(false);
        setBorder(null);
    }

    public void setAccentColor(Color color) {
        this.accentColor = color;
        repaint();
    }

    /**
     * Fait progresser l'animation de la pulsation.
     * Cette méthode doit être appelée périodiquement par un Timer (ex. toutes le 30-50ms).
     */
    public void incrementAnimation() {
        animationOffset += 0.02f;
        if (animationOffset > 1.0f) {
            animationOffset = 0.0f;
        }
        // Ne redessiner que si la barre est active (en cours ou indéterminée)
        if (isIndeterminate() || (getValue() > 0 && getValue() < getMaximum())) {
            repaint();
        }
    }

    public void setProgressWithSize(long bytesRecus, long tailleTotal) {
        if (tailleTotal > 0) {
            int percent = (int) ((bytesRecus * 100) / tailleTotal);
            setValue(Math.min(100, percent));
            
            String tailleStr;
            if (tailleTotal < 1024 * 1024) {
                tailleStr = bytesRecus / 1024 + " Ko / " + tailleTotal / 1024 + " Ko";
            } else {
                tailleStr = String.format("%.1f Mo / %.1f Mo", 
                        bytesRecus / (1024.0 * 1024.0), 
                        tailleTotal / (1024.0 * 1024.0));
            }
            setString(percent + "% - " + tailleStr);
        } else {
            setIndeterminate(true);
            setString("Connexion...");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int rx = 6; // Rayon des angles arrondis

        // 1. Fond du rail (Sombre et profond)
        g2.setColor(new Color(11, 16, 27, 240));
        g2.fillRoundRect(0, 0, w, h, rx, rx);
        
        // Bordure du rail
        g2.setColor(new Color(30, 41, 59, 150));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, rx, rx);

        // 2. Remplissage de la progression
        if (isIndeterminate()) {
            // Mode indéterminé : Bloc néon oscillant
            int blockWidth = w / 3;
            int x = (int) (animationOffset * (w + blockWidth)) - blockWidth;
            
            int clipX = Math.max(0, x);
            int clipW = Math.min(w - clipX, blockWidth - (clipX - x));
            
            if (clipW > 0) {
                g2.setClip(new RoundRectangle2D.Double(0, 0, w, h, rx, rx));
                GradientPaint gp = new GradientPaint(
                        clipX, 0, new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 50),
                        clipX + clipW, 0, accentColor
                );
                g2.setPaint(gp);
                g2.fillRoundRect(clipX, 1, clipW, h - 2, rx - 1, rx - 1);
                g2.setClip(null);
            }
        } else {
            // Mode déterminé : Remplissage avec dégradé
            double percent = (double) getValue() / getMaximum();
            int fillW = (int) (w * percent);
            
            if (fillW > 0) {
                g2.setClip(new RoundRectangle2D.Double(0, 0, w, h, rx, rx));
                
                // Dégradé de progression (cyan/bleu vers couleur accent)
                GradientPaint fillGradient = new GradientPaint(
                        0, 0, new Color(16, 32, 60),
                        fillW, 0, accentColor
                );
                g2.setPaint(fillGradient);
                g2.fillRoundRect(0, 1, fillW, h - 2, rx - 1, rx - 1);

                // Effet de flux d'énergie (pulsation blanche translucide) si le téléchargement est en cours
                if (getValue() < getMaximum()) {
                    int waveW = 100;
                    int waveX = (int) (animationOffset * (fillW + waveW)) - waveW;
                    
                    int drawX = Math.max(0, waveX);
                    int drawW = Math.min(fillW - drawX, waveW - (drawX - waveX));
                    
                    if (drawW > 0) {
                        Color trans = new Color(255, 255, 255, 0);
                        Color bright = new Color(255, 255, 255, 80);
                        
                        LinearGradientPaint lgp = new LinearGradientPaint(
                                drawX, 0, drawX + drawW, 0,
                                new float[]{0.0f, 0.5f, 1.0f},
                                new Color[]{trans, bright, trans}
                        );
                        g2.setPaint(lgp);
                        g2.fillRect(drawX, 1, drawW, h - 2);
                    }
                }
                
                g2.setClip(null);
            }
        }

        // 3. Dessin du pourcentage/statut textuel
        if (isStringPainted() && getString() != null) {
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String text = getString();
            int textX = (w - fm.stringWidth(text)) / 2;
            int textY = (h - fm.getHeight()) / 2 + fm.getAscent();
            
            // Ombre portée sous le texte pour assurer la lisibilité
            g2.setColor(new Color(0, 0, 0, 200));
            g2.drawString(text, textX + 1, textY + 1);
            
            // Texte blanc brillant
            g2.setColor(Color.WHITE);
            g2.drawString(text, textX, textY);
        }

        g2.dispose();
    }
}
