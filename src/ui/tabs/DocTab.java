package ui.tabs;

import ui.Theme;
import ui.components.FuturisticButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Onglet de Documentation interactive.
 * Affiche la structure architecturale (interfaces, thread-safety, verrous) sur la gauche.
 * Affiche un simulateur vectoriel interactif et animé représentant le cycle de vie
 * d'un Thread Java (NEW ➔ RUNNABLE ➔ TIMED_WAITING ➔ TERMINATED).
 */
public class DocTab extends JPanel {

    private int etatActif = 0; // 0: NEW, 1: RUNNABLE, 2: TIMED_WAITING, 3: TERMINATED
    private boolean transitionEnCours = false;
    private double progressionTransition = 0.0;
    private int etatSource = 0;
    private int etatCible = 0;

    // Coordonnées des centres des disques d'états
    private final Point[] centres = {
            new Point(70, 100),   // NEW
            new Point(230, 100),  // RUNNABLE
            new Point(230, 250),  // TIMED_WAITING
            new Point(390, 100)   // TERMINATED
    };

    private final String[] nomsEtats = {"NEW", "RUNNABLE", "TIMED WAITING", "TERMINATED"};
    private Timer timerAnim;

    public DocTab() {
        setLayout(new BorderLayout(20, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        // --- Volet Gauche : Documentation Technique ---
        JPanel panelGauche = new JPanel(new BorderLayout());
        panelGauche.setOpaque(false);
        panelGauche.setPreferredSize(new Dimension(380, 400));
        
        JLabel titreDoc = new JLabel("DOCUMENTATION TECHNIQUE", SwingConstants.LEFT);
        titreDoc.setFont(Theme.POLICE_SECTION);
        titreDoc.setForeground(Theme.ACCENT_CYAN);
        titreDoc.setBorder(new EmptyBorder(0, 0, 10, 0));
        panelGauche.add(titreDoc, BorderLayout.NORTH);

        JTextPane textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setBackground(new Color(15, 23, 42, 220));
        textPane.setForeground(Color.WHITE);
        textPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDURE_CARTE, 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        // Guide exhaustif de répartition des équipes et des intégrations (Cahier des charges TP)
        String htmlDoc = "<html><body style=\"font-family:'Segoe UI', sans-serif; font-size:11px; color:#cbd5e1; margin:0; padding:2px;\">"
                + "<h2 style=\"color:#00f0ff; font-size:13px; margin:0 0 12px 0; border-bottom:1px solid #22355b; padding-bottom:5px; letter-spacing:1px;\">GUIDE D'INTEGRATION DES EQUIPES (TP ICT308)</h2>"
                
                + "<div style=\"margin-bottom:14px; border-left:3px solid #00f0ff; padding-left:8px;\">"
                + "  <b style=\"color:#00f0ff; font-size:11px;\">Equipe 1 — Core & Metier (3 etudiants)</b>"
                + "  <br><b>Role :</b> Conception du coeur logique de l'application."
                + "  <br><b>Livrables :</b> Classe <code>TacheTelechargement</code> (Runnable, encapsulation private stricte), "
                + "gestion des exceptions personnalisees (<code>TelechargementEchoueException</code>), <code>GestionnaireTaches</code> "
                + "de centralisation et Javadoc complete."
                + "  <br><b>Points d'integration :</b> Fournit <code>ProgressionListener</code> à l'IHM Swing et ses structures Serialisables à l'equipe Persistance."
                + "  <br><b style=\"color:#94a3b8;\">Bareme :</b> Architecture POO (4 pts), Robustesse & Exceptions (2 pts)."
                + "</div>"
                
                + "<div style=\"margin-bottom:14px; border-left:3px solid #d600ff; padding-left:8px;\">"
                + "  <b style=\"color:#d600ff; font-size:11px;\">Equipe 2 — Persistance & Donnees (2 etudiants)</b>"
                + "  <br><b>Role :</b> Sauvegarde de l'historique et gestion des flux de donnees."
                + "  <br><b>Livrables :</b> Sauvegarde/chargement binaire ObjectOutputStream/InputStream (<code>historique.ser</code>), "
                + "generateur de rapports multiformats <code>CSV</code>, <code>HTML</code> et <code>PDF</code> programmatiquement "
                + "et pattern <code>try-with-resources</code> obligatoire."
                + "  <br><b>Points d'integration :</b> Se synchronise avec le Core sur la serialisation de <code>TacheTelechargement</code>, "
                + "fournit le chargement d'historique au demarrage de l'IHM."
                + "  <br><b style=\"color:#94a3b8;\">Bareme :</b> Persistance des Donnees (3 pts)."
                + "</div>"
                
                + "<div style=\"margin-bottom:14px; border-left:3px solid #00ff88; padding-left:8px;\">"
                + "  <b style=\"color:#00ff88; font-size:11px;\">Equipe 3 — IHM Swing (3 etudiants)</b>"
                + "  <br><b>Role :</b> Design graphique d'une interface cyberpunk claire et responsive."
                + "  <br><b>Livrables :</b> Fenetre principale en BorderLayout avec Sidebar et CardLayout, insertion dynamique de "
                + "<code>LigneTachePanel</code> avec <code>JProgressBar</code> dediee, boites de dialogues JOptionPane et respect de "
                + "la thread-safety via <code>SwingUtilities.invokeLater()</code>."
                + "  <br><b>Points d'integration :</b> Implemente le listener du Core, affiche l'historique charge par la Persistance."
                + "  <br><b style=\"color:#94a3b8;\">Bareme :</b> Interface Graphique Swing (4 pts)."
                + "</div>"
                
                + "</body></html>";
        textPane.setText(htmlDoc);

        JScrollPane scrollDoc = new JScrollPane(textPane);
        scrollDoc.setBorder(BorderFactory.createEmptyBorder());
        scrollDoc.setOpaque(false);
        scrollDoc.getViewport().setOpaque(false);
        panelGauche.add(scrollDoc, BorderLayout.CENTER);

        // --- Volet Droit : Simulateur interactif de Threads ---
        JPanel panelDroite = new JPanel(new BorderLayout(0, 10));
        panelDroite.setOpaque(false);

        JLabel titreSim = new JLabel("SIMULATEUR DU CYCLE DE VIE DES THREADS", SwingConstants.LEFT);
        titreSim.setFont(Theme.POLICE_SECTION);
        titreSim.setForeground(Theme.ACCENT_MAGENTA);
        panelDroite.add(titreSim, BorderLayout.NORTH);

        // Canvas de dessin interactif des états
        JPanel canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int rayon = 30;

                // 1. Dessiner les lignes de transition
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(Theme.BORDURE_CARTE);

                // NEW -> RUNNABLE
                dessinerFleche(g2, centres[0].x + rayon, centres[0].y, centres[1].x - rayon, centres[1].y, "start()");
                // RUNNABLE -> TIMED_WAITING
                dessinerFleche(g2, centres[1].x - 12, centres[1].y + rayon, centres[2].x - 12, centres[2].y - rayon, "Thread.sleep()");
                // TIMED_WAITING -> RUNNABLE
                dessinerFleche(g2, centres[2].x + 12, centres[2].y - rayon, centres[1].x + 12, centres[1].y + rayon, "Temps ecoule");
                // RUNNABLE -> TERMINATED
                dessinerFleche(g2, centres[1].x + rayon, centres[1].y, centres[3].x - rayon, centres[3].y, "run() termine");

                // 2. Dessiner les disques d'états
                for (int i = 0; i < 4; i++) {
                    Point c = centres[i];
                    boolean actif = (i == etatActif);

                    if (actif) {
                        Color colNeon = (i == 3) ? Theme.ROUGE_ERREUR : ((i == 2) ? Theme.ORANGE_ATTENTE : Theme.ACCENT_CYAN);
                        Theme.dessinerLueur(g2, new Ellipse2D.Double(c.x - rayon, c.y - rayon, rayon * 2, rayon * 2), colNeon, 6);
                        g2.setColor(new Color(colNeon.getRed(), colNeon.getGreen(), colNeon.getBlue(), 60));
                        g2.fillOval(c.x - rayon, c.y - rayon, rayon * 2, rayon * 2);
                        g2.setColor(colNeon);
                    } else {
                        g2.setColor(new Color(15, 23, 42, 200));
                        g2.fillOval(c.x - rayon, c.y - rayon, rayon * 2, rayon * 2);
                        g2.setColor(Theme.BORDURE_CARTE);
                    }

                    g2.setStroke(new BasicStroke(actif ? 2.2f : 1.0f));
                    g2.drawOval(c.x - rayon, c.y - rayon, rayon * 2, rayon * 2);

                    g2.setFont(Theme.POLICE_PETITE);
                    g2.setColor(actif ? Color.WHITE : Theme.TEXTE_SECONDAIRE);
                    FontMetrics fm = g2.getFontMetrics();
                    String nom = nomsEtats[i];
                    int tx = c.x - fm.stringWidth(nom) / 2;
                    int ty = c.y + fm.getAscent() / 2 - 2;
                    g2.drawString(nom, tx, ty);
                }

                // 3. Dessiner le point d'énergie de transition en déplacement
                if (transitionEnCours) {
                    Point src = centres[etatSource];
                    Point cib = centres[etatCible];
                    double px = src.x + (cib.x - src.x) * progressionTransition;
                    double py = src.y + (cib.y - src.y) * progressionTransition;

                    g2.setColor(Theme.ACCENT_MAGENTA);
                    Theme.dessinerLueur(g2, new Ellipse2D.Double(px - 5, py - 5, 10, 10), Theme.ACCENT_MAGENTA, 4);
                    g2.fillOval((int) px - 5, (int) py - 5, 10, 10);
                }

                g2.dispose();
            }
        };
        canvas.setBackground(new Color(11, 16, 27, 210));
        canvas.setBorder(BorderFactory.createLineBorder(Theme.BORDURE_CARTE, 1));
        panelDroite.add(canvas, BorderLayout.CENTER);

        // Actions de pilotage du simulateur
        JPanel panelBoutons = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        panelBoutons.setOpaque(false);

        FuturisticButton btnStart = new FuturisticButton("start()", Theme.ACCENT_CYAN);
        btnStart.setPreferredSize(new Dimension(86, 26));
        btnStart.setFont(Theme.POLICE_PETITE);
        btnStart.addActionListener(e -> declencherTransition(0, 1));

        FuturisticButton btnSleep = new FuturisticButton("sleep()", Theme.ORANGE_ATTENTE);
        btnSleep.setPreferredSize(new Dimension(86, 26));
        btnSleep.setFont(Theme.POLICE_PETITE);
        btnSleep.addActionListener(e -> declencherTransition(1, 2));

        FuturisticButton btnWake = new FuturisticButton("wake()", Theme.ACCENT_CYAN);
        btnWake.setPreferredSize(new Dimension(86, 26));
        btnWake.setFont(Theme.POLICE_PETITE);
        btnWake.addActionListener(e -> declencherTransition(2, 1));

        FuturisticButton btnEnd = new FuturisticButton("run complete", Theme.ROUGE_ERREUR);
        btnEnd.setPreferredSize(new Dimension(110, 26));
        btnEnd.setFont(Theme.POLICE_PETITE);
        btnEnd.addActionListener(e -> declencherTransition(1, 3));

        FuturisticButton btnReset = new FuturisticButton("Reset", new Color(112, 128, 144));
        btnReset.setPreferredSize(new Dimension(74, 26));
        btnReset.setFont(Theme.POLICE_PETITE);
        btnReset.addActionListener(e -> {
            etatActif = 0;
            transitionEnCours = false;
            canvas.repaint();
        });

        panelBoutons.add(btnStart);
        panelBoutons.add(btnSleep);
        panelBoutons.add(btnWake);
        panelBoutons.add(btnEnd);
        panelBoutons.add(btnReset);
        panelDroite.add(panelBoutons, BorderLayout.SOUTH);

        add(panelGauche, BorderLayout.WEST);
        add(panelDroite, BorderLayout.CENTER);

        // Timer de progression de la transition animée
        timerAnim = new Timer(15, e -> {
            if (transitionEnCours) {
                progressionTransition += 0.04;
                if (progressionTransition >= 1.0) {
                    progressionTransition = 1.0;
                    transitionEnCours = false;
                    etatActif = etatCible;
                    ((Timer) e.getSource()).stop();
                }
                canvas.repaint();
            }
        });
    }

