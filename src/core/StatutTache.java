package core;

import java.io.Serializable;

/**
 * Représente les différents états possibles d'une tâche de téléchargement
 * tout au long de son cycle de vie.
 */
public enum StatutTache implements Serializable {
    EN_ATTENTE("En attente"),
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    ERREUR("Erreur"),
    ANNULE("Annulé");

    private final String libelle;

    StatutTache(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    @Override
    public String toString() {
        return libelle;
    }
}
