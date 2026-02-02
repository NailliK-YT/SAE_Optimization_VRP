# Rapport Technique - SAE2026
## Optimisation des Tournées de Véhicules (CVRP)

**Auteur:** Étudiant BUT3  
**Date:** 2 février 2026  
**Projet:** SAE2026 - Application d'optimisation et d'aide à la décision pour les tournées de véhicules

---

## Table des Matières

1. [Introduction](#1-introduction)
2. [Formulation Mathématique](#2-formulation-mathématique)
3. [Implémentation CPLEX - Cas des Cantines](#3-implémentation-cplex---cas-des-cantines)
4. [API pour tai75a](#4-api-pour-tai75a)
5. [Résultats et Analyse](#5-résultats-et-analyse)
6. [Conclusion](#6-conclusion)

---

## 1. Introduction

### 1.1 Contexte

Le problème de tournées de véhicules avec contraintes de capacité (CVRP - *Capacitated Vehicle Routing Problem*) est un problème classique d'optimisation combinatoire. Il consiste à déterminer les itinéraires optimaux pour une flotte de véhicules devant livrer des clients depuis un dépôt central, tout en respectant les contraintes de capacité des véhicules.

### 1.2 Objectifs

- Formuler mathématiquement le problème CVRP
- Implémenter et résoudre le modèle avec CPLEX pour le cas des 10 cantines
- Développer une API Python pour traiter le benchmark tai75a (75 clients)

---

## 2. Formulation Mathématique

### 2.1 Notations et Définitions

#### Ensembles

| Notation | Description |
|----------|-------------|
| $V = \{0, 1, ..., n\}$ | Ensemble des sommets (0 = dépôt, 1..n = clients) |
| $C = \{1, ..., n\}$ | Ensemble des clients (cantines) |
| $K = \{1, ..., m\}$ | Ensemble des véhicules disponibles |

#### Paramètres

| Notation | Description |
|----------|-------------|
| $n$ | Nombre de clients à visiter |
| $m$ | Nombre de véhicules disponibles |
| $d_{ij}$ | Distance entre le sommet $i$ et le sommet $j$ |
| $q_i$ | Demande (quantité à livrer) du client $i$ |
| $Q$ | Capacité maximale de chaque véhicule |

### 2.2 Variables de Décision

#### Variable principale : $x_{ij}^k$

$$x_{ij}^k = \begin{cases} 1 & \text{si le véhicule } k \text{ emprunte l'arc } (i,j) \\ 0 & \text{sinon} \end{cases}$$

Cette variable binaire indique si le véhicule $k$ se déplace directement du sommet $i$ au sommet $j$.

#### Variable auxiliaire : $u_i$ (élimination des sous-tours - MTZ)

$$u_i \in \mathbb{R}^+ \quad \forall i \in C$$

Cette variable représente la charge cumulée du véhicule après avoir visité le client $i$. Elle est utilisée dans les contraintes de Miller-Tucker-Zemlin pour éliminer les sous-tours.

### 2.3 Fonction Objectif

**Minimiser la distance totale parcourue :**

$$\min Z = \sum_{k \in K} \sum_{i \in V} \sum_{j \in V} d_{ij} \cdot x_{ij}^k$$

### 2.4 Contraintes

#### Contrainte 1 : Chaque client est visité exactement une fois

$$\sum_{k \in K} \sum_{i \in V, i \neq j} x_{ij}^k = 1 \quad \forall j \in C$$

> **Explication :** Chaque client (cantine) doit être visité par un et un seul véhicule. Cette contrainte garantit la couverture complète de tous les clients.

#### Contrainte 2 : Conservation du flot

$$\sum_{i \in V, i \neq p} x_{ip}^k - \sum_{j \in V, j \neq p} x_{pj}^k = 0 \quad \forall k \in K, \forall p \in V$$

> **Explication :** Pour chaque sommet et chaque véhicule, le nombre d'arcs entrants est égal au nombre d'arcs sortants. Cela garantit la continuité des tournées (pas de rupture dans le chemin).

#### Contrainte 3 : Respect de la capacité des véhicules

$$\sum_{i \in C} \sum_{j \in V, j \neq i} q_i \cdot x_{ij}^k \leq Q \quad \forall k \in K$$

> **Explication :** La somme des demandes des clients visités par un véhicule ne peut pas dépasser sa capacité maximale $Q$.

#### Contrainte 4 : Au moins un véhicule utilisé

$$\sum_{k \in K} \sum_{j \in C} x_{0j}^k \geq 1$$

> **Explication :** Au moins un véhicule doit quitter le dépôt pour effectuer une livraison.

#### Contrainte 5 : Élimination des sous-tours (MTZ)

$$u_i - u_j + Q \cdot x_{ij}^k \leq Q - q_j \quad \forall k \in K, \forall i \in C, \forall j \in C, i \neq j$$

> **Explication :** Cette contrainte de Miller-Tucker-Zemlin empêche la formation de sous-tours (circuits qui ne passent pas par le dépôt). Elle utilise les variables auxiliaires $u_i$ pour imposer un ordre de visite.

### 2.5 Modèle Mathématique Complet

$$
\begin{aligned}
\min \quad & Z = \sum_{k \in K} \sum_{i \in V} \sum_{j \in V} d_{ij} \cdot x_{ij}^k \\
\text{s.c.} \quad & \sum_{k \in K} \sum_{i \in V, i \neq j} x_{ij}^k = 1 & \forall j \in C \\
& \sum_{i \in V, i \neq p} x_{ip}^k - \sum_{j \in V, j \neq p} x_{pj}^k = 0 & \forall k \in K, \forall p \in V \\
& \sum_{i \in C} \sum_{j \in V, j \neq i} q_i \cdot x_{ij}^k \leq Q & \forall k \in K \\
& \sum_{k \in K} \sum_{j \in C} x_{0j}^k \geq 1 \\
& u_i - u_j + Q \cdot x_{ij}^k \leq Q - q_j & \forall k \in K, \forall i,j \in C, i \neq j \\
& x_{ij}^k \in \{0, 1\} & \forall i,j \in V, \forall k \in K \\
& u_i \geq 0 & \forall i \in C
\end{aligned}
$$

---

## 3. Implémentation CPLEX - Cas des Cantines

### 3.1 Données du Problème

| Paramètre | Valeur |
|-----------|--------|
| Nombre de cantines | 10 |
| Nombre de véhicules | 4 |
| Capacité maximale | 100 |
| Demandes | [60, 18, 26, 15, 44, 32, 20, 10, 27, 11] |

**Matrice des distances :** Voir fichier [sae.dat](file:///c:/Users/killi/Documents/SAE_Optimization_VRP/sae.dat)

### 3.2 Code du Modèle OPL

Le modèle complet est disponible dans : [sae.mod](file:///c:/Users/killi/Documents/SAE_Optimization_VRP/sae.mod)

```opl
// Paramètres
int nbC = ...;
range Cantines = 1..nbC;
range Sommets = 0..nbC;
int nbV = ...;
range Vehicules = 1..nbV;
float Distance[Sommets][Sommets] = ...;
int DemandesCantines[Cantines] = ...;
int CapaMax = ...;

// Variables de décision
dvar boolean x[Sommets][Sommets][Vehicules];
dvar float+ u[Cantines];

// Fonction objectif
minimize sum(v in Vehicules, i in Sommets, j in Sommets) 
         Distance[i][j] * x[i][j][v];
```

### 3.3 Résultats Attendus

Les résultats de l'optimisation seront documentés dans [cantines_results.txt](file:///c:/Users/killi/Documents/SAE_Optimization_VRP/results/cantines_results.txt) après exécution.

---

## 4. API pour tai75a

### 4.1 Description du Fichier tai75a

| Caractéristique | Valeur |
|-----------------|--------|
| Nombre de clients | 75 |
| Capacité véhicule | 1445 |
| Solution optimale connue | 1618.36 |
| Format | Coordonnées + Demandes |

### 4.2 Structure du Parser Python

Le parser Python est disponible dans : [vrp_parser.py](file:///c:/Users/killi/Documents/SAE_Optimization_VRP/src/vrp_parser.py)

**Fonctionnalités principales :**
- `parse_tai_file()` : Lecture du fichier tai75a.txt
- `calculate_distance_matrix()` : Calcul des distances euclidiennes
- `generate_cplex_dat()` : Génération du fichier .dat pour CPLEX

### 4.3 Utilisation

```bash
cd c:\Users\killi\Documents\SAE_Optimization_VRP
python src/vrp_parser.py
```

---

## 5. Résultats et Analyse

### 5.1 Cas des Cantines (10 clients)

L'optimisation avec CPLEX a permis de trouver la solution optimale suivante :

**Fonction Objectif (Distance totale) :** 31.3

**Détail des tournées :**
- **Véhicule 1 :** Dépôt → C5 → C9 → C8 → C2 → Dépôt
- **Véhicule 3 :** Dépôt → C3 → C1 → C10 → Dépôt
- **Véhicule 4 :** Dépôt → C6 → C4 → C7 → Dépôt

**Analyse :**
- 3 véhicules sont nécessaires sur les 4 disponibles.
- La demande est satisfaite pour les 10 cantines.
- La distance totale parcourue est minimisée à 31.3 unités.

### 5.2 Benchmark tai75a (75 clients)

En raison des limitations de la version Community Edition de CPLEX (limitée à 1000 variables/contraintes), la résolution du benchmark complet `tai75a` (75 clients) n'a pas pu être effectuée directement.

**Stratégie alternative :**
Une version tronquée `tai75a_small_15.dat` contenant les 15 premiers clients a été générée pour valider le modèle mathématique sur ce format de données spécifique.

### 5.3 Visualisation des Résultats (Cantines)

Une représentation graphique des tournées optimales pour le cas des 10 cantines a été générée :
- **Fichier :** `results/visualisation_tournees.html` (Interactif)
- **Méthode :** Reconstruction des coordonnées 2D à partir de la matrice des distances (MDS).

---

## 6. Conclusion

Ce projet a permis de :
1. Formaliser mathématiquement le problème CVRP.
2. Implémenter une solution exacte avec CPLEX.
3. Développer une chaîne de traitement complète (Parser Python, Générateur de données).
4. Fournir des outils de visualisation pour l'aide à la décision.

Les résultats obtenus sur le cas des cantines (Distance 31.3) démontrent la validité de l'approche. Pour le passage à l'échelle (75+ clients), l'utilisation d'une licence académique CPLEX ou d'une heuristique serait nécessaire.

---

## Annexes

### A. Fichiers du Projet

| Fichier | Description |
|---------|-------------|
| `sae.mod` | Modèle CPLEX/OPL |
| `sae.dat` | Données des 10 cantines |
| `tai75a.dat` | Données tai75a générées |
| `src/vrp_parser.py` | API Python d'extraction |
| `docs/rapport_technique.md` | Ce document |
