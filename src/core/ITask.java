package core;

/**
 * Contrat définissant les fonctionnalités requises pour une tâche de téléchargement.
 * Assure le découplage entre les composants IHM et la logique d'exécution.
 */
public interface ITask {
    String getId();
    String getNomFichier();
    double getTailleTotaleMo();
    double getProgression();
    StatutTache getStatut();
    long getOctetsRecus();
    String getUrlSource();
    double getVitesseMoS();
    long getEtaSecondes();
    String getHeureDebutFormatee();
    String getHeureFinFormatee();
    void annuler();
    boolean estAnnulee();
}
