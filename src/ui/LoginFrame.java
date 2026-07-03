package ui;

import auth.SessionManager;
import db.DatabaseManager;
import db.UtilisateurDAO;
import model.Utilisateur;
import ui.components.LogoPainter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

/**
 * Fenêtre de connexion / inscription.
 * S'affiche au démarrage avant MainFrame.
 * Utilise le même thème cyberpunk que le reste de l'application.
 *
 * Deux modes :
 *   - CONNEXION  : vérification des identifiants en BD
 *   - INSCRIPTION : création d'un nouveau compte
 */
public class LoginFrame extends JFrame {

    private static final Color BG_DARK      = new Color(8, 12, 24);
    private static final Color PANEL_BG     = new Color(15, 23, 42, 240);
    private static final Color ACCENT_CYAN  = new Color(0, 212, 255);
    private static final Color ACCENT_MAG   = new Color(255, 0, 128);
    private static final Color TEXTE        = Color.WHITE;
    private static final Color TEXTE_SEC    = new Color(140, 160, 200);
    private static final Font  FONT_TITRE   = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font  FONT_LABEL   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FONT_INPUT   = new Font("Consolas", Font.PLAIN, 13);
    private static final Font  FONT_BTN     = new Font("Segoe UI", Font.BOLD, 13);

    private final UtilisateurDAO dao = new UtilisateurDAO();
    private final Runnable onConnexionReussie;

    private JTextField     champNom;
    private JPasswordField champMdp;
    private JPasswordField champMdpConfirm;
    private JLabel         labelConfirmLbl;
    private JLabel         labelMessage;
    private JButton        btnAction;
    private JButton        btnBasculer;
    private JLabel         labelTitre;
    private boolean        modeInscription = false;

    /**
     * @param onConnexionReussie callback exécuté après connexion réussie
     */
    public LoginFrame(Runnable onConnexionReussie) {
        super("NEXUS Download Suite — Connexion");
        this.onConnexionReussie = onConnexionReussie;

        // Initialiser les tables BD
        dao.initialiserTable();

        configurerFenetre();
        construireInterface();
        setVisible(true);
    }

