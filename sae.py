import math

def generate_opl_dat(file_path, output_path):
    with open(file_path, 'r') as f:
        lines = f.readlines()

    # Lecture nbClients et Qmax
    first_line = lines[0].split()
    nb_clients = int(first_line[0])
    q_max = int(lines[1].strip())

    nodes = []
    demandes = []
    
    # Extraction des coordonnées et demandes (75 clients + 1 dépôt = 76 lignes)
    for i in range(2, 2 + nb_clients + 1):
        parts = lines[i].split()
        # Format: ID, X, Y, Demande
        nodes.append((float(parts[1]), float(parts[2])))
        if i > 2: # Ne pas ajouter la demande du dépôt (toujours 0)
            demandes.append(int(parts[3]))

    # Calcul de la matrice des distances (Euclidienne)
    dist_matrix = []
    for i in range(len(nodes)):
        row = []
        for j in range(len(nodes)):
            d = math.sqrt((nodes[i][0] - nodes[j][0])**2 + (nodes[i][1] - nodes[j][1])**2)
            row.append(round(d, 2))
        dist_matrix.append(row)

    # Écriture du fichier .dat pour CPLEX
    with open(output_path, 'w') as out:
        out.write(f"nbC = {nb_clients};\n")
        out.write(f"nbV = 15;\n") # Nombre de véhicules estimé pour 75 clients
        out.write(f"CapaMax = {q_max};\n")
        out.write(f"DemandesCantines = {demandes};\n")
        out.write("Distance = [\n")
        for row in dist_matrix:
            out.write(f"  {row},\n")
        out.write("];\n")

    print(f"Fichier {output_path} généré avec succès.")

# Utilisation
generate_opl_dat('tai75a.txt', 'sae.dat')