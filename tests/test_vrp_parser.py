"""
Tests pour le module VRP Parser
SAE2026 - Optimisation des Tournées de Véhicules
"""

import sys
import math
from pathlib import Path

# Ajouter le répertoire src au path
sys.path.insert(0, str(Path(__file__).parent.parent / "src"))

from vrp_parser import (
    parse_tai_file,
    calculate_distance_matrix,
    generate_cplex_dat,
    VRPInstance
)


def test_calculate_distance_matrix():
    """Test du calcul de la matrice des distances."""
    # Triangle simple: (0,0), (3,0), (0,4)
    coords = [(0, 0), (3, 0), (0, 4)]
    matrix = calculate_distance_matrix(coords)
    
    # Vérifications
    assert len(matrix) == 3, "La matrice doit avoir 3 lignes"
    assert len(matrix[0]) == 3, "La matrice doit avoir 3 colonnes"
    
    # Diagonale = 0
    for i in range(3):
        assert matrix[i][i] == 0, "Diagonale doit être 0"
    
    # Distance (0,0) -> (3,0) = 3
    assert abs(matrix[0][1] - 3.0) < 0.001, f"Distance 0->1 incorrecte: {matrix[0][1]}"
    
    # Distance (0,0) -> (0,4) = 4
    assert abs(matrix[0][2] - 4.0) < 0.001, f"Distance 0->2 incorrecte: {matrix[0][2]}"
    
    # Distance (3,0) -> (0,4) = 5 (triangle 3-4-5)
    assert abs(matrix[1][2] - 5.0) < 0.001, f"Distance 1->2 incorrecte: {matrix[1][2]}"
    
    # Symétrie
    for i in range(3):
        for j in range(3):
            assert abs(matrix[i][j] - matrix[j][i]) < 0.001, "La matrice doit être symétrique"
    
    print("✓ test_calculate_distance_matrix: OK")


def test_parse_tai_file():
    """Test du parsing du fichier tai75a.txt."""
    project_root = Path(__file__).parent.parent
    tai_file = project_root / "tai75a.txt"
    
    if not tai_file.exists():
        print("⚠ test_parse_tai_file: SKIPPED (fichier tai75a.txt non trouvé)")
        return
    
    instance = parse_tai_file(str(tai_file))
    
    # Vérifications basiques
    assert instance.num_customers == 75, f"Nombre de clients incorrect: {instance.num_customers}"
    assert instance.capacity == 1445, f"Capacité incorrecte: {instance.capacity}"
    assert instance.depot == (0, 0), f"Dépôt incorrect: {instance.depot}"
    assert len(instance.customers) == 75, f"Nombre de clients parsés incorrect: {len(instance.customers)}"
    
    # Vérifier la matrice des distances
    assert len(instance.distance_matrix) == 76, "Matrice doit avoir 76 lignes (dépôt + 75 clients)"
    assert len(instance.distance_matrix[0]) == 76, "Matrice doit avoir 76 colonnes"
    
    # Vérifier premier client
    first_customer = instance.customers[0]
    assert first_customer['id'] == 1
    assert first_customer['x'] == 35
    assert first_customer['y'] == -56
    assert first_customer['demand'] == 50
    
    print("✓ test_parse_tai_file: OK")


def test_vrp_instance_creation():
    """Test de la création d'une instance VRP."""
    instance = VRPInstance(
        name="test",
        num_customers=2,
        num_vehicles=1,
        capacity=100,
        depot=(0, 0),
        customers=[
            {'id': 1, 'x': 10, 'y': 0, 'demand': 30},
            {'id': 2, 'x': 0, 'y': 10, 'demand': 40}
        ],
        distance_matrix=[
            [0, 10, 10],
            [10, 0, 14.142],
            [10, 14.142, 0]
        ],
        best_known_solution=34.142
    )
    
    assert instance.name == "test"
    assert instance.num_customers == 2
    assert len(instance.customers) == 2
    
    print("✓ test_vrp_instance_creation: OK")


def run_all_tests():
    """Exécute tous les tests."""
    print("\n" + "=" * 50)
    print("  Tests VRP Parser")
    print("=" * 50 + "\n")
    
    test_calculate_distance_matrix()
    test_vrp_instance_creation()
    test_parse_tai_file()
    
    print("\n" + "=" * 50)
    print("  Tous les tests ont réussi! ✓")
    print("=" * 50 + "\n")


if __name__ == "__main__":
    run_all_tests()
