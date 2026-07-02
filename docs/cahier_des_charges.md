# Cahier des charges — Projet 10

## Gestionnaire de Téléchargement Parallèle de Fichiers Multimédia

**Module :** ICT308 — Université de Yaoundé I
**Groupe :** Groupe 10
**Durée :** Travaux pratiques examen

---

## 1. Contexte

Outil utilitaire permettant de lancer plusieurs téléchargements (ou copies)
simultanés de fichiers volumineux, avec possibilité de suivre la progression
individuelle de chaque tâche en temps réel dans une interface graphique.

## 2. Thématique principale

Cycle de vie des threads (`start`, `join`, états de threads), `SwingWorker`
ou threads UI, collections thread-safe.

## 3. Exigences fonctionnelles

### 3.1 Métier (Core)
- Classe `TâcheTelechargement` implémentant `Runnable`, possédant un nom,
  une taille totale et une progression courante.
- Encapsulation stricte de tous les attributs (`private`).
- Gestion d'un cycle de vie complet : `EN_ATTENTE` → `EN_COURS` →
  `TERMINE` / `ERREUR` / `ANNULE`.

### 3.2 Multithreading
- L'utilisateur clique sur un bouton pour ajouter une tâche.
- Chaque téléchargement tourne dans un thread indépendant.
- Utilisation appropriée de `Thread.sleep()` pour cadencer le débit simulé.
- Utilisation de `join()` lorsqu'une opération globale doit attendre la fin
  de toutes les sous-tâches.
- Utilisation d'une collection thread-safe pour stocker les tâches actives.

### 3.3 IHM
- Ajout dynamique de lignes dans l'IHM Swing à chaque nouvelle tâche.
- Chaque ligne contient le nom du fichier et une `JProgressBar` dédiée.
- Rafraîchissement sécurisé via `SwingUtilities.invokeLater()`.
- Interface professionnelle : en-tête, barre d'outils, panneau de
  statistiques globales, messages d'erreur clairs.

### 3.4 Persistance
- Sauvegarde et chargement de l'historique des tâches (sérialisation).
- Export d'un rapport structuré (CSV).

### 3.5 Robustesse
- Gestion d'une exception personnalisée (`TelechargementEchoueException`).
- Aucune saisie utilisateur invalide ne doit faire planter l'application.

## 4. Critères d'acceptation

| Critère | Statut |
|---|---|
| Plusieurs téléchargements simultanés sans gel de l'IHM | Validé |
| `join()` utilisé pour une attente collective | Validé |
| Collection thread-safe (CopyOnWriteArrayList) | Validé |
| Historique persistant entre les sessions | Validé |
| Export CSV fonctionnel | Validé |
| Gestion d'erreur sans plantage | Validé |
