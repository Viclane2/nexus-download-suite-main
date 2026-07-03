package auth;

import model.Utilisateur;

/**
 * Singleton qui stocke l'utilisateur actuellement connecté.
 * Accessible depuis n'importe quelle classe de l'application.
 */
public class SessionManager {

    private static SessionManager instance;
    private Utilisateur utilisateurCourant;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void connecter(Utilisateur u)  { this.utilisateurCourant = u; }
    public void deconnecter()             { this.utilisateurCourant = null; }
    public Utilisateur getUtilisateur()   { return utilisateurCourant; }
    public boolean estConnecte()          { return utilisateurCourant != null; }

    /** Raccourci pour obtenir l'id (-1 si non connecté). */
    public int getUtilisateurId() {
        return utilisateurCourant != null ? utilisateurCourant.getId() : -1;
    }
}