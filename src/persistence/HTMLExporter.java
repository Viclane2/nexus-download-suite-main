package persistence;

import core.TacheTelechargement;
import core.StatutTache;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Générateur de rapports HTML5 autonomes.
 * Exporte l'historique sous forme d'une page Web interactive, styled dans la charte
 * sombre cyberpunk avec polices Google Fonts, grille CSS responsive de cartes de métriques,
 * badges colorés à l'effigie des statuts et barres de progression graphiques.
 */
public final class HTMLExporter {

    public static boolean exporter(List<TacheTelechargement> taches, String cheminFichier) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(cheminFichier))) {
            pw.println("<!DOCTYPE html>");
            pw.println("<html lang=\"fr\">");
            pw.println("<head>");
            pw.println("    <meta charset=\"UTF-8\">");
            pw.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
            pw.println("    <title>Nexus Download Suite - Rapport de téléchargement</title>");
            pw.println("    <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&display=swap\" rel=\"stylesheet\">");
            pw.println("    <style>");
            pw.println("        :root {");
            pw.println("            --fond: #060913;");
            pw.println("            --fond-carte: #0d1527;");
            pw.println("            --bordure: #22355b;");
            pw.println("            --cyan: #00f0ff;");
            pw.println("            --magenta: #d600ff;");
            pw.println("            --vert: #00ff88;");
            pw.println("            --rouge: #ff0055;");
            pw.println("            --texte: #ffffff;");
            pw.println("            --texte-sec: #94a3b8;");
            pw.println("        }");
            pw.println("        body {");
            pw.println("            font-family: 'Outfit', sans-serif;");
            pw.println("            background-color: var(--fond);");
            pw.println("            color: var(--texte);");
            pw.println("            margin: 0;");
            pw.println("            padding: 40px 20px;");
            pw.println("            display: flex;");
            pw.println("            flex-direction: column;");
            pw.println("            align-items: center;");
            pw.println("        }");
            pw.println("        .container {");
            pw.println("            width: 100%;");
            pw.println("            max-width: 1000px;");
            pw.println("        }");
            pw.println("        header {");
            pw.println("            display: flex;");
            pw.println("            justify-content: space-between;");
            pw.println("            align-items: center;");
            pw.println("            border-bottom: 2px solid var(--bordure);");
            pw.println("            padding-bottom: 20px;");
            pw.println("            margin-bottom: 30px;");
            pw.println("        }");
            pw.println("        .title-group h1 {");
            pw.println("            margin: 0;");
            pw.println("            font-weight: 800;");
            pw.println("            font-size: 28px;");
            pw.println("            letter-spacing: 2px;");
            pw.println("            background: linear-gradient(45deg, var(--cyan), var(--magenta));");
            pw.println("            -webkit-background-clip: text;");
            pw.println("            -webkit-text-fill-color: transparent;");
            pw.println("        }");
            pw.println("        .title-group p {");
            pw.println("            margin: 5px 0 0 0;");
            pw.println("            color: var(--texte-sec);");
            pw.println("            font-size: 14px;");
            pw.println("        }");
            pw.println("        .date-badge {");
            pw.println("            font-size: 13px;");
            pw.println("            background: rgba(0, 240, 255, 0.1);");
            pw.println("            border: 1px solid var(--cyan);");
            pw.println("            color: var(--cyan);");
            pw.println("            padding: 6px 14px;");
            pw.println("            border-radius: 20px;");
            pw.println("            box-shadow: 0 0 10px rgba(0, 240, 255, 0.2);");
            pw.println("        }");
            pw.println("        .stats-grid {");
            pw.println("            display: grid;");
            pw.println("            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));");
            pw.println("            gap: 20px;");
            pw.println("            margin-bottom: 30px;");
            pw.println("        }");
            pw.println("        .stat-card {");
            pw.println("            background: var(--fond-carte);");
            pw.println("            border: 1px solid var(--bordure);");
            pw.println("            border-radius: 12px;");
            pw.println("            padding: 20px;");
            pw.println("            text-align: center;");
            pw.println("            box-shadow: 0 4px 20px rgba(0,0,0,0.3);");
            pw.println("            transition: transform 0.3s, border-color 0.3s;");
            pw.println("        }");
            pw.println("        .stat-card:hover {");
            pw.println("            transform: translateY(-5px);");
            pw.println("            border-color: var(--magenta);");
            pw.println("        }");
            pw.println("        .stat-card h3 {");
            pw.println("            margin: 0;");
            pw.println("            font-size: 14px;");
            pw.println("            color: var(--texte-sec);");
            pw.println("            text-transform: uppercase;");
            pw.println("            letter-spacing: 1px;");
            pw.println("        }");
            pw.println("        .stat-card .val {");
            pw.println("            font-size: 32px;");
            pw.println("            font-weight: 800;");
            pw.println("            margin: 10px 0 0 0;");
            pw.println("            color: var(--cyan);");
            pw.println("        }");
            pw.println("        .stat-card.success .val { color: var(--vert); }");
            pw.println("        .stat-card.error .val { color: var(--rouge); }");
            pw.println("        .table-container {");
            pw.println("            background: var(--fond-carte);");
            pw.println("            border: 1px solid var(--bordure);");
            pw.println("            border-radius: 12px;");
            pw.println("            overflow: hidden;");
            pw.println("            box-shadow: 0 4px 20px rgba(0,0,0,0.3);");
            pw.println("        }");
            pw.println("        table {");
            pw.println("            width: 100%;");
            pw.println("            border-collapse: collapse;");
            pw.println("        }");
            pw.println("        th, td {");
            pw.println("            padding: 16px 20px;");
            pw.println("            text-align: left;");
            pw.println("            border-bottom: 1px solid var(--bordure);");
            pw.println("        }");
            pw.println("        th {");
            pw.println("            background: rgba(13, 21, 39, 0.8);");
            pw.println("            color: var(--cyan);");
            pw.println("            font-weight: 600;");
            pw.println("            text-transform: uppercase;");
            pw.println("            font-size: 12px;");
            pw.println("            letter-spacing: 1px;");
            pw.println("        }");
            pw.println("        tr:last-child td {");
            pw.println("            border-bottom: none;");
            pw.println("        }");
            pw.println("        tr:hover td {");
            pw.println("            background: rgba(0, 240, 255, 0.03);");
            pw.println("        }");
            pw.println("        .badge {");
            pw.println("            display: inline-block;");
            pw.println("            padding: 4px 10px;");
            pw.println("            border-radius: 6px;");
            pw.println("            font-size: 11px;");
            pw.println("            font-weight: 600;");
            pw.println("            text-transform: uppercase;");
            pw.println("        }");
            pw.println("        .badge.termine { background: rgba(0, 255, 136, 0.1); border: 1px solid var(--vert); color: var(--vert); }");
            pw.println("        .badge.en_cours { background: rgba(0, 240, 255, 0.1); border: 1px solid var(--cyan); color: var(--cyan); }");
            pw.println("        .badge.erreur { background: rgba(255, 0, 85, 0.1); border: 1px solid var(--rouge); color: var(--rouge); }");
            pw.println("        .badge.annule { background: rgba(148, 163, 184, 0.1); border: 1px solid var(--texte-sec); color: var(--texte-sec); }");
            pw.println("        .progress-bar-bg {");
            pw.println("            background: rgba(255,255,255,0.05);");
            pw.println("            border-radius: 10px;");
            pw.println("            height: 6px;");
            pw.println("            width: 100px;");
            pw.println("            overflow: hidden;");
            pw.println("        }");
            pw.println("        .progress-bar-fill {");
            pw.println("            height: 100%;");
            pw.println("            background: linear-gradient(90deg, var(--cyan), var(--magenta));");
            pw.println("        }");
            pw.println("        .progress-cell {");
            pw.println("            display: flex;");
            pw.println("            align-items: center;");
            pw.println("            gap: 10px;");
            pw.println("        }");
            pw.println("    </style>");
            pw.println("</head>");
            pw.println("<body>");
            pw.println("<div class=\"container\">");

            // En-tête
            pw.println("    <header>");
            pw.println("        <div class=\"title-group\">");
            pw.println("            <h1>NEXUS DOWNLOAD SUITE</h1>");
            pw.println("            <p>Rapport General des Telechargements • ICT308</p>");
            pw.println("        </div>");
            
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            pw.println("        <div class=\"date-badge\">Le " + LocalDateTime.now().format(dtf) + "</div>");
            pw.println("    </header>");

            // Statistiques globales
            int total = taches.size();
            int termines = 0;
            int erreurs = 0;
            double volume = 0.0;
            for (TacheTelechargement t : taches) {
                if (t.getStatut() == StatutTache.TERMINE) termines++;
                if (t.getStatut() == StatutTache.ERREUR) erreurs++;
                volume += t.getOctetsRecus() / (1024.0 * 1024.0);
            }

            pw.println("    <div class=\"stats-grid\">");
            pw.println("        <div class=\"stat-card\">");
            pw.println("            <h3>Total des T&acirc;ches</h3>");
            pw.println("            <div class=\"val\">" + total + "</div>");
            pw.println("        </div>");
            pw.println("        <div class=\"stat-card success\">");
            pw.println("            <h3>Succ&egrave;s</h3>");
            pw.println("            <div class=\"val\">" + termines + "</div>");
            pw.println("        </div>");
            pw.println("        <div class=\"stat-card error\">");
            pw.println("            <h3>&Eacute;checs</h3>");
            pw.println("            <div class=\"val\">" + erreurs + "</div>");
            pw.println("        </div>");
            pw.println("        <div class=\"stat-card\">");
            pw.println("            <h3>Volume Re&ccedil;u</h3>");
            pw.println("            <div class=\"val\">" + String.format("%.1f Mo", volume) + "</div>");
            pw.println("        </div>");
            pw.println("    </div>");

            // Tableau de données
            pw.println("    <div class=\"table-container\">");
            pw.println("        <table>");
            pw.println("            <thead>");
            pw.println("                <tr>");
            pw.println("                    <th>Nom du Fichier</th>");
            pw.println("                    <th>Taille</th>");
            pw.println("                    <th>Statut</th>");
            pw.println("                    <th>Progression</th>");
            pw.println("                    <th>Début / Fin</th>");
            pw.println("                </tr>");
            pw.println("            </thead>");
            pw.println("            <tbody>");

            for (TacheTelechargement t : taches) {
                pw.println("                <tr>");
                pw.println("                    <td style=\"font-weight: 600;\">" + htmlEncode(t.getNomFichier()) + "</td>");
                pw.println("                    <td>" + formaterTailleMo(t.getTailleTotaleMo()) + "</td>");
                
                String classBadge = switch (t.getStatut()) {
                    case TERMINE -> "termine";
                    case EN_COURS -> "en_cours";
                    case ERREUR -> "erreur";
                    default -> "annule";
                };
                pw.println("                    <td><span class=\"badge " + classBadge + "\">" + t.getStatut().getLibelle() + "</span></td>");
                
                pw.println("                    <td>");
                pw.println("                        <div class=\"progress-cell\">");
                pw.println("                            <div class=\"progress-bar-bg\">");
                pw.println("                                <div class=\"progress-bar-fill\" style=\"width: " + (int)t.getProgression() + "%;\"></div>");
                pw.println("                            </div>");
                pw.println("                            <span>" + String.format("%.0f%%", t.getProgression()) + "</span>");
                pw.println("                        </div>");
                pw.println("                    </td>");
                
                pw.println("                    <td style=\"font-size: 13px; color: var(--texte-sec);\">" + t.getHeureDebutFormatee() + " / " + t.getHeureFinFormatee() + "</td>");
                pw.println("                </tr>");
            }

            pw.println("            </tbody>");
            pw.println("        </table>");
            pw.println("    </div>");

            pw.println("</div>");
            pw.println("</body>");
            pw.println("</html>");

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String formaterTailleMo(double tailleMo) {
        if (tailleMo < 0) return "Inconnue";
        return tailleMo < 1 ? Math.round(tailleMo * 1024) + " Ko" : String.format("%.1f Mo", tailleMo);
    }

    private static String htmlEncode(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
