package core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Centralise la liste de toutes les tâches de téléchargement (actives et
 * terminées) ainsi que les statistiques globales de l'application.
 *
 * Deux mécanismes de thread-safety sont utilisés volontairement, à des fins
 * pédagogiques et de robustesse :
 *
 * 1) CopyOnWriteArrayList pour la liste des tâches : adaptée à un usage où
 *    les lectures (affichage IHM, calcul de statistiques) sont beaucoup plus
 *    fréquentes que les écritures (ajout d'une tâche), ce qui évite les
 *    ConcurrentModificationException lorsque plusieurs threads de
 *    téléchargement et le thread Swing accèdent à la liste simultanément.
 *
 * 2) Une méthode explicitement "synchronized" pour l'accumulateur de volume
 *    total téléchargé : cette variable est une ressource partagée modifiée
 *    concurremment par plusieurs threads de téléchargement à la fin de leur
 *    exécution ; elle doit donc être protégée par un verrou explicite,
 *    exactement comme un solde de caisse centrale dans un système concurrent.
 */
public class GestionnaireTaches {

    private final List<TacheTelechargement> taches = new CopyOnWriteArrayList<>();
    private double volumeTotalTelechargeMo = 0.0;

    public void ajouter(TacheTelechargement tache) {
        taches.add(tache);
    }

    public void retirer(TacheTelechargement tache) {
        taches.remove(tache);
    }

    public List<TacheTelechargement> lister() {
        return taches; // CopyOnWriteArrayList : itération sûre même en concurrence
    }

    public int compterParStatut(StatutTache statut) {
        int compteur = 0;
        for (TacheTelechargement t : taches) {
            if (t.getStatut() == statut) {
                compteur++;
            }
        }
        return compteur;
    }

    /** Progression moyenne de toutes les tâches actuellement en cours. */
    public double progressionGlobale() {
        List<TacheTelechargement> enCours = taches.stream()
                .filter(t -> t.getStatut() == StatutTache.EN_COURS)
                .toList();
        if (enCours.isEmpty()) {
            return 0.0;
        }
        return enCours.stream().mapToDouble(TacheTelechargement::getProgression).average().orElse(0.0);
    }

    /**
     * Ressource partagée protégée : incrémentée de façon concurrente par
     * plusieurs threads de téléchargement lorsqu'une tâche se termine avec
     * succès. Le mot-clé "synchronized" garantit qu'un seul thread à la fois
     * peut modifier cette valeur, évitant toute perte d'incrément.
     */
    public synchronized void ajouterVolumeTelecharge(double mo) {
        this.volumeTotalTelechargeMo += mo;
    }

    public synchronized double getVolumeTotalTelechargeMo() {
        return volumeTotalTelechargeMo;
    }
}
