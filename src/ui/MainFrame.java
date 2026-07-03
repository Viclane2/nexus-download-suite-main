package ui;

import core.*;
import db.DatabaseManager;
import persistence.GestionnairePersistance;
import threading.MoteurTelechargement;
import ui.components.FuturisticButton;
import ui.components.ModernScrollBarUI;
import ui.components.LogoPainter;
import ui.components.ParticleBackground;
import ui.components.FadeContainerPanel;
import ui.tabs.ConsoleTab;
import ui.tabs.LibraryTab;
import ui.tabs.SettingsTab;
import ui.tabs.DocTab;
import java.io.IOException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.net.URL;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Fenêtre principale de l'application : Suite de Téléchargement Cyberpunk.
 * Layout Sidebar de niveau entreprise avec gestion par onglets CardLayout.
 * Gère les rafraîchissements dynamiques, le chargement d'historique, la persistance
 * et l'ordonnancement de la file d'attente de téléchargement.
 */
public class MainFrame extends JFrame implements ProgressionListener {

    private static final String FICHIER_HISTORIQUE = "data/historique.ser";
    private String dossierTelechargement = "telechargements";

    private final GestionnaireTaches gestionnaireTaches = new GestionnaireTaches();
    private final MoteurTelechargement moteur = new MoteurTelechargement(gestionnaireTaches);
    private final GestionnairePersistance persistance = new GestionnairePersistance(FICHIER_HISTORIQUE);

    private final JPanel panelListeTaches = new JPanel();
    private final JLabel labelEtatVide = new JLabel(
            "Aucun téléchargement actif. Entrez une URL ci-dessus pour commencer.",
            SwingConstants.CENTER);
    private final StatistiquesPanel statistiquesPanel = new StatistiquesPanel();
    private final Map<String, LigneTachePanel> lignesParId = new HashMap<>();

    private ParticleBackground fondParticules;
    private FadeContainerPanel conteneurOnglets;
    private LibraryTab bibliothèqueTab;
    private SettingsTab settingsTab;
    private ConsoleTab consoleTab;
    private DocTab docTab;

    private Timer timerAnimation;
    private int ticksCompteur = 0;

    public MainFrame() {
        super("NEXUS Download Suite");
        appliquerApparence();

        // Créer le dossier de téléchargement s'il n'existe pas
        creerDossierTelechargement();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(960, 640);
        setMinimumSize(new Dimension(840, 520));
        setLocationRelativeTo(null);

        // Arrière-plan de particules réactives comme conteneur principal
        fondParticules = new ParticleBackground();
        fondParticules.setLayout(new BorderLayout());
        setContentPane(fondParticules);

        construireInterface();
        gestionnaireTaches.initialiserBD(); // Connexion MySQL + chargement historique
        chargerHistorique();
        gererFermeture();

        // Démarrer le rafraîchissement d'animation
        demarrerTimerAnimation();

        setVisible(true);
    }

    private void creerDossierTelechargement() {
        File dossier = new File(dossierTelechargement);
        if (!dossier.exists()) {
            dossier.mkdirs();
        }
    }

    private void appliquerApparence() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private void construireInterface() {
        // Appliquer l'icône de l'application néon générée à la volée
        setIconImage(LogoPainter.genererIconeApplication(64));

        // Bandeau d'en-tête futuriste
        add(new HeaderPanel(
                "NEXUS DOWNLOAD SUITE",
                "ICT308 — Système Multithread Cyberpunk d'Ordonnancement Parallèle — Groupe 10"), BorderLayout.NORTH);

        // Conteneur principal des onglets avec fondu d'opacité
        CardLayout cardLayout = new CardLayout();
        conteneurOnglets = new FadeContainerPanel(cardLayout);

        // -- Préparation des composants de la Console --
        panelListeTaches.setLayout(new BoxLayout(panelListeTaches, BoxLayout.Y_AXIS));
        panelListeTaches.setBackground(new Color(0, 0, 0, 0)); // Transparent pour le fond
        panelListeTaches.setOpaque(false);
        panelListeTaches.setBorder(new EmptyBorder(10, 0, 10, 0));

        labelEtatVide.setFont(Theme.POLICE_NORMALE);
        labelEtatVide.setForeground(Theme.TEXTE_SECONDAIRE);
        labelEtatVide.setBorder(new EmptyBorder(40, 0, 0, 0));
        panelListeTaches.add(labelEtatVide);

        JScrollPane scrollPane = new JScrollPane(panelListeTaches);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(new Color(0,0,0,0));
        scrollPane.getViewport().setBackground(new Color(0,0,0,0));
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());

