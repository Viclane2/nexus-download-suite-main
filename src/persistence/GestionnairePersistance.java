package persistence;

import core.TacheTelechargement;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère la persistance de l'historique des téléchargements.
 *
 * Deux mécanismes sont proposés, conformément au cahier des charges :
 * - sauvegarde/chargement binaire via sérialisation d'objets Java (.ser)
 * - export d'un rapport texte structuré (.csv) pour relecture externe
 *
 * Toutes les opérations utilisent try-with-resources afin de garantir la
 * fermeture propre des flux, même en cas d'exception, et aucune erreur
 * d'entrée/sortie ne remonte sous une forme qui ferait planter l'IHM.
 */
public class GestionnairePersistance implements IPersistance {

    private final String cheminFichierHistorique;

    public GestionnairePersistance(String cheminFichierHistorique) {
        this.cheminFichierHistorique = cheminFichierHistorique;
    }

    /** Sauvegarde la liste complète des tâches (historique) dans le fichier binaire. */
    public boolean sauvegarder(List<TacheTelechargement> taches) {
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(cheminFichierHistorique))) {
            oos.writeObject(new ArrayList<>(taches));
            return true;
        } catch (IOException e) {
            System.err.println("[Persistance] Erreur lors de la sauvegarde : " + e.getMessage());
            return false;
        }
    }

    /**
     * Charge l'historique depuis le fichier binaire, s'il existe.
     * Retourne une liste vide si le fichier n'existe pas encore (premier
     * lancement) ou en cas d'erreur de lecture — l'application démarre
     * toujours, même avec un historique corrompu ou absent.
     */
    @SuppressWarnings("unchecked")
    public List<TacheTelechargement> charger() {
        File fichier = new File(cheminFichierHistorique);
        if (!fichier.exists()) {
            return new ArrayList<>();
        }
        try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(fichier))) {
            Object objet = ois.readObject();
            if (objet instanceof List<?>) {
                return (List<TacheTelechargement>) objet;
            }
            return new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[Persistance] Erreur lors du chargement : " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Exporte un rapport CSV (séparateur point-virgule) listant tous les
     * téléchargements avec leur statut final, utile pour relecture rapide
     * hors de l'application (Excel, LibreOffice Calc, etc.).
     */
    public boolean exporterCsv(List<TacheTelechargement> taches, String cheminRapport) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(cheminRapport))) {
            // Séparateur point-virgule pour Excel
            writer.println("Nom du fichier;URL;Taille (Mo);Statut;Progression (%);Vitesse (Mo/s);Début;Fin");
            for (TacheTelechargement t : taches) {
                writer.printf("%s;%s;%.1f;%s;%.1f;%.2f;%s;%s%n",
                        t.getNomFichier(),
                        t.getUrlSource(),
                        t.getTailleTotaleMo(),
                        t.getStatut().getLibelle(),
                        t.getProgression(),
                        t.getVitesseMoS(),
                        t.getHeureDebutFormatee(),
                        t.getHeureFinFormatee());
            }
            return true;
        } catch (IOException e) {
            System.err.println("[Persistance] Erreur lors de l'export CSV : " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean exporterPdf(List<TacheTelechargement> taches, String cheminRapport) {
        return PDFExporter.exporter(taches, cheminRapport);
    }

    @Override
    public boolean exporterHtml(List<TacheTelechargement> taches, String cheminRapport) {
        return HTMLExporter.exporter(taches, cheminRapport);
    }
}
