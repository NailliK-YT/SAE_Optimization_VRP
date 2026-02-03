import math

def generate_opl_dat(file_path, output_path):
    print(f"Lecture du fichier {file_path}...")
    with open(file_path, 'r') as f:
        # Lire tout le contenu et filtrer les lignes vides
        lines = [line.strip() for line in f.readlines() if line.strip()]

    # 1. Parsing du Header
    # Ligne 1: "75 1618.36" -> nbClients et BestKnownCost (on ignore le coût pour le .dat)
    first_line_parts = lines[0].split()
    nb_clients = int(first_line_parts[0])
    
    # Ligne 2: "1445" -> Capacité Max
    capa_max = int(lines[1])

    nodes = []    # Liste de tuples (x, y)
    demandes = [] # Liste des demandes

    # 2. Parsing du Dépôt
    # Ligne 3: "0 0" -> Coordonnées du dépôt
    # Le dépôt est le premier noeud (indice 0 dans la matrice Distance, mais pas dans DemandesCantines)
    depot_parts = lines[2].split()
    nodes.append((float(depot_parts[0]), float(depot_parts[1])))
    # Le dépôt n'a pas de demande (0), on ne l'ajoute pas à la liste DemandesCantines pour OPL

    # 3. Parsing des Clients
    # Les lignes suivantes sont sous la forme : ID X Y Demand
    # On attend 'nb_clients' lignes
    start_line_clients = 3
    for i in range(start_line_clients, start_line_clients + nb_clients):
        parts = lines[i].split()
        # parts[0] = ID, parts[1] = X, parts[2] = Y, parts[3] = Demand
        x = float(parts[1])
        y = float(parts[2])
        demand = int(parts[3])
        
        nodes.append((x, y))
        demandes.append(demand)

    print(f" - {nb_clients} clients trouvés.")
    print(f" - Capacité Max: {capa_max}")
    print(f" - Nombre total de noeuds (Dépôt inclus): {len(nodes)}")

    # 4. Calcul de la Matrice des Distances (Euclidienne)
    dist_matrix = []
    for i in range(len(nodes)):
        row = []
        for j in range(len(nodes)):
            # Distance Euclidienne : sqrt((x1-x2)^2 + (y1-y2)^2)
            d = math.sqrt((nodes[i][0] - nodes[j][0])**2 + (nodes[i][1] - nodes[j][1])**2)
            row.append(round(d, 2))
        dist_matrix.append(row)

    # 5. Écriture du fichier .dat au format OPL
    print(f"Génération du fichier {output_path}...")
    with open(output_path, 'w') as out:
        out.write("/*********************************************\n")
        out.write(" * Fichier de donnees genere automatiquement\n")
        out.write(f" * Source: {file_path}\n")
        out.write(" *********************************************/\n\n")
        
        out.write(f"nbC = {nb_clients};\n")
        # Estimation simple : total demande / capacité (arrondi sup)
        total_demand = sum(demandes)
        min_vehicles = math.ceil(total_demand / capa_max)
        # On met une marge, par exemple +20% ou +5 véhicules pour la flexibilité
        out.write(f"nbV = {min_vehicles + 5};\n") 
        out.write(f"CapaMax = {capa_max};\n")
        
        out.write(f"DemandesCantines = {demandes};\n")
        
        out.write("Distance = [\n")
        for row in dist_matrix:
            out.write(f"  {row},\n")
        out.write("];\n")

    print("Terminé.")

# Exécution du script
if __name__ == "__main__":
    generate_opl_dat('tai75a.txt', 'tai75a.dat')