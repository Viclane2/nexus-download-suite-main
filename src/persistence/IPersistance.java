package persistence;

import core.TacheTelechargement;
import java.util.List;

/**
 * Contrat définissant les services de persistance des données de téléchargement.
 */
public interface IPersistance {
    /** Sauvegarde la liste des tâches dans le support de stockage. */
    boolean sauvegarder(List<TacheTelechargement> taches);

    /** Charge l'historique des tâches depuis le support de stockage. */
    List<TacheTelechargement> charger();

    /** Exporte les tâches au format CSV dans un fichier externe. */
    boolean exporterCsv(List<TacheTelechargement> taches, String cheminRapport);

    /** Exporte les tâches au format PDF dans un fichier externe. */
    boolean exporterPdf(List<TacheTelechargement> taches, String cheminRapport);

    /** Exporte les tâches au format HTML stylisé dans un fichier externe. */
    boolean exporterHtml(List<TacheTelechargement> taches, String cheminRapport);
}
