package persistence;

import core.TacheTelechargement;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de rapports PDF autonome en Java pur.
 * Construit manuellement la structure logique et binaire d'un fichier PDF/1.4,
 * avec catalogue d'objets, ressources de police Helvetica, flux de contenu de page
 * et table des références croisées (xref) calculée dynamiquement au byte près.
 * Zéro dépendance externe requise.
 */
public final class PDFExporter {

    public static boolean exporter(List<TacheTelechargement> taches, String cheminFichier) {
        try (FileOutputStream out = new FileOutputStream(cheminFichier)) {
            List<Long> offsets = new ArrayList<>();
            List<String> objets = new ArrayList<>();

            // 1. Catalogue racine
            objets.add("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
            // 2. Index des pages
            objets.add("2 0 obj\n<< /Type /Pages /Kids [ 3 0 R ] /Count 1 >>\nendobj\n");
            // 3. Spécification de la page (format A4 standard 595x842)
            objets.add("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [ 0 0 595 842 ] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n");
            // 4. Déclaration de la police standard Helvetica
            objets.add("4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            // 5. Construction du flux textuel de la page
            StringBuilder sb = new StringBuilder();
            sb.append("BT\n"); // Début du bloc texte
            
            // Grand Titre principal
            sb.append("/F1 18 Tf\n"); // Police F1, taille 18
            sb.append("50 780 Td\n"); // Position initiale (x=50, y=780)
            sb.append("(NEXUS DOWNLOAD SUITE - RAPPORT GENERAL) Tj\n");
            
            // Sous-titre
            sb.append("/F1 10 Tf\n");
            sb.append("0 -26 Td\n");
            sb.append("(Genere programmatiquement par le noyau de persistance - Groupe 10) Tj\n");
            
            // En-têtes du tableau
            sb.append("/F1 11 Tf\n");
            sb.append("0 -42 Td\n");
            sb.append("(Nom du fichier) Tj\n");
            sb.append("220 0 Td\n");
            sb.append("(Taille) Tj\n");
            sb.append("100 0 Td\n");
            sb.append("(Statut) Tj\n");
            sb.append("100 0 Td\n");
            sb.append("(Progression) Tj\n");
            
            // Ligne de séparation
            sb.append("-420 -18 Td\n");
            sb.append("(----------------------------------------------------------------------------------------------------------------------) Tj\n");

            // Lignes de tâches
            for (TacheTelechargement t : taches) {
                sb.append("0 -20 Td\n");
                
                // Nettoyer les caractères spéciaux pour éviter les crashs de flux PDF
                String nom = t.getNomFichier();
                if (nom.length() > 32) {
                    nom = nom.substring(0, 30) + "..";
                }
                nom = nom.replaceAll("[\\(\\)\\\\]", ""); // Retire les parenthèses
                
                sb.append("(").append(nom).append(") Tj\n");
                
                sb.append("220 0 Td\n");
                sb.append("(").append(formaterTailleMo(t.getTailleTotaleMo())).append(") Tj\n");
                
                sb.append("100 0 Td\n");
                sb.append("(").append(t.getStatut().getLibelle()).append(") Tj\n");
                
                sb.append("100 0 Td\n");
                sb.append("(").append(String.format("%.0f%%", t.getProgression())).append(") Tj\n");
                
                // Retourner au début de la colonne pour la ligne suivante
                sb.append("-420 0 Td\n");
            }
            
            sb.append("ET\n"); // Fin du bloc texte

            String contenuStream = sb.toString();
            byte[] octetsStream = contenuStream.getBytes(StandardCharsets.ISO_8859_1);
            
            String enteteObjet5 = "5 0 obj\n<< /Length " + octetsStream.length + " >>\nstream\n";
            String finObjet5 = "\nendstream\nendobj\n";
            
            objets.add(enteteObjet5 + contenuStream + finObjet5);

            // Écriture du fichier binaire
            long offsetCourant = 0;
            byte[] octetsEntete = "%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1);
            out.write(octetsEntete);
            offsetCourant += octetsEntete.length;

            for (String obj : objets) {
                offsets.add(offsetCourant);
                byte[] octetsObj = obj.getBytes(StandardCharsets.ISO_8859_1);
                out.write(octetsObj);
                offsetCourant += octetsObj.length;
            }

            // Écriture de la table des références croisées (Xref)
            long debutXref = offsetCourant;
            out.write("xref\n".getBytes(StandardCharsets.ISO_8859_1));
            out.write(("0 " + (objets.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
            for (long offset : offsets) {
                String entree = String.format("%010d 00000 n \n", offset);
                out.write(entree.getBytes(StandardCharsets.ISO_8859_1));
            }

            // Écriture du pied de page PDF (Trailer et startxref)
            out.write("trailer\n".getBytes(StandardCharsets.ISO_8859_1));
            out.write(("<< /Size " + (objets.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write("startxref\n".getBytes(StandardCharsets.ISO_8859_1));
            out.write((debutXref + "\n").getBytes(StandardCharsets.ISO_8859_1));
            out.write("%%EOF\n".getBytes(StandardCharsets.ISO_8859_1));
            
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
}
