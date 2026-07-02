package ui.tabs;

import threading.MoteurTelechargement;
import ui.Theme;
import ui.components.FuturisticButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * Onglet Paramètres.
 * Fournit une interface utilisateur pour modifier le répertoire de destination,
 * limiter la concurrence du moteur de téléchargement et basculer en temps réel
 * entre 3 thèmes de couleurs néon.
 */
public class SettingsTab extends JPanel {

    private final MoteurTelechargement moteur;
    private final JTextField champDossier;
    private final JComboBox<String> comboMax;
    private final JComboBox<String> comboTheme;
    private final JFrame parentFrame;

    public SettingsTab(JFrame parent, MoteurTelechargement moteur, String dossierCourant, 
                       java.util.function.Consumer<String> dossierCallback) {
        this.parentFrame = parent;
        this.moteur = moteur;

        setLayout(new GridBagLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(24, 32, 24, 32));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Section 1: Répertoire par défaut ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel labelDossier = new JLabel("Dossier de destination local :");
        labelDossier.setFont(Theme.POLICE_SECTION);
        labelDossier.setForeground(Theme.TEXTE_PRINCIPAL);
        add(labelDossier, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 2;
        JPanel panelDossierInput = new JPanel(new BorderLayout(8, 0));
        panelDossierInput.setOpaque(false);

        champDossier = new JTextField(dossierCourant) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(15, 23, 42, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Theme.BORDURE_CARTE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        champDossier.setOpaque(false);
        champDossier.setCaretColor(Color.WHITE);
        champDossier.setForeground(Color.WHITE);
        champDossier.setFont(Theme.POLICE_NORMALE);
        champDossier.setBorder(new EmptyBorder(8, 12, 8, 12));
        champDossier.setEditable(false);

        FuturisticButton btnParcourir = new FuturisticButton("Parcourir...", Theme.ACCENT_CYAN);
        btnParcourir.setPreferredSize(new Dimension(120, 36));
        btnParcourir.addActionListener(e -> {
            JFileChooser selecteur = new JFileChooser();
            selecteur.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            selecteur.setSelectedFile(new File(champDossier.getText()));
            int retour = selecteur.showOpenDialog(parentFrame);
            if (retour == JFileChooser.APPROVE_OPTION) {
                String chemin = selecteur.getSelectedFile().getAbsolutePath();
                champDossier.setText(chemin);
                dossierCallback.accept(chemin);
            }
        });

        panelDossierInput.add(champDossier, BorderLayout.CENTER);
        panelDossierInput.add(btnParcourir, BorderLayout.EAST);
        add(panelDossierInput, gbc);

        // --- Section 2: Concurrence ---
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        JLabel labelMax = new JLabel("Nombre max de tâches en parallèle :");
        labelMax.setFont(Theme.POLICE_SECTION);
        labelMax.setForeground(Theme.TEXTE_PRINCIPAL);
        add(labelMax, gbc);

        gbc.gridy = 3;
        String[] optionsMax = {"1", "2", "3", "5", "10", "Illimité"};
        comboMax = new JComboBox<>(optionsMax);
        comboMax.setFont(Theme.POLICE_NORMALE);
        comboMax.setBackground(Theme.FOND_CARTE);
        comboMax.setForeground(Color.WHITE);
        
        int limiteCourante = moteur.getMaxTelechargementsSimultanes();
        if (limiteCourante == 1) comboMax.setSelectedIndex(0);
        else if (limiteCourante == 2) comboMax.setSelectedIndex(1);
        else if (limiteCourante == 3) comboMax.setSelectedIndex(2);
        else if (limiteCourante == 5) comboMax.setSelectedIndex(3);
        else if (limiteCourante == 10) comboMax.setSelectedIndex(4);
        else comboMax.setSelectedIndex(5);

        comboMax.addActionListener(e -> {
            String selection = (String) comboMax.getSelectedItem();
            int limite = 3;
            if ("1".equals(selection)) limite = 1;
            else if ("2".equals(selection)) limite = 2;
            else if ("3".equals(selection)) limite = 3;
            else if ("5".equals(selection)) limite = 5;
            else if ("10".equals(selection)) limite = 10;
            else if ("Illimité".equals(selection)) limite = Integer.MAX_VALUE;
            moteur.setMaxTelechargementsSimultanes(limite);
        });
        add(comboMax, gbc);

        // --- Section 3: Limiteur de vitesse (Style IDM) ---
        gbc.gridy = 4;
        JLabel labelLimiteur = new JLabel("Limiteur de vitesse global (Style IDM) :");
        labelLimiteur.setFont(Theme.POLICE_SECTION);
        labelLimiteur.setForeground(Theme.TEXTE_PRINCIPAL);
        add(labelLimiteur, gbc);

        gbc.gridy = 5;
        JPanel panelLimiteur = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelLimiteur.setOpaque(false);

        JCheckBox checkLimiter = new JCheckBox("Activer la limitation");
        checkLimiter.setFont(Theme.POLICE_NORMALE);
        checkLimiter.setForeground(Color.WHITE);
        checkLimiter.setOpaque(false);
        checkLimiter.setSelected(moteur.isLimiteurVitesseActif());

        String[] optionsVitesse = {"100 Ko/s", "250 Ko/s", "500 Ko/s", "1 Mo/s", "2 Mo/s", "5 Mo/s"};
        JComboBox<String> comboVitesse = new JComboBox<>(optionsVitesse);
        comboVitesse.setFont(Theme.POLICE_NORMALE);
        comboVitesse.setBackground(Theme.FOND_CARTE);
        comboVitesse.setForeground(Color.WHITE);
        
        int limiteCouranteKo = moteur.getLimiteVitesseKoS();
        if (limiteCouranteKo == 100) comboVitesse.setSelectedIndex(0);
        else if (limiteCouranteKo == 250) comboVitesse.setSelectedIndex(1);
        else if (limiteCouranteKo == 500) comboVitesse.setSelectedIndex(2);
        else if (limiteCouranteKo == 1024) comboVitesse.setSelectedIndex(3);
        else if (limiteCouranteKo == 2048) comboVitesse.setSelectedIndex(4);
        else if (limiteCouranteKo == 5120) comboVitesse.setSelectedIndex(5);
        else comboVitesse.setSelectedIndex(2);

        checkLimiter.addActionListener(e -> moteur.setLimiteurVitesseActif(checkLimiter.isSelected()));
        comboVitesse.addActionListener(e -> {
            String selection = (String) comboVitesse.getSelectedItem();
            int limite = 500;
            if ("100 Ko/s".equals(selection)) limite = 100;
            else if ("250 Ko/s".equals(selection)) limite = 250;
            else if ("500 Ko/s".equals(selection)) limite = 500;
            else if ("1 Mo/s".equals(selection)) limite = 1024;
            else if ("2 Mo/s".equals(selection)) limite = 2048;
            else if ("5 Mo/s".equals(selection)) limite = 5120;
            moteur.setLimiteVitesseKoS(limite);
        });

        panelLimiteur.add(checkLimiter);
        panelLimiteur.add(comboVitesse);
        add(panelLimiteur, gbc);

        // --- Section 4: Thème néon dynamique ---
        gbc.gridy = 6;
        JLabel labelTheme = new JLabel("Palette graphique de l'IHM :");
        labelTheme.setFont(Theme.POLICE_SECTION);
        labelTheme.setForeground(Theme.TEXTE_PRINCIPAL);
        add(labelTheme, gbc);

        gbc.gridy = 7;
        String[] optionsThemes = {"Cyberpunk Cyan", "Matrix Green", "Vaporwave Pink"};
        comboTheme = new JComboBox<>(optionsThemes);
        comboTheme.setFont(Theme.POLICE_NORMALE);
        comboTheme.setBackground(Theme.FOND_CARTE);
        comboTheme.setForeground(Color.WHITE);
        comboTheme.addActionListener(e -> {
            String selectionTheme = (String) comboTheme.getSelectedItem();
            if ("Cyberpunk Cyan".equals(selectionTheme)) {
                Theme.ACCENT_CYAN = new Color(0, 240, 255);
                Theme.ACCENT_MAGENTA = new Color(214, 0, 255);
            } else if ("Matrix Green".equals(selectionTheme)) {
                Theme.ACCENT_CYAN = new Color(0, 255, 51);
                Theme.ACCENT_MAGENTA = new Color(16, 185, 129);
            } else if ("Vaporwave Pink".equals(selectionTheme)) {
                Theme.ACCENT_CYAN = new Color(255, 0, 160);
                Theme.ACCENT_MAGENTA = new Color(139, 92, 246);
            }
            parentFrame.repaint();
        });
        add(comboTheme, gbc);

        // Espacement vertical
        gbc.gridy = 8;
        gbc.weighty = 1.0;
        add(Box.createGlue(), gbc);
    }
}
