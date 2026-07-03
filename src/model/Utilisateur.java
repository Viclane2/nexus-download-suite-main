package model;

import java.time.LocalDateTime;

/**
 * Modèle représentant un utilisateur de l'application.
 */
public class Utilisateur {

    private final int           id;
    private final String        nomUtilisateur;
    private final LocalDateTime dateCreation;

    public Utilisateur(int id, String nomUtilisateur, LocalDateTime dateCreation) {
        this.id             = id;
        this.nomUtilisateur = nomUtilisateur;
        this.dateCreation   = dateCreation;
    }

    public int           getId()             { return id; }
    public String        getNomUtilisateur() { return nomUtilisateur; }
    public LocalDateTime getDateCreation()   { return dateCreation; }

    @Override
    public String toString() { return nomUtilisateur; }
}