package ui;

import ui.components.RealTimeChartPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panneau de statistiques globales, affiché en pied de fenêtre.
 * Présente les compteurs d'état sous forme de blocs de verre néon et intègre
 * le composant graphique de bande passante réseau en temps réel.
 */
public class StatistiquesPanel extends JPanel {

    private final JLabel valeurEnAttente = creerLabelValeur();
    private final JLabel valeurEnCours = creerLabelValeur();
    private final JLabel valeurTermine = creerLabelValeur();
    private final JLabel valeurErreur = creerLabelValeur();
    private final JLabel valeurAnnule = creerLabelValeur();
    private final JLabel valeurVolume = creerLabelValeur();

    private final RealTimeChartPanel graphTrafic = new RealTimeChartPanel();

    public StatistiquesPanel() {
        setLayout(new BorderLayout(16, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        // Panneau gauche : compteurs statistiques
        JPanel panelStats = new JPanel(new BorderLayout());
        panelStats.setOpaque(false);

        JLabel titre = new JLabel("Tableau de bord global");
        titre.setFont(Theme.POLICE_SECTION);
        titre.setForeground(Theme.TEXTE_PRINCIPAL);
        titre.setBorder(new EmptyBorder(0, 0, 8, 0));
        panelStats.add(titre, BorderLayout.NORTH);

        JPanel grille = new JPanel(new GridLayout(1, 6, 8, 0));
        grille.setOpaque(false);

        grille.add(creerBloc("En attente", valeurEnAttente, Theme.ORANGE_ATTENTE));
        grille.add(creerBloc("En cours", valeurEnCours, Theme.ACCENT_CYAN));
        grille.add(creerBloc("Terminés", valeurTermine, Theme.VERT_SUCCES));
        grille.add(creerBloc("Erreurs", valeurErreur, Theme.ROUGE_ERREUR));
        grille.add(creerBloc("Annulés", valeurAnnule, Theme.GRIS_ANNULE));
        grille.add(creerBloc("Volume (Mo)", valeurVolume, Theme.ACCENT_MAGENTA));

        panelStats.add(grille, BorderLayout.CENTER);

        // Panneau droit : graphique de débit en direct
        JPanel panelGraph = new JPanel(new BorderLayout());
        panelGraph.setOpaque(false);
        panelGraph.setPreferredSize(new Dimension(200, 68));

        JLabel titreGraph = new JLabel("Activité réseau");
        titreGraph.setFont(Theme.POLICE_SECTION);
        titreGraph.setForeground(Theme.TEXTE_PRINCIPAL);
        titreGraph.setBorder(new EmptyBorder(0, 0, 8, 0));
        
        panelGraph.add(titreGraph, BorderLayout.NORTH);
        panelGraph.add(graphTrafic, BorderLayout.CENTER);

        add(panelStats, BorderLayout.CENTER);
        add(panelGraph, BorderLayout.EAST);
    }

    private JLabel creerLabelValeur() {
        JLabel label = new JLabel("0", SwingConstants.CENTER);
        label.setFont(Theme.POLICE_STAT_VALEUR);
        return label;
    }

    private JPanel creerBloc(String libelle, JLabel labelValeur, Color couleur) {
        JPanel bloc = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Fond en verre sombre
                g2.setColor(Theme.FOND_CARTE);
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Ligne supérieure de couleur d'accent néon
                g2.setColor(couleur);
                g2.fillRoundRect(0, 0, w, 3, 4, 4);

                // Bordure fine
                g2.setColor(Theme.BORDURE_CARTE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);

                g2.dispose();
            }
        };
        bloc.setOpaque(false);
        bloc.setLayout(new BoxLayout(bloc, BoxLayout.Y_AXIS));
        bloc.setBorder(new EmptyBorder(6, 6, 6, 6));

        labelValeur.setForeground(couleur);
        labelValeur.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel labelTexte = new JLabel(libelle, SwingConstants.CENTER);
        labelTexte.setFont(Theme.POLICE_STAT_LABEL);
        labelTexte.setForeground(Theme.TEXTE_SECONDAIRE);
        labelTexte.setAlignmentX(Component.CENTER_ALIGNMENT);

        bloc.add(labelValeur);
        bloc.add(Box.createVerticalStrut(2));
        bloc.add(labelTexte);
        return bloc;
    }

    public void mettreAJour(int enAttente, int enCours, int termine, int erreur, int annule, double volumeMo) {
        valeurEnAttente.setText(String.valueOf(enAttente));
        valeurEnCours.setText(String.valueOf(enCours));
        valeurTermine.setText(String.valueOf(termine));
        valeurErreur.setText(String.valueOf(erreur));
        valeurAnnule.setText(String.valueOf(annule));
        valeurVolume.setText(String.format("%.1f", volumeMo));
    }

    public RealTimeChartPanel getGraphTrafic() {
        return graphTrafic;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Theme.FOND_CARTE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // Bordure supérieure fine
        g2.setColor(Theme.BORDURE_CARTE);
        g2.drawLine(0, 0, getWidth(), 0);
        g2.dispose();
        super.paintComponent(g);
    }
}
