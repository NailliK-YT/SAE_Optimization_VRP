"""
VRP Parser - API pour l'extraction des données VRP
SAE2026 - Optimisation des Tournées de Véhicules

Ce module fournit des fonctions pour:
- Parser des fichiers VRP au format tai (tai75a.txt)
- Calculer les matrices de distances euclidiennes
- Générer des fichiers .dat compatibles CPLEX
"""

import math
from dataclasses import dataclass
from typing import List, Tuple
from pathlib import Path


@dataclass
class VRPInstance:
    """Représente une instance du problème VRP."""
    name: str
    num_customers: int
    num_vehicles: int
    capacity: int
    depot: Tuple[float, float]
    customers: List[dict]  # [{id, x, y, demand}, ...]
    distance_matrix: List[List[float]]
    best_known_solution: float = None


def parse_tai_file(filepath: str) -> VRPInstance:
    """
    Parse un fichier au format tai (tai75a.txt).
    
    Format attendu:
    - Ligne 1: nb_clients meilleure_solution
    - Ligne 2: capacité
    - Ligne 3: depot_x depot_y
    - Lignes suivantes: id x y demande
    
    Args:
        filepath: Chemin vers le fichier .txt
        
    Returns:
        VRPInstance: Instance VRP parsée
    """
    customers = []
    
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    # Ligne 1: nombre de clients et meilleure solution connue
    parts = lines[0].strip().split()
    num_customers = int(parts[0])
    best_known = float(parts[1]) if len(parts) > 1 else None
    
    # Ligne 2: capacité
    capacity = int(lines[1].strip())
    
    # Ligne 3: dépôt (coordonnées)
    depot_parts = lines[2].strip().split()
    depot = (float(depot_parts[0]), float(depot_parts[1]))
    
    # Lignes suivantes: clients (id x y demande)
    for i in range(3, 3 + num_customers):
        if i < len(lines):
            parts = lines[i].strip().split()
            if len(parts) >= 4:
                customers.append({
                    'id': int(parts[0]),
                    'x': float(parts[1]),
                    'y': float(parts[2]),
                    'demand': int(parts[3])
                })
    
    # Calculer le nombre de véhicules nécessaires (estimation)
    total_demand = sum(c['demand'] for c in customers)
    num_vehicles = math.ceil(total_demand / capacity)
    
    # Créer les coordonnées complètes (dépôt + clients)
    all_coords = [depot] + [(c['x'], c['y']) for c in customers]
    
    # Calculer la matrice des distances
    distance_matrix = calculate_distance_matrix(all_coords)
    
    return VRPInstance(
        name=Path(filepath).stem,
        num_customers=num_customers,
        num_vehicles=num_vehicles,
        capacity=capacity,
        depot=depot,
        customers=customers,
        distance_matrix=distance_matrix,
        best_known_solution=best_known
    )


def calculate_distance_matrix(coordinates: List[Tuple[float, float]]) -> List[List[float]]:
    """
    Calcule la matrice des distances euclidiennes.
    
    Args:
        coordinates: Liste de tuples (x, y) pour chaque nœud
        
    Returns:
        Matrice n×n des distances
    """
    n = len(coordinates)
    matrix = [[0.0] * n for _ in range(n)]
    
    for i in range(n):
        for j in range(n):
            if i != j:
                dx = coordinates[i][0] - coordinates[j][0]
                dy = coordinates[i][1] - coordinates[j][1]
                matrix[i][j] = round(math.sqrt(dx * dx + dy * dy), 3)
    
    return matrix


def generate_cplex_dat(instance: VRPInstance, output_path: str, num_vehicles: int = None):
    """
    Génère un fichier .dat compatible CPLEX/OPL.
    
    Args:
        instance: Instance VRP parsée
        output_path: Chemin du fichier de sortie
        num_vehicles: Nombre de véhicules (optionnel, utilise l'estimation si non fourni)
    """
    if num_vehicles is None:
        num_vehicles = instance.num_vehicles
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("/*********************************************\n")
        f.write(" * OPL 22.1.1.0 Data\n")
        f.write(f" * Instance: {instance.name}\n")
        f.write(f" * Clients: {instance.num_customers}\n")
        f.write(f" * Solution optimale connue: {instance.best_known_solution}\n")
        f.write(" *********************************************/\n")
        
        f.write(f"nbC = {instance.num_customers};\n")
        f.write(f"nbV = {num_vehicles};\n")
        f.write(f"CapaMax = {instance.capacity};\n")
        
        # Demandes des clients
        demands = [c['demand'] for c in instance.customers]
        f.write(f"DemandesCantines = {demands};\n")
        
        # Matrice des distances
        f.write("Distance = [\n")
        for i, row in enumerate(instance.distance_matrix):
            formatted_row = ", ".join(f"{d:.3f}" for d in row)
            if i < len(instance.distance_matrix) - 1:
                f.write(f"    [{formatted_row}],\n")
            else:
                f.write(f"    [{formatted_row}]\n")
        f.write("];\n")
    
    print(f"✓ Fichier CPLEX généré: {output_path}")


def print_instance_info(instance: VRPInstance):
    """Affiche les informations de l'instance."""
    print("=" * 50)
    print(f"Instance: {instance.name}")
    print("=" * 50)
    print(f"Nombre de clients: {instance.num_customers}")
    print(f"Nombre de véhicules estimé: {instance.num_vehicles}")
    print(f"Capacité véhicule: {instance.capacity}")
    print(f"Dépôt: {instance.depot}")
    print(f"Solution optimale connue: {instance.best_known_solution}")
    print(f"Demande totale: {sum(c['demand'] for c in instance.customers)}")
    print("=" * 50)


def main():
    """Point d'entrée principal."""
    import argparse
    
    parser = argparse.ArgumentParser(description='VRP Parser for SAE2026')
    parser.add_argument('--truncate', type=int, help='Nombre maximum de clients à garder (pour CPLEX Community)', default=None)
    args = parser.parse_args()
    
    # Chemins des fichiers
    project_root = Path(__file__).parent.parent
    tai_file = project_root / "tai75a.txt"
    
    print("\n" + "=" * 60)
    print("  VRP Parser - SAE2026")
    print("=" * 60 + "\n")
    
    if not tai_file.exists():
        print(f"❌ Erreur: Fichier non trouvé: {tai_file}")
        return
    
    # Parser le fichier
    print(f"📂 Lecture de: {tai_file}")
    instance = parse_tai_file(str(tai_file))
    
    # Troncature si demandée
    if args.truncate and args.truncate < instance.num_customers:
        print(f"⚠️  Troncature de l'instance à {args.truncate} clients (CPLEX Community Limit)")
        instance.customers = instance.customers[:args.truncate]
        instance.num_customers = args.truncate
        # Recalcul de la demande totale et véhicules pour le sous-ensemble
        total_demand = sum(c['demand'] for c in instance.customers)
        instance.num_vehicles = math.ceil(total_demand / instance.capacity)
        # Recalcul de la matrice des distances pour le sous-ensemble
        all_coords = [instance.depot] + [(c['x'], c['y']) for c in instance.customers]
        instance.distance_matrix = calculate_distance_matrix(all_coords)
        
        output_dat = project_root / f"tai75a_small_{args.truncate}.dat"
    else:
        output_dat = project_root / "tai75a.dat"
    
    # Afficher les informations
    print_instance_info(instance)
    
    # Générer le fichier CPLEX
    print(f"\n📝 Génération du fichier CPLEX...")
    generate_cplex_dat(instance, str(output_dat))
    
    print(f"\n✅ Terminé! Fichier généré: {output_dat}")


if __name__ == "__main__":
    main()
