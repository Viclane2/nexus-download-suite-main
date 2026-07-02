# Notes de développement — Projet 10

## Choix techniques justifiés (pour la soutenance)

**Pourquoi un Thread par tâche plutôt qu'un pool ?**
Le cahier des charges impose explicitement le cycle de vie des threads
(`start`, `join`, états) comme thématique principale. Un thread dédié par
tâche illustre directement ce cycle de vie sans l'abstraire derrière un
pool, ce qui est plus pédagogique pour la démonstration.

**Pourquoi CopyOnWriteArrayList ?**
La liste des tâches est lue très souvent (affichage, statistiques) et
modifiée rarement (ajout/suppression d'une tâche). CopyOnWriteArrayList
est optimisée pour ce cas : aucune exception de concurrence possible,
même si plusieurs threads de téléchargement et le thread Swing y accèdent
en même temps.

**Pourquoi une méthode `synchronized` en plus de la collection thread-safe ?**
Le volume total téléchargé (`volumeTotalTelechargeMo`) est un cumul
(lecture-puis-écriture) modifié concurremment par plusieurs threads. Une
collection thread-safe ne protège pas ce type d'opération composite ; un
verrou explicite (`synchronized`) est donc nécessaire pour éviter la perte
d'incréments.

**Pourquoi SwingUtilities.invokeLater() partout dans le listener ?**
Swing n'est pas thread-safe : tout composant graphique modifié en dehors
de l'Event Dispatch Thread (EDT) peut provoquer un comportement
imprévisible. Toutes les notifications venant des threads de
téléchargement sont donc systématiquement redirigées vers l'EDT.

**Pourquoi join() dans un thread séparé pour le bouton "Attendre la fin de tout" ?**
join() est une méthode bloquante. L'appeler directement depuis l'EDT
gèlerait l'intégralité de l'interface jusqu'à la fin de tous les
téléchargements. Elle est donc encapsulée dans un thread d'attente dédié,
qui notifie l'EDT uniquement une fois l'attente terminée.

## Historique des décisions

- Choix de la sérialisation binaire (`.ser`) plutôt que JSON/texte pour
  l'historique principal : plus simple à mettre en œuvre avec les classes
  Java standard (`ObjectOutputStream`/`ObjectInputStream`), sans dépendance
  externe.
- Ajout d'un export CSV en complément, pour répondre au besoin de relecture
  humaine hors application.
- Ajout d'un bouton d'annulation par tâche (au-delà du strict minimum
  demandé) pour démontrer une gestion fine du cycle de vie des threads.
