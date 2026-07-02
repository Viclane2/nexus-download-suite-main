package ui.components;

import ui.Theme;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Bouton personnalisé arborant un design futuriste cyberpunk.
 * Comprend des effets de survol animés avec dégradés et lueurs néon.
 */
public class FuturisticButton extends JButton {

    private final Color colorAccent;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private float hoverAlpha = 0.0f; // Pour la transition de survol
    private Timer hoverTimer;

    public FuturisticButton(String text) {
        this(text, Theme.ACCENT_MAGENTA);
    }

    public FuturisticButton(String text, Color colorAccent) {
        super(text);
        this.colorAccent = colorAccent;
        setFont(Theme.POLICE_SECTION);
        setForeground(Theme.TEXTE_PRINCIPAL);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    startHoverAnimation(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                startHoverAnimation(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    isPressed = true;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = false;
                    repaint();
                }
            }
        });
    }

    private void startHoverAnimation(boolean forward) {
        if (hoverTimer != null && hoverTimer.isRunning()) {
            hoverTimer.stop();
        }
        hoverTimer = new Timer(15, e -> {
            if (forward) {
                hoverAlpha += 0.1f;
                if (hoverAlpha >= 1.0f) {
                    hoverAlpha = 1.0f;
                    hoverTimer.stop();
                }
            } else {
                hoverAlpha -= 0.1f;
                if (hoverAlpha <= 0.0f) {
                    hoverAlpha = 0.0f;
                    hoverTimer.stop();
                }
            }
            repaint();
        });
        hoverTimer.start();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            isHovered = false;
            hoverAlpha = 0.0f;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 1. Dessin de la lueur néon au survol
        if (isEnabled() && hoverAlpha > 0) {
            Shape shape = new RoundRectangle2D.Double(3, 3, w - 6, h - 6, 8, 8);
            Color glowColor = new Color(colorAccent.getRed(), colorAccent.getGreen(), colorAccent.getBlue(), (int)(255 * hoverAlpha * 0.4f));
            Theme.dessinerLueur(g2, shape, glowColor, 4);
        }

        // 2. Fond du bouton (Verre sombre)
        Color bg;
        if (!isEnabled()) {
            bg = new Color(20, 30, 45, 100);
        } else if (isPressed) {
            bg = new Color(colorAccent.getRed(), colorAccent.getGreen(), colorAccent.getBlue(), 80);
        } else {
            // Mélange le fond de carte avec l'accent lors du survol
            int r = (int) (Theme.FOND_CARTE.getRed() * (1.0f - hoverAlpha * 0.15f) + colorAccent.getRed() * (hoverAlpha * 0.15f));
            int gr = (int) (Theme.FOND_CARTE.getGreen() * (1.0f - hoverAlpha * 0.15f) + colorAccent.getGreen() * (hoverAlpha * 0.15f));
            int b = (int) (Theme.FOND_CARTE.getBlue() * (1.0f - hoverAlpha * 0.15f) + colorAccent.getBlue() * (hoverAlpha * 0.15f));
            bg = new Color(r, gr, b, 230);
        }
        g2.setColor(bg);
        g2.fillRoundRect(3, 3, w - 6, h - 6, 8, 8);

        // 3. Bordure Néon
        Color borderCol;
        if (!isEnabled()) {
            borderCol = new Color(45, 55, 75, 100);
        } else {
            int r = (int) (Theme.BORDURE_CARTE.getRed() * (1.0f - hoverAlpha) + colorAccent.getRed() * hoverAlpha);
            int gr = (int) (Theme.BORDURE_CARTE.getGreen() * (1.0f - hoverAlpha) + colorAccent.getGreen() * hoverAlpha);
            int b = (int) (Theme.BORDURE_CARTE.getBlue() * (1.0f - hoverAlpha) + colorAccent.getBlue() * hoverAlpha);
            borderCol = new Color(r, gr, b);
        }
        g2.setColor(borderCol);
        g2.setStroke(new BasicStroke(isHovered && isEnabled() ? 1.5f : 1.0f));
        g2.drawRoundRect(3, 3, w - 6, h - 6, 8, 8);

        // 4. Dessin du texte
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int textX = (w - fm.stringWidth(getText())) / 2;
        int textY = (h - fm.getHeight()) / 2 + fm.getAscent();

        if (!isEnabled()) {
            g2.setColor(Theme.TEXTE_SECONDAIRE);
        } else if (isHovered) {
            g2.setColor(Color.WHITE);
        } else {
            g2.setColor(Theme.TEXTE_PRINCIPAL);
        }
        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }
}
