package ui;

import core.StatutTache;
import core.TacheTelechargement;
import ui.components.FuturisticButton;
import ui.components.FuturisticProgressBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Représente une "carte" dans l'IHM associée à une tâche de téléchargement.
 * Redessinée dans un style cyberpunk avec indicateur d'état latéral, icône de fichier
 * dynamique selon l'extension, FuturisticProgressBar animée et FuturisticButton.
 */
public class LigneTachePanel extends JPanel {

    private final JLabel labelNom;
    private final JLabel labelTaille;
    private final FuturisticProgressBar barreProgression;
    private final JLabel badgeStatut;
    private final FuturisticButton boutonAnnuler;
    private final TacheTelechargement tache;

    public LigneTachePanel(TacheTelechargement tache, Runnable actionAnnuler) {
        this.tache = tache;

        setLayout(new BorderLayout(14, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        // ----- Colonne gauche : Icône fichier + Nom + Métriques -----
        JPanel panelGauche = new JPanel(new BorderLayout(10, 0));
        panelGauche.setOpaque(false);
        panelGauche.setPreferredSize(new Dimension(280, 48));

        // Icône de fichier futuriste basée sur l'extension
        JPanel panelIcone = creerPanelIcone(tache.getNomFichier());
        panelGauche.add(panelIcone, BorderLayout.WEST);

        JPanel panelTextes = new JPanel();
        panelTextes.setOpaque(false);
        panelTextes.setLayout(new BoxLayout(panelTextes, BoxLayout.Y_AXIS));
        
        labelNom = new JLabel(tache.getNomFichier());
        labelNom.setFont(Theme.POLICE_SECTION);
        labelNom.setForeground(Theme.TEXTE_PRINCIPAL);
        labelNom.setAlignmentX(LEFT_ALIGNMENT);

        labelTaille = new JLabel(formaterMetriques(tache));
        labelTaille.setFont(Theme.POLICE_PETITE);
        labelTaille.setForeground(Theme.TEXTE_SECONDAIRE);
        labelTaille.setAlignmentX(LEFT_ALIGNMENT);

        panelTextes.add(Box.createVerticalStrut(2));
        panelTextes.add(labelNom);
        panelTextes.add(Box.createVerticalStrut(4));
        panelTextes.add(labelTaille);

        panelGauche.add(panelTextes, BorderLayout.CENTER);

        // ----- Centre : Barre de progression néon -----
        barreProgression = new FuturisticProgressBar();
        barreProgression.setStringPainted(true);
        barreProgression.setFont(Theme.POLICE_PETITE);

        // ----- Droite : Badge OU bouton annuler (selon l'état) -----
        JPanel colonneDroite = new JPanel(new GridBagLayout());
        colonneDroite.setOpaque(false);
        colonneDroite.setPreferredSize(new Dimension(96, 32));

        badgeStatut = new JLabel(tache.getStatut().getLibelle(), SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                Color col = Theme.couleurStatut(tache.getStatut());
                
                // Dessine un conteneur translucide bordé de néon
                g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 30));
                g2.fillRoundRect(0, 0, w, h, 6, 6);
                
                g2.setColor(col);
                g2.setStroke(new BasicStroke(1.0f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, 6, 6);
                
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badgeStatut.setFont(Theme.POLICE_PETITE);
        badgeStatut.setForeground(Color.WHITE);
        badgeStatut.setPreferredSize(new Dimension(86, 22));

        boutonAnnuler = new FuturisticButton("Annuler", Theme.ROUGE_ERREUR);
        boutonAnnuler.setFont(Theme.POLICE_PETITE);
        boutonAnnuler.setPreferredSize(new Dimension(86, 24));
        boutonAnnuler.addActionListener(e -> actionAnnuler.run());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        colonneDroite.add(badgeStatut, gbc);
        colonneDroite.add(boutonAnnuler, gbc);

        add(panelGauche, BorderLayout.WEST);
        add(barreProgression, BorderLayout.CENTER);
        add(colonneDroite, BorderLayout.EAST);

        mettreAJour(tache);
    }

    /**
     * Crée un panneau d'icône vectorielle stylisée selon l'extension du fichier.
     */
    private JPanel creerPanelIcone(String nomFichier) {
        String ext = "";
        int idx = nomFichier.lastIndexOf('.');
        if (idx > 0) {
            ext = nomFichier.substring(idx).toLowerCase();
        }

        String emoji = "🌐";
        Color couleurBorder = Theme.ACCENT_CYAN;

        if (ext.equals(".zip") || ext.equals(".rar") || ext.equals(".tar") || ext.equals(".gz") || ext.equals(".7z")) {
            emoji = "📦";
            couleurBorder = Theme.ACCENT_MAGENTA;
        } else if (ext.equals(".mp3") || ext.equals(".wav") || ext.equals(".ogg") || ext.equals(".flac") || ext.equals(".m4a")) {
            emoji = "🎵";
            couleurBorder = Theme.ORANGE_ATTENTE;
        } else if (ext.equals(".mp4") || ext.equals(".avi") || ext.equals(".mkv") || ext.equals(".mov") || ext.equals(".webm")) {
            emoji = "🎬";
            couleurBorder = Theme.ACCENT_MAGENTA;
        } else if (ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".gif") || ext.equals(".webp")) {
            emoji = "🖼️";
            couleurBorder = Theme.VERT_SUCCES;
        } else if (ext.equals(".pdf") || ext.equals(".txt") || ext.equals(".docx") || ext.equals(".xlsx") || ext.equals(".csv")) {
            emoji = "📄";
            couleurBorder = Theme.TEXTE_PRINCIPAL;
        }

        final String finalEmoji = emoji;
        final Color finalBorder = couleurBorder;

        JPanel panelIcone = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();

                // Fond de l'icône
                g2.setColor(new Color(15, 23, 42, 180));
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);

                // Bordure fine néon
                g2.setColor(finalBorder);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(2, 2, w - 4, h - 4, 8, 8);

                // Émoticône au centre
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                FontMetrics fm = g2.getFontMetrics();
                int ex = (w - fm.stringWidth(finalEmoji)) / 2;
                int ey = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(finalEmoji, ex, ey);

                g2.dispose();
            }
        };
        panelIcone.setPreferredSize(new Dimension(36, 36));
        panelIcone.setMinimumSize(new Dimension(36, 36));
        panelIcone.setMaximumSize(new Dimension(36, 36));
        panelIcone.setOpaque(false);
        return panelIcone;
    }

    /**
     * Formate les métriques détaillées (vitesse, ETA, volume).
     */
    /*private String formaterMetriques(TacheTelechargement t) {
        StatutTache statut = t.getStatut();
        if (statut == StatutTache.EN_COURS) {
            double vitesse = t.getVitesseMoS();
            long eta = t.getEtaSecondes();
            String vitesseStr = vitesse < 0.1 ? String.format("%.0f Ko/s", vitesse * 1024) : String.format("%.1f Mo/s", vitesse);
            String etaStr = eta < 0 ? "calcul..." : (eta >= 60 ? (eta / 60) + "m " + (eta % 60) + "s" : eta + "s");
            return String.format("%s / %s • %s • Restant: %s", 
                    formaterTailleMo(t.getOctetsRecus() / (1024.0 * 1024.0)), 
                    formaterTailleMo(t.getTailleTotaleMo()), 
                    vitesseStr, 
                    etaStr);
        } else if (statut == StatutTache.EN_ATTENTE) {
            return "En attente... • Taille: " + formaterTailleMo(t.getTailleTotaleMo());
        } else if (statut == StatutTache.TERMINE) {
            return "Terminé • Taille: " + formaterTailleMo(t.getTailleTotaleMo());
        } else if (statut == StatutTache.ERREUR) {
            return "Échec • Récupéré: " + formaterTailleMo(t.getOctetsRecus() / (1024.0 * 1024.0)) + " / " + formaterTailleMo(t.getTailleTotaleMo());
        } else {
            return "Annulé • Récupéré: " + formaterTailleMo(t.getOctetsRecus() / (1024.0 * 1024.0)) + " / " + formaterTailleMo(t.getTailleTotaleMo());
        }
    }*/

    private String formaterMetriques(TacheTelechargement t) {
        StatutTache statut = t.getStatut();
        double tailleMo = t.getTailleTotaleMo();
        double progression = t.getProgression();
        
        String tailleStr;
        if (tailleMo < 0) {
            tailleStr = "???";
        } else if (tailleMo < 0.001) {
            tailleStr = "Fichier vide";
        } else if (tailleMo < 1) {
            long tailleKo = Math.round(tailleMo * 1024);
            tailleStr = tailleKo + " Ko";
        } else {
            tailleStr = String.format("%.1f Mo", tailleMo);
        }
        
        String progressStr;
        if (progression < 0) {
            progressStr = "Calcul...";
        } else {
            progressStr = String.format("%.1f%%", progression);
        }
        
        if (statut == StatutTache.EN_COURS) {
            double vitesse = t.getVitesseMoS();
            long eta = t.getEtaSecondes();
            String vitesseStr = vitesse < 0.1 ? String.format("%.0f Ko/s", vitesse * 1024) : String.format("%.1f Mo/s", vitesse);
            String etaStr = eta < 0 ? "calcul..." : (eta >= 60 ? (eta / 60) + "m " + (eta % 60) + "s" : eta + "s");
            return progressStr + " • " + tailleStr + " • " + vitesseStr + " • Restant: " + etaStr;
        } else if (statut == StatutTache.EN_ATTENTE) {
            return "En attente • " + tailleStr;
        } else if (statut == StatutTache.TERMINE) {
            return "✓ Terminé • " + tailleStr;
        } else if (statut == StatutTache.ERREUR) {
            return "✗ Échec • " + tailleStr;
        } else {
            return "⊘ Annulé • " + tailleStr;
        }
    }

    private String formaterTailleMo(double tailleMo) {
        if (tailleMo < 0) {
            return "Inconnue";
        } else if (tailleMo < 1) {
            long tailleKo = Math.round(tailleMo * 1024);
            return tailleKo + " Ko";
        } else {
            return String.format("%.1f Mo", tailleMo);
        }
    }

    /**
     * Permet d'animer le flux d'énergie de la barre de progression et le contour lumineux.
     */
    public void animerBarre() {
        barreProgression.incrementAnimation();
        // Force le rafraîchissement visuel pour animer la lueur pulsante du contour
        if (tache.getStatut() == StatutTache.EN_COURS) {
            repaint();
        }
    }

    /**
     * Met à jour l'affichage de la ligne.
     */
    public void mettreAJour(TacheTelechargement tacheMaj) {
        double progression = tacheMaj.getProgression();
        StatutTache statut = tacheMaj.getStatut();
        
        // Mettre à jour les textes descriptifs
        labelTaille.setText(formaterMetriques(tacheMaj));
        
        // Configurer la couleur et l'état de la barre de progression
        barreProgression.setAccentColor(Theme.couleurStatut(statut));

        if (progression < 0 && (statut == StatutTache.EN_COURS || statut == StatutTache.EN_ATTENTE)) {
            if (!barreProgression.isIndeterminate()) {
                barreProgression.setIndeterminate(true);
            }
            barreProgression.setString("Calcul...");
        } else if (progression >= 0) {
            if (barreProgression.isIndeterminate()) {
                barreProgression.setIndeterminate(false);
            }
            int progressionArrondie = (int) Math.round(progression);
            barreProgression.setValue(Math.min(100, progressionArrondie));
            
            if (statut == StatutTache.TERMINE) {
                barreProgression.setString("100% - Terminé");
            } else if (statut == StatutTache.ERREUR) {
                barreProgression.setString("Échec - " + progressionArrondie + "%");
            } else if (statut == StatutTache.ANNULE) {
                barreProgression.setString("Annulé - " + progressionArrondie + "%");
            } else {
                barreProgression.setString(progressionArrondie + "%");
            }
        }

        // Mettre à jour le badge de statut
        badgeStatut.setText(statut.getLibelle());

        // Afficher l'action d'annulation pendant le téléchargement, et le badge statique à la fin
        boolean enCoursOuAttente = statut == StatutTache.EN_COURS || statut == StatutTache.EN_ATTENTE;
        boutonAnnuler.setVisible(enCoursOuAttente);
        boutonAnnuler.setEnabled(enCoursOuAttente);
        badgeStatut.setVisible(!enCoursOuAttente);
        
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Fond de la carte
        g2.setColor(Theme.FOND_CARTE);
        g2.fillRoundRect(0, 0, w, h, 10, 10);

        // Bordure fine avec effet de pulsation néon pour les téléchargements actifs
        if (tache.getStatut() == StatutTache.EN_COURS) {
            double temps = System.currentTimeMillis() / 250.0;
            float intensite = (float) (0.35 + 0.65 * Math.abs(Math.sin(temps))); // Respiration sinusoïdale
            Color couleurBase = Theme.couleurStatut(StatutTache.EN_COURS);
            Color couleurPulsante = new Color(couleurBase.getRed(), couleurBase.getGreen(), couleurBase.getBlue(), (int) (120 * intensite));
            
            g2.setColor(couleurPulsante);
            g2.setStroke(new BasicStroke(1.2f + 0.8f * intensite));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
            
            // Effet lueur externe
            Theme.dessinerLueur(g2, new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 10, 10), couleurBase, (int) (2 + 3 * intensite));
        } else {
            g2.setColor(Theme.BORDURE_CARTE);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
        }

        // Ligne d'accentuation d'état sur le côté gauche
        Color col = Theme.couleurStatut(tache.getStatut());
        g2.setColor(col);
        g2.fillRoundRect(0, 0, 5, h, 10, 10);
        g2.fillRect(3, 0, 2, h); // Complète la courbure sur l'arête gauche

        g2.dispose();
    }

    public TacheTelechargement getTache() {
        return tache;
    }
}