package core;

/**
 * Interface d'écoute (pattern Observer) permettant à une tâche de
 * téléchargement de notifier l'IHM de son avancement, sans dépendre
 * directement de Swing.
 *
 * Toute classe qui implémente cette interface et touche à des composants
 * Swing DANS ses méthodes doit impérativement déléguer le traitement à
 * SwingUtilities.invokeLater(), car ces méthodes sont appelées depuis des
 * threads d'arrière-plan, jamais depuis l'Event Dispatch Thread (EDT).
 */
public interface ProgressionListener {

    /** Appelée à chaque mise à jour de la progression d'une tâche. */
    void onProgressionMiseAJour(TacheTelechargement tache);

    /** Appelée lorsqu'une tâche se termine (succès, erreur ou annulation). */
    void onTacheTerminee(TacheTelechargement tache);
}