    private void declencherTransition(int src, int cib) {
        if (transitionEnCours) return;
        if (etatActif != src) {
            JOptionPane.showMessageDialog(this, 
                    "Action impossible. Le Thread n'est pas dans l'état requis.", 
                    "Verrou d'état", JOptionPane.WARNING_MESSAGE);
            return;
        }
        etatSource = src;
        etatCible = cib;
        progressionTransition = 0.0;
        transitionEnCours = true;
        timerAnim.start();
    }

    private void dessinerFleche(Graphics2D g2, int x1, int y1, int x2, int y2, String label) {
        g2.drawLine(x1, y1, x2, y2);
        
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int tail = 6;
        
        Path2D pointe = new Path2D.Double();
        pointe.moveTo(x2, y2);
        pointe.lineTo(x2 - tail * Math.cos(angle - Math.PI / 6), y2 - tail * Math.sin(angle - Math.PI / 6));
        pointe.lineTo(x2 - tail * Math.cos(angle + Math.PI / 6), y2 - tail * Math.sin(angle + Math.PI / 6));
        pointe.closePath();
        g2.fill(pointe);

        g2.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        g2.setColor(Theme.TEXTE_SECONDAIRE);
        int lx = (x1 + x2) / 2 - 20;
        int ly = (y1 + y2) / 2 - 4;
        
        if (x1 == x2) {
            lx = x1 + 8;
            ly = (y1 + y2) / 2 + 3;
        }
        g2.drawString(label, lx, ly);
    }
}
