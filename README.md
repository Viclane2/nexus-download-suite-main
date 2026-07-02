# Projet 10 — Gestionnaire de Téléchargement Parallèle de Fichiers Multimédia

ICT308 — Travaux Pratiques Examen — Université de Yaoundé I — Groupe 10

## Aperçu

Application Swing professionnelle simulant le téléchargement parallèle de
plusieurs fichiers, avec suivi en temps réel de la progression de chaque
tâche, panneau de statistiques globales, persistance de l'historique, export
CSV, annulation par tâche, et démonstration explicite du cycle de vie des
threads (`start`, `join`, états).

## Structure du projet

```
projet10-download-manager/
├── src/
│   ├── Main.java            → point d'entrée de l'application
│   ├── core/                → Équipe 1 : modèle métier
│   │   ├── TacheTelechargement.java
│   │   ├── GestionnaireTaches.java
│   │   ├── StatutTache.java
│   │   ├── ProgressionListener.java
│   │   └── TelechargementEchoueException.java
│   ├── persistence/         → Équipe 2 : sérialisation et export
│   │   └── GestionnairePersistance.java
│   ├── ui/                  → Équipe 3 : interface graphique Swing
│   │   ├── MainFrame.java
│   │   ├── LigneTachePanel.java
│   │   ├── StatistiquesPanel.java
│   │   ├── HeaderPanel.java
│   │   └── Theme.java
│   └── threading/           → Équipe 4 : gestion des threads
│       └── MoteurTelechargement.java
├── data/                    → fichiers de sauvegarde/historique générés
├── docs/                    → cahier des charges, notes de développement
├── pom.xml
├── .gitignore
└── README.md
```

## Compilation et exécution

### Avec Maven
```bash
mvn clean package
java -jar target/download-manager.jar
```

### Sans Maven (javac direct)
```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
java -cp out Main
```

## Fonctionnement

1. **Ajouter une tâche** : saisir un nom de fichier et une taille (Mo). Une
   carte apparaît immédiatement dans la liste, avec sa propre barre de
   progression.
2. **Suivi en temps réel** : chaque tâche s'exécute dans un thread
   indépendant, cadencé par `Thread.sleep()`, et notifie l'IHM via un
   listener (`ProgressionListener`) systématiquement traité sur l'Event
   Dispatch Thread via `SwingUtilities.invokeLater()`.
3. **Attendre la fin de tout (join)** : ce bouton illustre l'utilisation de
   `Thread.join()` pour attendre la terminaison de tous les threads actifs,
   sans geler l'interface (l'attente s'exécute elle-même dans un thread
   séparé).
4. **Annulation** : chaque tâche peut être annulée individuellement via son
   bouton "Annuler".
5. **Gestion d'erreur** : une proportion des tâches échoue aléatoirement
   pour démontrer la gestion de l'exception personnalisée
   `TelechargementEchoueException`, sans jamais faire planter l'application.
6. **Statistiques globales** : panneau en pied de fenêtre (mélange
   `BorderLayout` structurel + `GridLayout` pour les indicateurs) affichant
   le nombre de tâches par statut et le volume total téléchargé.
7. **Persistance** : à la fermeture, l'historique est sauvegardé
   (sérialisation `.ser`) et rechargé automatiquement au démarrage suivant.
8. **Export CSV** : génère un rapport structuré, lisible dans Excel ou
   LibreOffice Calc.

## Points techniques clés (pour la soutenance)

- **Thread-safety à deux niveaux** : `CopyOnWriteArrayList` pour la
  collection de tâches (beaucoup de lectures, peu d'écritures), et une
  méthode explicitement `synchronized` (`ajouterVolumeTelecharge`) pour
  protéger l'accumulateur partagé de volume total — un cumul concurrent
  qu'une simple collection thread-safe ne suffit pas à protéger.
- **Réactivité de l'IHM** : aucune opération bloquante (téléchargement,
  attente `join()`) n'est jamais exécutée directement sur l'EDT.
- **Cycle de vie des threads** : démarrage explicite (`start()`), attente
  collective (`join()`), et états observables via `isAlive()`.
- **Robustesse** : toute erreur (saisie utilisateur, échec simulé,
  fichier inaccessible) est interceptée et affichée proprement, sans jamais
  interrompre l'exécution globale de l'application.

Voir `docs/notes_developpement.md` pour la justification détaillée de
chaque choix technique, utile en cas de questions du jury.
