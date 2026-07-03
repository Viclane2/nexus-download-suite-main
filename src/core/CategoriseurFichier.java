package core;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * Catégorise et déplace automatiquement les fichiers téléchargés
 * dans des sous-dossiers selon leur extension.
 *
 * Catégories :
 *   Images      → jpg, jpeg, png, gif, bmp, svg, webp, ico, tiff
 *   Vidéos      → mp4, avi, mkv, mov, wmv, flv, webm, m4v, mpeg, ts
 *   Audio       → mp3, wav, flac, aac, ogg, wma, m4a, opus
 *   Documents   → pdf, doc, docx, xls, xlsx, ppt, pptx, txt, odt, ods, csv, md
 *   Archives    → zip, rar, 7z, tar, gz, bz2, xz, tar.gz, tar.bz2
 *   Programmes  → exe, msi, deb, rpm, dmg, apk, sh, jar
 *   Autres      → tout le reste
 */
public class CategoriseurFichier {

    // ── Mapping extension → catégorie ────────────────────────────────────────
    private static final Map<String, String> CATEGORIES = Map.ofEntries(
            // Images
            Map.entry("jpg",  "Images"), Map.entry("jpeg", "Images"),
            Map.entry("png",  "Images"), Map.entry("gif",  "Images"),
            Map.entry("bmp",  "Images"), Map.entry("svg",  "Images"),
            Map.entry("webp", "Images"), Map.entry("ico",  "Images"),
            Map.entry("tiff", "Images"), Map.entry("tif",  "Images"),

            // Vidéos
            Map.entry("mp4",  "Vidéos"), Map.entry("avi",  "Vidéos"),
            Map.entry("mkv",  "Vidéos"), Map.entry("mov",  "Vidéos"),
            Map.entry("wmv",  "Vidéos"), Map.entry("flv",  "Vidéos"),
            Map.entry("webm", "Vidéos"), Map.entry("m4v",  "Vidéos"),
            Map.entry("mpeg", "Vidéos"), Map.entry("mpg",  "Vidéos"),
            Map.entry("ts",   "Vidéos"),

            // Audio
            Map.entry("mp3",  "Audio"),  Map.entry("wav",  "Audio"),
            Map.entry("flac", "Audio"),  Map.entry("aac",  "Audio"),
            Map.entry("ogg",  "Audio"),  Map.entry("wma",  "Audio"),
            Map.entry("m4a",  "Audio"),  Map.entry("opus", "Audio"),

            // Documents
            Map.entry("pdf",  "Documents"), Map.entry("doc",  "Documents"),
            Map.entry("docx", "Documents"), Map.entry("xls",  "Documents"),
            Map.entry("xlsx", "Documents"), Map.entry("ppt",  "Documents"),
            Map.entry("pptx", "Documents"), Map.entry("txt",  "Documents"),
            Map.entry("odt",  "Documents"), Map.entry("ods",  "Documents"),
            Map.entry("csv",  "Documents"), Map.entry("md",   "Documents"),
            Map.entry("rtf",  "Documents"),

            // Archives
            Map.entry("zip",  "Archives"), Map.entry("rar",  "Archives"),
            Map.entry("7z",   "Archives"), Map.entry("tar",  "Archives"),
            Map.entry("gz",   "Archives"), Map.entry("bz2",  "Archives"),
            Map.entry("xz",   "Archives"), Map.entry("tgz",  "Archives"),

            // Programmes
            Map.entry("exe",  "Programmes"), Map.entry("msi",  "Programmes"),
            Map.entry("deb",  "Programmes"), Map.entry("rpm",  "Programmes"),
            Map.entry("dmg",  "Programmes"), Map.entry("apk",  "Programmes"),
            Map.entry("sh",   "Programmes"), Map.entry("jar",  "Programmes"),
            Map.entry("bin",  "Programmes"), Map.entry("run",  "Programmes")
    );

    // ── API publique ─────────────────────────────────────────────────────────

    /**
     * Retourne la catégorie d'un fichier selon son extension.
     * @param nomFichier nom ou chemin du fichier
     * @return nom du sous-dossier (ex. "Images", "Vidéos", ..., "Autres")
     */
    public static String getCategorieFor(String nomFichier) {
        String ext = extraireExtension(nomFichier).toLowerCase();
        return CATEGORIES.getOrDefault(ext, "Autres");
    }

    /**
     * Déplace le fichier dans le sous-dossier approprié à l'intérieur du
     * dossier de destination, et retourne le nouveau chemin complet.
     *
     * Si le déplacement échoue, le fichier reste en place et l'ancien chemin est retourné.
     *
     * @param dossierBase dossier de téléchargement racine (ex. "telechargements")
     * @param nomFichier  nom du fichier à déplacer
     * @return chemin du dossier final où se trouve le fichier
     */
    public static String categoriserEtDeplacer(String dossierBase, String nomFichier) {
        String categorie   = getCategorieFor(nomFichier);
        File   dossierDest = new File(dossierBase, categorie);

        // Créer le sous-dossier si nécessaire
        if (!dossierDest.exists()) dossierDest.mkdirs();

        File source = new File(dossierBase, nomFichier);
        File dest   = new File(dossierDest, nomFichier);

        if (!source.exists()) {
            System.err.println("[CATÉGORIE] Fichier source introuvable : " + source.getAbsolutePath());
            return dossierBase;
        }

        // Éviter d'écraser un fichier existant → renommer si besoin
        if (dest.exists()) dest = eviterDoublon(dossierDest, nomFichier);

        boolean ok = source.renameTo(dest);
        if (ok) {
            System.out.println("[CATÉGORIE] " + nomFichier + " → " + categorie + "/");
            return dossierDest.getPath();
        } else {
            System.err.println("[CATÉGORIE] Déplacement impossible pour : " + nomFichier);
            return dossierBase;
        }
    }

    /**
     * Crée tous les sous-dossiers de catégories dans le dossier de base.
     * À appeler une seule fois au démarrage de l'appli.
     */
    public static void creerSousDossiers(String dossierBase) {
        Set<String> categories = Set.of("Images", "Vidéos", "Audio",
                "Documents", "Archives", "Programmes", "Autres");
        for (String cat : categories) {
            new File(dossierBase, cat).mkdirs();
        }
        System.out.println("[CATÉGORIE] Sous-dossiers créés dans : " + dossierBase);
    }

    // ── Utilitaires privés ───────────────────────────────────────────────────

    private static String extraireExtension(String nomFichier) {
        if (nomFichier == null) return "";
        int dot = nomFichier.lastIndexOf('.');
        if (dot < 0 || dot == nomFichier.length() - 1) return "";
        return nomFichier.substring(dot + 1);
    }

    /** Génère un nom non-conflictuel si le fichier existe déjà dans le dossier cible. */
    private static File eviterDoublon(File dossier, String nomFichier) {
        String ext  = extraireExtension(nomFichier);
        String base = ext.isEmpty() ? nomFichier
                : nomFichier.substring(0, nomFichier.length() - ext.length() - 1);
        int compteur = 1;
        File candidat;
        do {
            String nouveauNom = ext.isEmpty()
                    ? base + "_" + compteur
                    : base + "_" + compteur + "." + ext;
            candidat = new File(dossier, nouveauNom);
            compteur++;
        } while (candidat.exists());
        return candidat;
    }
}
