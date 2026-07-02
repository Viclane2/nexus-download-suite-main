package threading;

import core.GestionnaireTaches;
import core.TacheTelechargement;
import core.StatutTache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pilote l'exécution multithread et la file d'attente.
 * Ordonnance le démarrage des threads de téléchargement en respectant la limite
 * maximale paramétrée de tâches parallèles.
 */
public class MoteurTelechargement {

    private final GestionnaireTaches gestionnaireTaches;
    private final List<Thread> threadsActifs = new CopyOnWriteArrayList<>();
    private volatile int maxTelechargementsSimultanes = 3; // Limite par défaut
    private volatile boolean limiteurVitesseActif = false;
    private volatile int limiteVitesseKoS = 500; // en Ko/s

    public MoteurTelechargement(GestionnaireTaches gestionnaireTaches) {
        this.gestionnaireTaches = gestionnaireTaches;
    }

    public void setLimiteurVitesseActif(boolean actif) {
        this.limiteurVitesseActif = actif;
    }

    public boolean isLimiteurVitesseActif() {
        return limiteurVitesseActif;
    }

    public void setLimiteVitesseKoS(int limite) {
        this.limiteVitesseKoS = limite;
    }

    public int getLimiteVitesseKoS() {
        return limiteVitesseKoS;
    }

    public void setMaxTelechargementsSimultanes(int max) {
        this.maxTelechargementsSimultanes = max;
        ordonnancer();
    }

    public int getMaxTelechargementsSimultanes() {
        return maxTelechargementsSimultanes;
    }

    /**
     * Enregistre une tâche. Elle démarrera immédiatement si la limite n'est pas atteinte,
     * sinon elle reste dans l'état EN_ATTENTE.
     */
    public void demarrerTache(TacheTelechargement tache) {
        tache.setGestionnaire(gestionnaireTaches);
        gestionnaireTaches.ajouter(tache);
        ordonnancer();
    }

    /**
     * Parcourt la liste des tâches et démarre les tâches en attente si le nombre
     * de téléchargements actifs est inférieur au maximum configuré.
     */
    public synchronized void ordonnancer() {
        // Nettoyer les références de threads morts pour éviter les fuites de mémoire
        threadsActifs.removeIf(t -> !t.isAlive());

        // Compter les tâches actuellement en cours
        int nbEnCours = 0;
        for (TacheTelechargement t : gestionnaireTaches.lister()) {
            if (t.getStatut() == StatutTache.EN_COURS) {
                nbEnCours++;
            }
        }

        // Si on a de la place, démarrer les tâches en attente
        if (nbEnCours < maxTelechargementsSimultanes) {
            for (TacheTelechargement t : gestionnaireTaches.lister()) {
                if (t.getStatut() == StatutTache.EN_ATTENTE) {
                    
                    // S'assurer qu'un thread n'est pas déjà actif pour celle-ci
                    boolean dejaLance = false;
                    for (Thread th : threadsActifs) {
                        if (th.getName().equals("Thread-" + t.getNomFichier())) {
                            dejaLance = true;
                            break;
                        }
                    }

                    if (!dejaLance) {
                        Thread thread = new Thread(t, "Thread-" + t.getNomFichier());
                        thread.setDaemon(true);
                        threadsActifs.add(thread);
                        thread.start(); // NEW -> RUNNABLE

                        nbEnCours++;
                        if (nbEnCours >= maxTelechargementsSimultanes) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Attend la fin de tous les threads actifs via join().
     */
    public void attendreToutesLesTaches() {
        List<Thread> snapshot = new ArrayList<>(threadsActifs);
        for (Thread t : snapshot) {
            try {
                if (t.isAlive()) {
                    t.join(); // attend la terminaison effective
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /** Annule toutes les tâches en cours. */
    public void annulerToutesLesTaches() {
        for (TacheTelechargement t : gestionnaireTaches.lister()) {
            t.annuler();
        }
    }

    public int nombreThreadsActifs() {
        int compteur = 0;
        for (Thread t : threadsActifs) {
            if (t.isAlive()) {
                compteur++;
            }
        }
        return compteur;
    }
}

