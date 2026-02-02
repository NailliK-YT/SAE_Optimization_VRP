/*********************************************
 * OPL 22.1.1.0 Model
 * Author: cm231805
 * Creation Date: 2 févr. 2026 at 09:32:30
 *********************************************/
// Parametres
int nbC = ...; // Nombre de cantines
range Cantines = 1..nbC;
range Sommets = 0..nbC; // 0 est le Hub/Depot [cite: 39, 40]

int nbV = ...; // Nombre de vehicules [cite: 41, 60]
range Vehicules = 1..nbV;

float Distance[Sommets][Sommets] = ...; // Matrice Dist_ij [cite: 42, 64]
int DemandesCantines[Cantines] = ...; // Quantite a livrer [cite: 43, 61]
int CapaMax = ...; // Qmax_v [cite: 44, 62]

// Variables de decision [cite: 45, 46, 48]
dvar boolean x[Sommets][Sommets][Vehicules]; 
dvar float+ u[Cantines]; // Pour l'elimination des sous-tours

// Objectif : Minimiser les durees (distances) des tournees [cite: 16, 56, 57]
minimize sum(v in Vehicules, i in Sommets, j in Sommets) Distance[i][j] * x[i][j][v];

subject to {
    // 1. Un client est visité exactement une fois par un seul véhicule [cite: 52, 54]
    forall(j in Cantines)
        sum(v in Vehicules, i in Sommets : i != j) x[i][j][v] == 1;

    // 2. Conservation du flot : depart et retour au depot [cite: 51]
    forall(v in Vehicules, p in Sommets)
        sum(i in Sommets : i != p) x[i][p][v] - sum(j in Sommets : j != p) x[p][j][v] == 0;

    // 3. Capacite du vehicule ne peut etre depassee [cite: 55]
    forall(v in Vehicules)
        sum(i in Cantines, j in Sommets : i != j) DemandesCantines[i] * x[i][j][v] <= CapaMax;

    // 4. Au moins un vehicule utilise [cite: 53]
    sum(v in Vehicules, j in Cantines) x[0][j][v] >= 1;

    // 5. Elimination des sous-tours (Formulation MTZ) [cite: 54]
    forall(v in Vehicules, i in Cantines, j in Cantines : i != j)
        u[i] - u[j] + CapaMax * x[i][j][v] <= CapaMax - DemandesCantines[j];
}

// Affichage des resultats au format demande [cite: 95, 96, 97]
execute {
    for(var v in Vehicules) {
        var start = 0;
        var hasRoute = false;
        for(var j in Cantines) if(x[0][j][v] == 1) hasRoute = true;
        
        if(hasRoute) {
            write("Vehicule " + v + ": Depot");
            var current = 0;
            var tourTerminated = false;
            while(!tourTerminated) {
                for(var nextNode in Sommets) {
                    if(x[current][nextNode][v] == 1) {
                        if(nextNode == 0) {
                            write(" → Depot");
                            tourTerminated = true;
                        } else {
                            write(" → C" + nextNode);
                            current = nextNode;
                        }
                        break;
                    }
                }
            }
            writeln();
        }
    }
}