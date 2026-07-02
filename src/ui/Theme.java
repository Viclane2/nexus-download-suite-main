package ui;

import java.awt.*;

/**
 * Centralise la charte graphique de l'application (couleurs, polices, effets)
 * afin de garantir une interface futuriste et hautement professionnelle.
 */
public final class Theme {

    // Couleurs de fond et structurelles (Cyberpunk Dark Mode)
    public static final Color FOND_APPLICATION = new Color(10, 14, 23); // Deep Space Blue
    public static final Color FOND_CARTE = new Color(20, 29, 47);       // Dark Glass Card
    public static final Color BORDURE_CARTE = new Color(34, 49, 79);     // Neon cyan-tinted borders

    // Couleurs d'accents et statuts (non-finales pour le recoloriage dynamique)
    public static Color ACCENT_CYAN = new Color(0, 240, 255);       // Néon Cyan (Normal/Actif)
    public static Color ACCENT_MAGENTA = new Color(214, 0, 255);    // Néon Magenta (Global/Actions)
    public static final Color VERT_SUCCES = new Color(0, 255, 136);       // Néon Vert (Succès/Terminé)
    public static final Color ROUGE_ERREUR = new Color(255, 0, 85);       // Néon Rouge (Échec/Erreur)
    public static final Color GRIS_ANNULE = new Color(148, 163, 184);     // Gris ardoise (Annulé)
    public static final Color ORANGE_ATTENTE = new Color(255, 179, 0);    // Néon Orange (En attente)

    // Couleurs de texte
    public static final Color TEXTE_PRINCIPAL = new Color(248, 250, 252); // Blanc éclatant
    public static final Color TEXTE_SECONDAIRE = new Color(148, 163, 184);// Gris ardoise soft

    // Polices modernes
    public static final Font POLICE_TITRE = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font POLICE_SOUS_TITRE = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font POLICE_SECTION = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font POLICE_NORMALE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font POLICE_PETITE = new Font("Segoe UI", Font.PLAIN, 11);
    public static final Font POLICE_STAT_VALEUR = new Font("Segoe UI", Font.BOLD, 22);
    public static final Font POLICE_STAT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);

    /**
     * Associe chaque statut à sa couleur néon représentative.
     */
    public static Color couleurStatut(core.StatutTache statut) {
        return switch (statut) {
            case TERMINE -> VERT_SUCCES;
            case ERREUR -> ROUGE_ERREUR;
            case ANNULE -> GRIS_ANNULE;
            case EN_COURS -> ACCENT_CYAN;
            case EN_ATTENTE -> ORANGE_ATTENTE;
        };
    }

    /**
     * Dessine une lueur néon réaliste autour d'une forme géométrique
     */
    public static void dessinerLueur(Graphics2D g2, Shape forme, Color couleur, int epaisseur) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Composite compositeOriginal = g2.getComposite();
        for (int i = epaisseur; i > 0; i--) {
            float alpha = 0.08f / i;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(couleur);
            g2.setStroke(new BasicStroke(i * 1.8f));
            g2.draw(forme);
        }
        g2.setComposite(compositeOriginal);
    }

    private Theme() {
    }
}