        JTextField champUrl = creerChampUrl();

        // 1. Initialiser l'onglet Console
        consoleTab = new ConsoleTab(
                champUrl,
                scrollPane,
                statistiquesPanel,
                () -> lancerTelechargementRapide(champUrl),
                this::attendreToutesLesTachesEnArrierePlan,
                this::exporterRapport,
                this::effacerLignesTerminees
        );

        // Associer la validation par appui sur Entrée dans le champ URL au bouton de lancement
        champUrl.addActionListener(e -> lancerTelechargementRapide(champUrl));

        // 2. Initialiser l'onglet Bibliothèque (Tableau historique de l'équipe 2)
        bibliothèqueTab = new LibraryTab(gestionnaireTaches, this::ajouterEtLancerTache);

        // 3. Initialiser l'onglet Paramètres (Dossier, file d'attente, thèmes)
        settingsTab = new SettingsTab(this, moteur, dossierTelechargement, path -> {
            dossierTelechargement = path;
            creerDossierTelechargement();
        });

        // 4. Initialiser l'onglet de Documentation
        docTab = new DocTab();

        conteneurOnglets.add(consoleTab, "console");
        conteneurOnglets.add(bibliothèqueTab, "library");
        conteneurOnglets.add(settingsTab, "settings");
        conteneurOnglets.add(docTab, "doc");

        // Construire la barre latérale gauche (Sidebar)
        JPanel sidebar = construireSidebar(cardLayout, conteneurOnglets);

        add(sidebar, BorderLayout.WEST);
        add(conteneurOnglets, BorderLayout.CENTER);

