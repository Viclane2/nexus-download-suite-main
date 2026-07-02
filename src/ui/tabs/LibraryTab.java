package ui.tabs;

import core.GestionnaireTaches;
import core.StatutTache;
import core.TacheTelechargement;
import ui.Theme;
import ui.components.FuturisticButton;
import ui.components.ModernScrollBarUI;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Onglet Bibliothèque (Historique).
 * Affiche un tableau récapitulatif interactif de l'historique complet des tâches.
 * Fournit une recherche instantanée et des contrôles pour relancer, supprimer
 * ou ouvrir le dossier contenant le fichier téléchargé.
 */
public class LibraryTab extends JPanel {

    private final GestionnaireTaches gestionnaire;
    private final JTable table;
    private final ModeleTable modeleTable;
    private final JTextField champRecherche;
    private final java.util.function.BiConsumer<String, String> relancerCallback;

    public LibraryTab(GestionnaireTaches gestionnaire, java.util.function.BiConsumer<String, String> relancerCallback) {
        this.gestionnaire = gestionnaire;
        this.relancerCallback = relancerCallback;

        setLayout(new BorderLayout(0, 12));
        setOpaque(false);
        setBorder(new EmptyBorder(14, 16, 12, 16));

        // 1. En-tête de recherche
        JPanel panelRecherche = new JPanel(new BorderLayout(10, 0));
        panelRecherche.setOpaque(false);
        panelRecherche.setBorder(new EmptyBorder(0, 0, 4, 0));

        champRecherche = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(new Color(15, 23, 42, 220));
                g2.fillRoundRect(0, 0, w, h, 8, 8);

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

                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2d.setFont(Theme.POLICE_NORMALE);
                    g2d.setColor(Theme.TEXTE_SECONDAIRE);
                    g2d.drawString("Rechercher dans l'historique par nom...", 12, getHeight() / 2 + 5);
                    g2d.dispose();
                }
            }
        };
        champRecherche.setOpaque(false);
        champRecherche.setCaretColor(Color.WHITE);
        champRecherche.setForeground(Color.WHITE);
        champRecherche.setFont(Theme.POLICE_NORMALE);
        champRecherche.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        champRecherche.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filtrer(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filtrer(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filtrer(); }
        });
        
        panelRecherche.add(champRecherche, BorderLayout.CENTER);
        add(panelRecherche, BorderLayout.NORTH);

        // 2. Tableau d'historique
        modeleTable = new ModeleTable();
        table = new JTable(modeleTable);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setBackground(Theme.FOND_CARTE);
        table.setForeground(Theme.TEXTE_PRINCIPAL);
        table.setFont(Theme.POLICE_NORMALE);
        table.setGridColor(Theme.BORDURE_CARTE);
        table.setSelectionBackground(new Color(Theme.ACCENT_MAGENTA.getRed(), Theme.ACCENT_MAGENTA.getGreen(), Theme.ACCENT_MAGENTA.getBlue(), 60));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(Theme.FOND_APPLICATION);
        header.setForeground(Theme.ACCENT_CYAN);
        header.setFont(Theme.POLICE_SECTION);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDURE_CARTE));

        table.setDefaultRenderer(Object.class, new RenduCelluleDefaut());
        table.getColumnModel().getColumn(2).setCellRenderer(new RenduCelluleStatut());
        table.getColumnModel().getColumn(3).setCellRenderer(new RenduCelluleProgression());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDURE_CARTE, 1));
        scroll.setBackground(Theme.FOND_APPLICATION);
        scroll.getViewport().setBackground(Theme.FOND_APPLICATION);
        scroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        add(scroll, BorderLayout.CENTER);

        // 3. Actions bas de tableau
        JPanel panelActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panelActions.setOpaque(false);

        FuturisticButton btnDossier = new FuturisticButton("Ouvrir dossier", Theme.ACCENT_CYAN);
        btnDossier.setPreferredSize(new Dimension(140, 28));
        btnDossier.setFont(Theme.POLICE_PETITE);
        btnDossier.addActionListener(e -> ouvrirDossierSelectionne());

        FuturisticButton btnRelancer = new FuturisticButton("Relancer", Theme.ACCENT_MAGENTA);
        btnRelancer.setPreferredSize(new Dimension(110, 28));
        btnRelancer.setFont(Theme.POLICE_PETITE);
        btnRelancer.addActionListener(e -> relancerSelection());

        FuturisticButton btnSupprimer = new FuturisticButton("Supprimer", Theme.ROUGE_ERREUR);
        btnSupprimer.setPreferredSize(new Dimension(110, 28));
        btnSupprimer.setFont(Theme.POLICE_PETITE);
        btnSupprimer.addActionListener(e -> supprimerSelectionne());

        panelActions.add(btnDossier);
        panelActions.add(btnRelancer);
        panelActions.add(btnSupprimer);
        add(panelActions, BorderLayout.SOUTH);

        rafraichir();
    }

    public void rafraichir() {
        modeleTable.mettreAJour(gestionnaire.lister());
        filtrer();
    }

    private void filtrer() {
        String recherche = champRecherche.getText().trim().toLowerCase();
        modeleTable.filtrer(recherche);
    }

    private void ouvrirDossierSelectionne() {
        int r = table.getSelectedRow();
        if (r >= 0) {
            TacheTelechargement t = modeleTable.obtenirTache(r);
            if (t != null) {
                File dossier = new File("telechargements");
                if (dossier.exists()) {
                    try {
                        Desktop.getDesktop().open(dossier);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this, "Impossible d'ouvrir le répertoire.", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Sélectionnez un téléchargement dans la table.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void relancerSelection() {
        int r = table.getSelectedRow();
        if (r >= 0) {
            TacheTelechargement t = modeleTable.obtenirTache(r);
            if (t != null) {
                relancerCallback.accept(t.getNomFichier(), t.getUrlSource());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Sélectionnez un téléchargement dans la table.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void supprimerSelectionne() {
        int r = table.getSelectedRow();
        if (r >= 0) {
            TacheTelechargement t = modeleTable.obtenirTache(r);
            if (t != null) {
                int confirm = JOptionPane.showConfirmDialog(this, 
                        "Supprimer \"" + t.getNomFichier() + "\" de l'historique ?", 
                        "Confirmation", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    gestionnaire.retirer(t);
                    rafraichir();
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Sélectionnez un téléchargement dans la table.", "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // --- Modèle JTable Interne ---
    private class ModeleTable extends AbstractTableModel {
        private final String[] entetes = {"Nom du fichier", "Taille", "Statut", "Progression", "Début", "Fin"};
        private final List<TacheTelechargement> toutesLesTaches = new ArrayList<>();
        private final List<TacheTelechargement> tachesFiltrees = new ArrayList<>();

        public void mettreAJour(List<TacheTelechargement> list) {
            toutesLesTaches.clear();
            toutesLesTaches.addAll(list);
            fireTableDataChanged();
        }

        public void filtrer(String query) {
            tachesFiltrees.clear();
            if (query.isEmpty()) {
                tachesFiltrees.addAll(toutesLesTaches);
            } else {
                tachesFiltrees.addAll(toutesLesTaches.stream()
                        .filter(t -> t.getNomFichier().toLowerCase().contains(query))
                        .collect(Collectors.toList()));
            }
            fireTableDataChanged();
        }

        public TacheTelechargement obtenirTache(int index) {
            if (index >= 0 && index < tachesFiltrees.size()) {
                return tachesFiltrees.get(index);
            }
            return null;
        }

        @Override public int getRowCount() { return tachesFiltrees.size(); }
        @Override public int getColumnCount() { return entetes.length; }
        @Override public String getColumnName(int c) { return entetes[c]; }

        @Override
        public Object getValueAt(int r, int c) {
            TacheTelechargement t = tachesFiltrees.get(r);
            if (t == null) return "";
            return switch (c) {
                case 0 -> t.getNomFichier();
                case 1 -> formaterTailleMo(t.getTailleTotaleMo());
                case 2 -> t.getStatut();
                case 3 -> t.getProgression();
                case 4 -> t.getHeureDebutFormatee();
                case 5 -> t.getHeureFinFormatee();
                default -> "";
            };
        }

        private String formaterTailleMo(double tailleMo) {
            if (tailleMo < 0) return "Inconnue";
            return tailleMo < 1 ? Math.round(tailleMo * 1024) + " Ko" : String.format("%.1f Mo", tailleMo);
        }
    }

    // --- Renders personnalisés ---
    private static class RenduCelluleDefaut extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
            setBackground(Theme.FOND_CARTE);
            setForeground(Theme.TEXTE_PRINCIPAL);
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    private static class RenduCelluleStatut extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
            setBackground(Theme.FOND_CARTE);
            if (val instanceof StatutTache s) {
                setForeground(Theme.couleurStatut(s));
                setText(s.getLibelle());
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }

    private static class RenduCelluleProgression extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
            super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
            setBackground(Theme.FOND_CARTE);
            setForeground(Theme.TEXTE_SECONDAIRE);
            if (val instanceof Double d) {
                if (d < 0) {
                    setText("calcul...");
                } else {
                    setText(String.format("%.0f%%", d));
                }
            }
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            return this;
        }
    }
}
