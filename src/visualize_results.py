"""
Visualization of VRP Solution for SAE2026
Uses Multidimensional Scaling (MDS) to reconstruct coordinates from the distance matrix.
"""

import numpy as np
import matplotlib.pyplot as plt
import networkx as nx
from sklearn.manifold import MDS
import warnings

# Suppress warnings
warnings.filterwarnings('ignore')

def visualize_solution():
    print("Génération de la visualisation...")

    # 1. Données du problème (sae.dat)
    # 0 = Dépôt, 1-10 = Cantines
    # Matrice des distances (copiée de sae.dat)
    distance_matrix = np.array([
        [0, 2.7, 4.6, 2.8, 3, 3.3, 3.1, 2.7, 5.1, 3.9, 4.7],
        [2.7, 0, 3.1, 0.8, 1.8, 2.5, 4.2, 1.4, 3.6, 2.5, 3],
        [4.6, 3.1, 0, 3.3, 4.4, 1.7, 6.8, 4.1, 1.3, 1.7, 1.4],
        [2.8, 0.8, 3.3, 0, 1.9, 2, 4, 1.5, 3.8, 2.8, 3.2],
        [3, 1.8, 4.4, 1.9, 0, 3.4, 2.6, 0.5, 4.7, 4.7, 4.1],
        [3.3, 2.5, 1.7, 2, 3.4, 0, 5.8, 3, 1.8, 0.5, 2.6],
        [3.1, 4.2, 6.8, 4, 2.6, 5.8, 0, 3, 7.4, 6.1, 7.6],
        [2.7, 1.4, 4.1, 1.5, 0.5, 3, 3, 0, 4.6, 3.7, 4.3],
        [5.1, 3.6, 1.3, 3.8, 4.7, 1.8, 7.4, 4.6, 0, 1.4, 2.8],
        [3.9, 2.5, 1.7, 2.8, 4.7, 0.5, 6.1, 3.7, 1.4, 0, 2.8],
        [4.7, 3, 1.4, 3.2, 4.1, 2.6, 7.6, 4.3, 2.8, 2.8, 0]
    ])

    # Demandes (pour la taille des noeuds)
    demands = [0, 60, 18, 26, 15, 44, 32, 20, 10, 27, 11] # 0 for depot

    # 2. Reconstitution des coordonnées via MDS (Multidimensional Scaling)
    # L'état aléatoire est fixé pour avoir toujours le même graphe
    mds = MDS(n_components=2, dissimilarity="precomputed", random_state=42, normalized_stress='auto')
    coords = mds.fit_transform(distance_matrix)

    # 3. Définition des tournées (Résultat optimal)
    # Vehicule 1: Depot -> C5 -> C9 -> C8 -> C2 -> Depot
    # Vehicule 3: Depot -> C3 -> C1 -> C10 -> Depot
    # Vehicule 4: Depot -> C6 -> C4 -> C7 -> Depot (Attention: C4->C7 seems long, verifying optimization)
    # Note: Indices python sont décalés de 1 par rapport aux données OPL si on map direct
    # OPL: 0=Depot, 1=C1...
    # Python: 0=Depot, 1=C1... (Match direct)

    routes = [
        {"id": 1, "path": [0, 5, 9, 8, 2, 0], "color": "red"},    # V1
        {"id": 3, "path": [0, 3, 1, 10, 0], "color": "blue"},     # V3
        {"id": 4, "path": [0, 6, 4, 7, 0], "color": "green"}      # V4
    ]

    # 4. Création du graphique
    plt.figure(figsize=(10, 8))
    
    # Tracer les arêtes (routes)
    for route in routes:
        path = route["path"]
        color = route["color"]
        # Tracer les lignes segment par segment
        for i in range(len(path) - 1):
            u, v = path[i], path[i+1]
            p1, p2 = coords[u], coords[v]
            
            # Flèche pour la direction
            plt.arrow(p1[0], p1[1], p2[0]-p1[0], p2[1]-p1[1], 
                      head_width=0.15, head_length=0.15, fc=color, ec=color, 
                      alpha=0.6, length_includes_head=True,
                      label=f'Véhicule {route["id"]}' if i == 0 else "")

    # Tracer les noeuds
    # Dépôt
    plt.scatter(coords[0, 0], coords[0, 1], c='black', s=200, marker='s', label='Dépôt', zorder=5)
    plt.text(coords[0, 0], coords[0, 1]+0.2, "Dépôt", ha='center', fontweight='bold')

    # Cantines
    for i in range(1, 11):
        # Taille proportionnelle à la demande
        size = demands[i] * 5 + 100
        plt.scatter(coords[i, 0], coords[i, 1], c='orange', s=size, edgecolors='black', zorder=5)
        plt.text(coords[i, 0], coords[i, 1], f"C{i}\n({demands[i]})", 
                 ha='center', va='center', fontsize=8, fontweight='bold')

    # Légende et titres
    plt.title(f"Optimisation des Tournées de Cantines\nDistance Totale: 31.3", fontsize=14)
    plt.xlabel("Coordonnée X (reconstituée)")
    plt.ylabel("Coordonnée Y (reconstituée)")
    plt.legend(loc='upper right')
    plt.grid(True, linestyle='--', alpha=0.5)

    # Sauvegarde
    output_path = "results/visualisation_tournees.png"
    plt.savefig(output_path, dpi=300)
    print(f"Visualisation sauvegardée dans : {output_path}")
    
    # Affichage (optionnel si exécuté localement)
    # plt.show()

if __name__ == "__main__":
    visualize_solution()