        mettreAJourRecap();
    }

    private JTextField creerChampUrl() {
        JTextField champUrl = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                // Fond sombre
                g2.setColor(new Color(15, 23, 42, 220));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

                // Bordure néon si focused
                if (isFocusOwner()) {
                    g2.setColor(Theme.ACCENT_CYAN);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                    Theme.dessinerLueur(g2, new java.awt.geom.RoundRectangle2D.Double(0, 0, w, h, 8, 8), Theme.ACCENT_CYAN, 3);
                } else {
                    g2.setColor(Theme.BORDURE_CARTE);
                    g2.setStroke(new BasicStroke(1.0f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 8, 8);
                }

                g2.dispose();
                super.paintComponent(g);

                // Placeholder
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2d.setFont(Theme.POLICE_NORMALE);
                    g2d.setColor(Theme.TEXTE_SECONDAIRE);
                    g2d.drawString("Coller l'URL du fichier à télécharger ici...", 12, getHeight() / 2 + 5);
                    g2d.dispose();
                }
            }
        };
        champUrl.setOpaque(false);
        champUrl.setCaretColor(Color.WHITE);
        champUrl.setForeground(Color.WHITE);
        champUrl.setFont(Theme.POLICE_NORMALE);
        champUrl.setBorder(new EmptyBorder(8, 12, 8, 12));
        return champUrl;
    }

    /*private void lancerTelechargementRapide(JTextField champUrl) {
        String urlStr = champUrl.getText().trim();
        if (!urlStr.isEmpty()) {
            try {
                new URI(urlStr).toURL();
                String nomFichier = extraireNomFichier(urlStr);
                ajouterEtLancerTache(nomFichier, urlStr);
                champUrl.setText("");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "L'URL saisie est invalide.\nVeuillez saisir une URL HTTP ou HTTPS correcte.",
                        "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "L'URL ne peut pas être vide.",
                    "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
        }
    }*/

    private void lancerTelechargementRapide(JTextField champUrl) {
        String urlStr = champUrl.getText().trim();
        if (urlStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez entrer une URL valide.",
                    "Erreur de saisie", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Vérifier que l'URL commence par http:// ou https://
        if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
            urlStr = "https://" + urlStr;
        }

        try {
            // Valider l'URL
            java.net.URL url = new java.net.URL(urlStr);

            // Extraire le nom du fichier
            String nomFichier = extraireNomFichier(urlStr);

            // Ajouter et lancer la tâche
            ajouterEtLancerTache(nomFichier, urlStr);
            champUrl.setText("");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "L'URL saisie est invalide.\n" +
                            "Assurez-vous qu'elle commence par http:// ou https://\n" +
                            "Exemple: https://example.com/fichier.zip",
                    "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ouvrirFichier(TacheTelechargement tache) {
        File fichier = new File(dossierTelechargement, tache.getNomFichier());
        if (fichier.exists()) {
            try {
                Desktop.getDesktop().open(fichier);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Impossible d'ouvrir le fichier : " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel construireSidebar(CardLayout cardLayout, FadeContainerPanel conteneurOnglets) {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // Fond translucide
                g2.setColor(new Color(6, 9, 15, 220));
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Fine bordure verticale à droite
                g2.setColor(Theme.BORDURE_CARTE);
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
                g2.dispose();
            }
        };
        sidebar.setPreferredSize(new Dimension(170, 600));
        sidebar.setOpaque(false);
        sidebar.setLayout(new BorderLayout());

        // Logo Header de la Sidebar
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                LogoPainter.dessinerLogo(g2, 14, 12, 32, Theme.ACCENT_MAGENTA);
                g2.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(170, 56));
        headerPanel.setLayout(null);

        JLabel labelName = new JLabel("NEXUS DL");
        labelName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        labelName.setForeground(Color.WHITE);
        labelName.setBounds(54, 13, 100, 30);
        headerPanel.add(labelName);
        sidebar.add(headerPanel, BorderLayout.NORTH);

        // Menu Central
        JPanel menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(new EmptyBorder(24, 8, 24, 8));

        FuturisticButton btnConsole = new FuturisticButton("Console", Theme.ACCENT_CYAN);
        FuturisticButton btnLibrary = new FuturisticButton("Bibliothèque", Theme.ACCENT_MAGENTA);
        FuturisticButton btnSettings = new FuturisticButton("Paramètres", Theme.ACCENT_CYAN);
        FuturisticButton btnDoc = new FuturisticButton("Documentation", Theme.ACCENT_MAGENTA);

        Dimension btnSize = new Dimension(154, 34);
        btnConsole.setMaximumSize(btnSize);
        btnLibrary.setMaximumSize(btnSize);
        btnSettings.setMaximumSize(btnSize);
        btnDoc.setMaximumSize(btnSize);

        // Style initial (Console active par défaut, donc en gras)
        btnConsole.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLibrary.setFont(Theme.POLICE_SECTION);
        btnSettings.setFont(Theme.POLICE_SECTION);
        btnDoc.setFont(Theme.POLICE_SECTION);

        menuPanel.add(btnConsole);
        menuPanel.add(Box.createVerticalStrut(10));
        menuPanel.add(btnLibrary);
        menuPanel.add(Box.createVerticalStrut(10));
        menuPanel.add(btnSettings);
        menuPanel.add(Box.createVerticalStrut(10));
        menuPanel.add(btnDoc);
        sidebar.add(menuPanel, BorderLayout.CENTER);

        // Pied de Sidebar
        JPanel footerPanel = new JPanel(new GridLayout(2, 1));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(10, 8, 16, 8));

        JLabel labelInfo1 = new JLabel("Projet 10 • Groupe 10", SwingConstants.CENTER);
        labelInfo1.setFont(Theme.POLICE_PETITE);
        labelInfo1.setForeground(Theme.TEXTE_SECONDAIRE);

        JLabel labelInfo2 = new JLabel("U-Yaoundé I", SwingConstants.CENTER);
        labelInfo2.setFont(Theme.POLICE_PETITE);
        labelInfo2.setForeground(Theme.TEXTE_SECONDAIRE);

        footerPanel.add(labelInfo1);
        footerPanel.add(labelInfo2);
        sidebar.add(footerPanel, BorderLayout.SOUTH);

        // Listeners de permutation d'écrans avec fondu d'opacité
        btnConsole.addActionListener(e -> {
            conteneurOnglets.basculerVersOnglet("console");
            btnConsole.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnLibrary.setFont(Theme.POLICE_SECTION);
            btnSettings.setFont(Theme.POLICE_SECTION);
            btnDoc.setFont(Theme.POLICE_SECTION);
        });

        btnLibrary.addActionListener(e -> {
            conteneurOnglets.basculerVersOnglet("library");
            btnConsole.setFont(Theme.POLICE_SECTION);
            btnLibrary.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnSettings.setFont(Theme.POLICE_SECTION);
            btnDoc.setFont(Theme.POLICE_SECTION);
            if (bibliothèqueTab != null) {
                bibliothèqueTab.rafraichir();
            }
        });

        btnSettings.addActionListener(e -> {
            conteneurOnglets.basculerVersOnglet("settings");
            btnConsole.setFont(Theme.POLICE_SECTION);
            btnLibrary.setFont(Theme.POLICE_SECTION);
            btnSettings.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnDoc.setFont(Theme.POLICE_SECTION);
        });

        btnDoc.addActionListener(e -> {
            conteneurOnglets.basculerVersOnglet("doc");
            btnConsole.setFont(Theme.POLICE_SECTION);
            btnLibrary.setFont(Theme.POLICE_SECTION);
            btnSettings.setFont(Theme.POLICE_SECTION);
            btnDoc.setFont(new Font("Segoe UI", Font.BOLD, 13));
        });

        return sidebar;
    }

    private void demarrerTimerAnimation() {
        // Ticks à 30 FPS
        timerAnimation = new Timer(33, e -> {
            // Animer les pulsations d'énergie sur toutes les cartes de tâches
            for (LigneTachePanel ligne : lignesParId.values()) {
                ligne.animerBarre();
            }

            // Calculer et envoyer le trafic global toutes les secondes
            ticksCompteur++;
            double vitesseTotale = 0.0;
            for (TacheTelechargement t : gestionnaireTaches.lister()) {
                if (t.getStatut() == StatutTache.EN_COURS) {
                    vitesseTotale += t.getVitesseMoS();
                }
            }

            // Animer le déplacement des poussières d'étoiles en arrière-plan
            if (fondParticules != null) {
                fondParticules.mettreAJour(vitesseTotale);
            }

            if (ticksCompteur >= 30) {
                ticksCompteur = 0;
                statistiquesPanel.getGraphTrafic().ajouterPoint(vitesseTotale);

                // Ordonnancer périodiquement au cas où des éléments en file d'attente attendent
                moteur.ordonnancer();
            }
        });
        timerAnimation.start();
    }

    /*private String extraireNomFichier(String url) {
        try {
            String path = url.split("\\?")[0];
            String[] segments = path.split("/");
            String dernierSegment = segments[segments.length - 1];

            if (dernierSegment.isEmpty() || !dernierSegment.contains(".")) {
                return "telechargement_" + System.currentTimeMillis();
            }

            return java.net.URLDecoder.decode(dernierSegment, "UTF-8");
        } catch (Exception e) {
            return "telechargement_" + System.currentTimeMillis();
        }
    }*/

    private String extraireNomFichier(String url) {
        try {
            // Nettoyer l'URL des paramètres
            String cleanUrl = url.split("\\?")[0];
            String[] segments = cleanUrl.split("/");
            String dernierSegment = segments[segments.length - 1];

            // Si le dernier segment est vide ou n'a pas d'extension
            if (dernierSegment == null || dernierSegment.isEmpty() || !dernierSegment.contains(".")) {
                // Générer un nom basé sur le domaine + timestamp
                try {
                    java.net.URL urlObj = new java.net.URL(url);
                    String host = urlObj.getHost();
                    String nomBase = host.replaceAll("^www\\.", "").replaceAll("\\..*$", "");
                    return nomBase + "_" + System.currentTimeMillis() + ".file";
                } catch (Exception e) {
                    return "telechargement_" + System.currentTimeMillis() + ".file";
                }
            }

            // Décoder les caractères encodés (ex: %20 -> espace)
            return java.net.URLDecoder.decode(dernierSegment, "UTF-8");
        } catch (Exception e) {
            return "telechargement_" + System.currentTimeMillis();
        }
    }

    public void ajouterEtLancerTache(String nom, String url) {
        if (labelEtatVide.getParent() == panelListeTaches) {
            panelListeTaches.remove(labelEtatVide);
        }

        TacheTelechargement tache = new TacheTelechargement(nom, url, dossierTelechargement);
        tache.setListener(this);
        tache.setGestionnaire(gestionnaireTaches);
        tache.setMoteur(moteur);

        LigneTachePanel ligne = new LigneTachePanel(tache, () -> tache.annuler());
        lignesParId.put(tache.getId(), ligne);

        panelListeTaches.add(ligne);
        panelListeTaches.add(Box.createVerticalStrut(8));
        panelListeTaches.revalidate();
        panelListeTaches.repaint();

        moteur.demarrerTache(tache);
        mettreAJourRecap();

        if (bibliothèqueTab != null) {
            bibliothèqueTab.rafraichir();
        }
    }

    private void attendreToutesLesTachesEnArrierePlan() {
        if (moteur.nombreThreadsActifs() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Aucun téléchargement actif en cours.",
                    "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        consoleTab.getBoutonAttendre().setEnabled(false);
        consoleTab.getBoutonAttendre().setText("Attente...");

        Thread threadAttente = new Thread(() -> {
            moteur.attendreToutesLesTaches();
            SwingUtilities.invokeLater(() -> {
                consoleTab.getBoutonAttendre().setEnabled(true);
                consoleTab.getBoutonAttendre().setText("Attendre la fin (join)");
                JOptionPane.showMessageDialog(this,
                        "Tous les téléchargements actifs sont terminés !",
                        "Téléchargements terminés", JOptionPane.INFORMATION_MESSAGE);
            });
        }, "Thread-Attente-Globale");
        threadAttente.setDaemon(true);
        threadAttente.start();
    }

    private void exporterRapport() {
        JFileChooser selecteur = new JFileChooser();
        selecteur.setDialogTitle("Exporter le rapport des téléchargements");

        // Ajout des filtres de fichiers
        var htmlFilter = new javax.swing.filechooser.FileNameExtensionFilter("Rapport Web HTML (*.html)", "html");
        var pdfFilter = new javax.swing.filechooser.FileNameExtensionFilter("Document PDF (*.pdf)", "pdf");
        var csvFilter = new javax.swing.filechooser.FileNameExtensionFilter("Fichier CSV (*.csv)", "csv");

        selecteur.addChoosableFileFilter(htmlFilter);
        selecteur.addChoosableFileFilter(pdfFilter);
        selecteur.addChoosableFileFilter(csvFilter);
        selecteur.setFileFilter(htmlFilter); // Par défaut HTML

        int choix = selecteur.showSaveDialog(this);
        if (choix == JFileChooser.APPROVE_OPTION) {
            File dest = selecteur.getSelectedFile();
            String path = dest.getAbsolutePath();
            var filtreActuel = selecteur.getFileFilter();
            boolean succes = false;

            if (filtreActuel == htmlFilter || path.endsWith(".html")) {
                if (!path.endsWith(".html")) path += ".html";
                succes = persistance.exporterHtml(gestionnaireTaches.lister(), path);
            } else if (filtreActuel == pdfFilter || path.endsWith(".pdf")) {
                if (!path.endsWith(".pdf")) path += ".pdf";
                succes = persistance.exporterPdf(gestionnaireTaches.lister(), path);
            } else {
                if (!path.endsWith(".csv")) path += ".csv";
                succes = persistance.exporterCsv(gestionnaireTaches.lister(), path);
            }

            if (succes) {
                final String cheminFinal = path;
                int opt = JOptionPane.showConfirmDialog(this,
                        "Rapport exporté avec succès !\nVoulez-vous l'ouvrir maintenant ?",
                        "Export réussi", JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION) {
                    try {
                        Desktop.getDesktop().open(new File(cheminFinal));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Impossible d'ouvrir le fichier.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Une erreur est survenue lors de l'export.",
                        "Erreur d'export", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void effacerLignesTerminees() {
        for (TacheTelechargement t : gestionnaireTaches.lister()) {
            if (t.getStatut() == StatutTache.TERMINE
                    || t.getStatut() == StatutTache.ERREUR
                    || t.getStatut() == StatutTache.ANNULE) {

                LigneTachePanel ligne = lignesParId.remove(t.getId());
                if (ligne != null) {
                    panelListeTaches.remove(ligne);
                }
                gestionnaireTaches.retirer(t);
            }
        }
        if (gestionnaireTaches.lister().isEmpty()) {
            panelListeTaches.add(labelEtatVide);
        }
        panelListeTaches.revalidate();
        panelListeTaches.repaint();
        mettreAJourRecap();

        if (bibliothèqueTab != null) {
            bibliothèqueTab.rafraichir();
        }
    }

    private void chargerHistorique() {
        var historique = persistance.charger();
        if (!historique.isEmpty()) {
            if (labelEtatVide.getParent() == panelListeTaches) {
                panelListeTaches.remove(labelEtatVide);
            }

            for (TacheTelechargement t : historique) {
                t.setListener(this);
                t.setGestionnaire(gestionnaireTaches);
                t.setMoteur(moteur);

                if (t.getStatut() == StatutTache.EN_COURS || t.getStatut() == StatutTache.EN_ATTENTE) {
                    try {
                        java.lang.reflect.Field field = TacheTelechargement.class.getDeclaredField("statut");
                        field.setAccessible(true);
                        field.set(t, StatutTache.ANNULE);
                    } catch (Exception ignored) {
                    }
                }

                gestionnaireTaches.ajouter(t);

                LigneTachePanel ligne = new LigneTachePanel(t, () -> t.annuler());
                lignesParId.put(t.getId(), ligne);

                panelListeTaches.add(ligne);
                panelListeTaches.add(Box.createVerticalStrut(8));
            }
            panelListeTaches.revalidate();
            panelListeTaches.repaint();
            mettreAJourRecap();

            if (bibliothèqueTab != null) {
                bibliothèqueTab.rafraichir();
            }
        }
    }

    private void gererFermeture() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (timerAnimation != null) {
                    timerAnimation.stop();
                }
                moteur.annulerToutesLesTaches();
                persistance.sauvegarder(gestionnaireTaches.lister());
                DatabaseManager.getInstance().fermer(); // Fermer la connexion MySQL
                dispose();
                System.exit(0);
            }
        });
    }

    private void mettreAJourRecap() {
        int enAttente = gestionnaireTaches.compterParStatut(StatutTache.EN_ATTENTE);
        int enCours = gestionnaireTaches.compterParStatut(StatutTache.EN_COURS);
        int termine = gestionnaireTaches.compterParStatut(StatutTache.TERMINE);
        int erreur = gestionnaireTaches.compterParStatut(StatutTache.ERREUR);
        int annule = gestionnaireTaches.compterParStatut(StatutTache.ANNULE);
        double volume = gestionnaireTaches.getVolumeTotalTelechargeMo();

        statistiquesPanel.mettreAJour(enAttente, enCours, termine, erreur, annule, volume);
    }

    // ---------------------------------------------------------------
    // Implémentation de ProgressionListener
    // ---------------------------------------------------------------

    @Override
    public void onProgressionMiseAJour(TacheTelechargement tache) {
        SwingUtilities.invokeLater(() -> {
            LigneTachePanel ligne = lignesParId.get(tache.getId());
            if (ligne != null) {
                ligne.mettreAJour(tache);
            }
            mettreAJourRecap();
        });
    }

    @Override
    public void onTacheTerminee(TacheTelechargement tache) {
        SwingUtilities.invokeLater(() -> {
            LigneTachePanel ligne = lignesParId.get(tache.getId());
            if (ligne != null) {
                ligne.mettreAJour(tache);
            }
            mettreAJourRecap();

            if (bibliothèqueTab != null) {
                bibliothèqueTab.rafraichir();
            }

            // Ordonnancer pour lancer le téléchargement en attente suivant
            moteur.ordonnancer();

            // ── Persistance MySQL : mettre à jour le statut final en base ──
            gestionnaireTaches.sauvegarderEnBD(tache);

            if (tache.getStatut() == StatutTache.ERREUR) {
                JOptionPane.showMessageDialog(this,
                        "Le téléchargement de \"" + tache.getNomFichier() + "\" a échoué.\nVérifiez l'URL ou votre connexion.",
                        "Échec de téléchargement", JOptionPane.WARNING_MESSAGE);
            } else if (tache.getStatut() == StatutTache.TERMINE) {
                JOptionPane.showMessageDialog(this,
                        "Téléchargement terminé !\nFichier sauvegardé dans : " + dossierTelechargement,
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
            }
            int choix = JOptionPane.showConfirmDialog(this,
                    "Téléchargement terminé !\nFichier sauvegardé dans : " + dossierTelechargement +
                            "\n\nVoulez-vous ouvrir le fichier ?",
                    "Succès", JOptionPane.YES_NO_OPTION);
            if (choix == JOptionPane.YES_OPTION) {
                ouvrirFichier(tache);
            }
        });
    }
}