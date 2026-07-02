package ui.tabs;

import ui.Theme;
import ui.StatistiquesPanel;
import ui.components.FuturisticButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Onglet Principal "Console".
 * Affiche la barre d'ajout rapide de fichiers, la liste de défilement des
 * téléchargements en direct, ainsi que le volet de statistiques et de bande passante.
 */
public class ConsoleTab extends JPanel {

    private final JTextField champUrl;
    private final FuturisticButton btnAjouter;
    private final FuturisticButton boutonAttendre;
    private final FuturisticButton boutonExporter;
    private final FuturisticButton boutonEffacerTerminees;

    private final JScrollPane scrollPane;
    private final StatistiquesPanel statistiquesPanel;

    public ConsoleTab(JTextField champUrlInput, JScrollPane scrollPane, StatistiquesPanel statsPanel,
                      Runnable actionAjouter, Runnable actionAttendre, Runnable actionExporter, Runnable actionNettoyer) {
        
        this.champUrl = champUrlInput;
        this.scrollPane = scrollPane;
        this.statistiquesPanel = statsPanel;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 12, 16));

        // Conteneur supérieur (Outils)
        JPanel conteneurBarre = new JPanel();
        conteneurBarre.setOpaque(false);
        conteneurBarre.setLayout(new BoxLayout(conteneurBarre, BoxLayout.Y_AXIS));

        // Ligne 1 : Ajout d'URL
        JPanel ligneAjout = new JPanel(new BorderLayout(10, 0));
        ligneAjout.setOpaque(false);
        ligneAjout.setBorder(new EmptyBorder(0, 0, 10, 0));

        btnAjouter = new FuturisticButton("Télécharger", Theme.ACCENT_CYAN);
        btnAjouter.setPreferredSize(new Dimension(140, 36));
        btnAjouter.addActionListener(e -> actionAjouter.run());

        ligneAjout.add(champUrl, BorderLayout.CENTER);
        ligneAjout.add(btnAjouter, BorderLayout.EAST);

        // Ligne 2 : Actions secondaires
        JPanel ligneActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ligneActions.setOpaque(false);

        boutonAttendre = new FuturisticButton("Attendre la fin (join)", new Color(112, 128, 144));
        boutonAttendre.setPreferredSize(new Dimension(180, 28));
        boutonAttendre.setFont(Theme.POLICE_PETITE);
        boutonAttendre.addActionListener(e -> actionAttendre.run());

        boutonExporter = new FuturisticButton("Exporter en CSV", new Color(112, 128, 144));
        boutonExporter.setPreferredSize(new Dimension(140, 28));
        boutonExporter.setFont(Theme.POLICE_PETITE);
        boutonExporter.addActionListener(e -> actionExporter.run());

        boutonEffacerTerminees = new FuturisticButton("Nettoyer la liste", new Color(112, 128, 144));
        boutonEffacerTerminees.setPreferredSize(new Dimension(140, 28));
        boutonEffacerTerminees.setFont(Theme.POLICE_PETITE);
        boutonEffacerTerminees.addActionListener(e -> actionNettoyer.run());

        ligneActions.add(boutonAttendre);
        ligneActions.add(boutonExporter);
        ligneActions.add(boutonEffacerTerminees);

        conteneurBarre.add(ligneAjout);
        conteneurBarre.add(ligneActions);

        add(conteneurBarre, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statistiquesPanel, BorderLayout.SOUTH);
    }

    public FuturisticButton getBoutonAttendre() {
        return boutonAttendre;
    }
}
