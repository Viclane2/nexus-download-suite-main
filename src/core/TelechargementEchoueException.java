package core;

/**
 * Exception métier personnalisée, levée lorsqu'une tâche de téléchargement
 * échoue (source simulée indisponible, fichier corrompu, etc.).
 *
 * Il s'agit d'une exception vérifiée (checked exception) : elle oblige
 * l'appelant à la traiter explicitement, ce qui garantit qu'aucun échec
 * de téléchargement ne sera ignoré silencieusement.
 */
public class TelechargementEchoueException extends Exception {

    public TelechargementEchoueException(String message) {
        super(message);
    }

    public TelechargementEchoueException(String message, Throwable cause) {
        super(message, cause);
    }
}