    private void configurerFenetre() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 480);
        setMinimumSize(new Dimension(380, 420));
        setLocationRelativeTo(null);
        setResizable(false);
        setIconImage(LogoPainter.genererIconeApplication(32));
        getContentPane().setBackground(BG_DARK);
    }

    private void construireInterface() {
        setLayout(new BorderLayout());

        // ── En-tête ────────────────────────────────────────────────────────
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Dégradé
                GradientPaint gp = new GradientPaint(0, 0, new Color(0, 30, 60),
                        getWidth(), 0, new Color(30, 0, 60));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Ligne néon bas
                g2.setColor(ACCENT_CYAN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(420, 80));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(18, 24, 16, 24));

        JLabel logo = new JLabel("⬡ NEXUS DOWNLOAD SUITE");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logo.setForeground(ACCENT_CYAN);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel soustitre = new JLabel("Système d'authentification sécurisé");
        soustitre.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        soustitre.setForeground(TEXTE_SEC);
        soustitre.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(logo);
        header.add(Box.createVerticalStrut(6));
        header.add(soustitre);
        add(header, BorderLayout.NORTH);

        // ── Panneau central ────────────────────────────────────────────────
        JPanel centre = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_DARK);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        centre.setLayout(new GridBagLayout());
        centre.setOpaque(true);
        centre.setBorder(new EmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0; gbc.weightx = 1.0;

        // Titre mode
        labelTitre = new JLabel("CONNEXION", SwingConstants.CENTER);
        labelTitre.setFont(FONT_TITRE);
        labelTitre.setForeground(TEXTE);
        gbc.gridy = 0;
        centre.add(labelTitre, gbc);

        // Champ nom
        gbc.gridy = 1;
        centre.add(creerLabel("Nom d'utilisateur"), gbc);
        gbc.gridy = 2;
        champNom = creerChampTexte("Entrez votre identifiant...");
        centre.add(champNom, gbc);

        // Champ mot de passe
        gbc.gridy = 3;
        centre.add(creerLabel("Mot de passe"), gbc);
        gbc.gridy = 4;
        champMdp = creerChampMdp("Entrez votre mot de passe...");
        centre.add(champMdp, gbc);

        // Champ confirmation (inscription seulement)
        labelConfirmLbl = creerLabel("Confirmer le mot de passe");
        labelConfirmLbl.setVisible(false);
        gbc.gridy = 5;
        centre.add(labelConfirmLbl, gbc);

        champMdpConfirm = creerChampMdp("Répétez le mot de passe...");
        champMdpConfirm.setVisible(false);
        gbc.gridy = 6;
        centre.add(champMdpConfirm, gbc);

        // Message d'erreur/succès
        labelMessage = new JLabel(" ", SwingConstants.CENTER);
        labelMessage.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        labelMessage.setForeground(ACCENT_MAG);
        gbc.gridy = 7;
        gbc.insets = new Insets(2, 0, 2, 0);
        centre.add(labelMessage, gbc);

        // Bouton principal
        gbc.gridy = 8;
        gbc.insets = new Insets(8, 0, 4, 0);
        btnAction = creerBouton("Se connecter", ACCENT_CYAN);
        centre.add(btnAction, gbc);

        // Bouton basculer mode
        gbc.gridy = 9;
        gbc.insets = new Insets(4, 0, 0, 0);
        btnBasculer = creerBoutonLien("Pas encore de compte ? S'inscrire");
        centre.add(btnBasculer, gbc);

        add(centre, BorderLayout.CENTER);

        // ── Listeners ──────────────────────────────────────────────────────
        btnAction.addActionListener(e -> traiterAction());
        btnBasculer.addActionListener(e -> basculerMode());

        // Valider avec Entrée
        KeyAdapter enterListener = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) traiterAction();
            }
        };
        champNom.addKeyListener(enterListener);
        champMdp.addKeyListener(enterListener);
        champMdpConfirm.addKeyListener(enterListener);
    }

    // ── Logique principale ────────────────────────────────────────────────────

    private void traiterAction() {
        String nom = champNom.getText().trim();
        String mdp = new String(champMdp.getPassword());

        if (nom.isEmpty() || mdp.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        if (modeInscription) {
            // ── INSCRIPTION ─────────────────────────────────────────────────
            String mdpConfirm = new String(champMdpConfirm.getPassword());
            if (!mdp.equals(mdpConfirm)) {
                afficherErreur("Les mots de passe ne correspondent pas.");
                return;
            }
            if (mdp.length() < 4) {
                afficherErreur("Le mot de passe doit faire au moins 4 caractères.");
                return;
            }
            if (nom.length() < 3) {
                afficherErreur("Le nom d'utilisateur doit faire au moins 3 caractères.");
                return;
            }

            btnAction.setEnabled(false);
            btnAction.setText("Inscription...");

            new Thread(() -> {
                boolean ok = dao.inscrire(nom, mdp);
                SwingUtilities.invokeLater(() -> {
                    btnAction.setEnabled(true);
                    btnAction.setText("S'inscrire");
                    if (ok) {
                        // Connexion automatique après inscription
                        Utilisateur u = dao.connecter(nom, mdp);
                        if (u != null) ouvrirApplication(u);
                    } else {
                        afficherErreur("Ce nom d'utilisateur est déjà pris.");
                    }
                });
            }, "Thread-Inscription").start();

        } else {
            // ── CONNEXION ────────────────────────────────────────────────────
            btnAction.setEnabled(false);
            btnAction.setText("Connexion...");

            new Thread(() -> {
                Utilisateur u = dao.connecter(nom, mdp);
                SwingUtilities.invokeLater(() -> {
                    btnAction.setEnabled(true);
                    btnAction.setText("Se connecter");
                    if (u != null) {
                        ouvrirApplication(u);
                    } else {
                        afficherErreur("Identifiants incorrects. Réessayez.");
                        champMdp.setText("");
                    }
                });
            }, "Thread-Connexion").start();
        }
    }

    private void ouvrirApplication(Utilisateur u) {
        SessionManager.getInstance().connecter(u);
        System.out.println("[AUTH] Connecté en tant que : " + u.getNomUtilisateur());
        dispose();
        onConnexionReussie.run();
    }

    private void basculerMode() {
        modeInscription = !modeInscription;
        if (modeInscription) {
            labelTitre.setText("INSCRIPTION");
            btnAction.setText("S'inscrire");
            btnBasculer.setText("Déjà un compte ? Se connecter");
            labelConfirmLbl.setVisible(true);
            champMdpConfirm.setVisible(true);
        } else {
            labelTitre.setText("CONNEXION");
            btnAction.setText("Se connecter");
            btnBasculer.setText("Pas encore de compte ? S'inscrire");
            labelConfirmLbl.setVisible(false);
            champMdpConfirm.setVisible(false);
        }
        labelMessage.setText(" ");
        revalidate();
        repaint();
    }

    // ── Helpers de construction UI ────────────────────────────────────────────

    private void afficherErreur(String msg) {
        labelMessage.setForeground(ACCENT_MAG);
        labelMessage.setText(msg);
    }

    private JLabel creerLabel(String texte) {
        JLabel lbl = new JLabel(texte);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXTE_SEC);
        return lbl;
    }

    private JTextField creerChampTexte(String placeholder) {
        JTextField champ = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(PANEL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isFocusOwner() ? ACCENT_CYAN : new Color(40, 60, 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D ph = (Graphics2D) g.create();
                    ph.setFont(FONT_INPUT);
                    ph.setColor(new Color(80, 100, 130));
                    ph.drawString(placeholder, 10, getHeight()/2 + 5);
                    ph.dispose();
                }
            }
        };
        champ.setOpaque(false);
        champ.setForeground(TEXTE);
        champ.setCaretColor(ACCENT_CYAN);
        champ.setFont(FONT_INPUT);
        champ.setBorder(new EmptyBorder(9, 10, 9, 10));
        champ.setPreferredSize(new Dimension(300, 38));
        return champ;
    }

    private JPasswordField creerChampMdp(String placeholder) {
        JPasswordField champ = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(PANEL_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isFocusOwner() ? ACCENT_CYAN : new Color(40, 60, 90));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
                if (getPassword().length == 0 && !isFocusOwner()) {
                    Graphics2D ph = (Graphics2D) g.create();
                    ph.setFont(FONT_INPUT);
                    ph.setColor(new Color(80, 100, 130));
                    ph.drawString(placeholder, 10, getHeight()/2 + 5);
                    ph.dispose();
                }
            }
        };
        champ.setOpaque(false);
        champ.setForeground(TEXTE);
        champ.setCaretColor(ACCENT_CYAN);
        champ.setFont(FONT_INPUT);
        champ.setBorder(new EmptyBorder(9, 10, 9, 10));
        champ.setPreferredSize(new Dimension(300, 38));
        champ.setEchoChar('●');
        return champ;
    }

    private JButton creerBouton(String texte, Color couleur) {
        JButton btn = new JButton(texte) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = isEnabled()
                        ? (getModel().isPressed() ? couleur.darker() : couleur)
                        : new Color(40, 60, 80);
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(base);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 10, 10);
                g2.setColor(isEnabled() ? TEXTE : TEXTE_SEC);
                g2.setFont(FONT_BTN);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(texte)) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(texte, tx, ty);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(FONT_BTN);
        btn.setPreferredSize(new Dimension(300, 40));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton creerBoutonLien(String texte) {
        JButton btn = new JButton(texte);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(ACCENT_CYAN);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}