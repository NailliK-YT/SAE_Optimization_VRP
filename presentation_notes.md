# Présentation Modèle Mathématique VRP (Simplifié)

## 1. Le But (Fonction Objectif)
**"Payer le moins cher possible en kilomètres."**

Imaginez que chaque kilomètre parcouru coûte 1€.
L'algorithme additionne le coût de tous les trajets choisis. Son seul but est de trouver la combinaison qui donne la facture la plus basse à la fin.

---

## 2. Les Variables (Les Choix)
C'est comme un tableau géant d'interrupteurs.
Pour chaque route possible entre deux villes (A vers B, B vers A, A vers C...), l'ordinateur a un interrupteur :
*   **ON (1)** : Le camion passe par cette route.
*   **OFF (0)** : Le camion ne passe pas par là.

---

## 3. Les 4 Règles du Jeu (Contraintes)
Pour que la solution soit réaliste, l'algorithme doit respecter 4 règles simples :

1.  **Contrainte d'Affectation (Visite Unique)** :
    *   Chaque client doit recevoir 1 seule visite.
    *   *Analogie : Le livreur ne sonne pas deux fois.*

2.  **Conservation du Flot** :
    *   Si un camion entre dans une ville, il doit en sortir.
    *   *Analogie : Pas de camping chez le client. On entre, on livre, on repart.*

3.  **Contrainte de Capacité** :
    *   La marchandise totale ne doit pas dépasser la taille du camion.
    *   *Analogie : Si le coffre fait 100L, on ne peut pas mettre 110L de valises.*

4.  **Élimination des Sous-tours (MTZ)** :
    *   Tous les trajets doivent être reliés au dépôt central.
    *   *Analogie : Un camion ne peut pas apparaître magiquement pour livrer 3 villes dans un coin perdu sans être parti du dépôt principal.*
