import java.io.*;
import java.util.*;

/**
 * Implémentation du Recuit Simulé pour le Vehicle Routing Problem (VRP)
 * Compatible avec les fichiers .dat CPLEX
 */
public class RecuitSimuleVRP {

    private int nbClients;
    private int nbVehicules;
    private int capaciteVehicule;
    private double[][] distanceMatrix;
    private int[] demandes;
    private int depot = 0; // Le dépôt est toujours le nœud 0

    // Paramètres du recuit simulé
    private double temperatureInitiale = 1000.0;
    private double temperatureFinale = 0.1;
    private double tauxRefroidissement = 0.95;
    private int iterationsParTemperature = 100;

    private Random random = new Random();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java RecuitSimuleVRP <fichier.dat>");
            System.out.println("Exemple: java RecuitSimuleVRP cantines.dat");
            return;
        }

        RecuitSimuleVRP vrp = new RecuitSimuleVRP();
        try {
            vrp.lireFichierDat(args[0]);
            vrp.afficherDonnees();

            System.out.println("\n=== DÉBUT DU RECUIT SIMULÉ ===\n");
            Solution meilleuresolution = vrp.recuitSimule();

            System.out.println("\n=== SOLUTION OPTIMALE ===");
            vrp.afficherSolution(meilleuresolution);

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lit un fichier .dat au format CPLEX
     */
    public void lireFichierDat(String nomFichier) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(nomFichier));
        String ligne;

        while ((ligne = reader.readLine()) != null) {
            ligne = ligne.trim();

            // Ignorer les commentaires et lignes vides
            if (ligne.isEmpty() || ligne.startsWith("//")) {
                continue;
            }

            // Lire nbClients (format standard ou OPL)
            if (ligne.startsWith("nbClients") || ligne.startsWith("nbC")) {
                nbClients = extraireEntier(ligne);
            }
            // Lire nbVehicules (format standard ou OPL)
            else if (ligne.startsWith("nbVehicules") || ligne.startsWith("nbV")) {
                nbVehicules = extraireEntier(ligne);
            }
            // Lire capaciteVehicule (format standard ou OPL)
            else if (ligne.startsWith("capaciteVehicule") || ligne.startsWith("capacite")
                    || ligne.startsWith("CapaMax")) {
                capaciteVehicule = extraireEntier(ligne);
            }
            // Lire demandes (format standard ou OPL)
            else if (ligne.startsWith("demandes") || ligne.startsWith("DemandesCantines")) {
                demandes = extraireTableauEntiers(reader, ligne);
            }
            // Lire matrice de distances (format standard ou OPL)
            else if (ligne.startsWith("distances") || ligne.startsWith("Distance")) {
                distanceMatrix = extraireMatrice(reader, ligne);
            }
        }

        reader.close();

        // Validation
        if (nbClients == 0 || nbVehicules == 0 || capaciteVehicule == 0) {
            throw new IOException("Données incomplètes dans le fichier .dat");
        }
    }

    /**
     * Extrait un entier d'une ligne "nom = valeur;"
     */
    private int extraireEntier(String ligne) {
        String[] parts = ligne.split("=");
        if (parts.length < 2)
            return 0;
        String valeur = parts[1].trim().replace(";", "").trim();
        return Integer.parseInt(valeur);
    }

    /**
     * Extrait un tableau d'entiers
     */
    private int[] extraireTableauEntiers(BufferedReader reader, String premiereLigne) throws IOException {
        StringBuilder sb = new StringBuilder();
        String ligne = premiereLigne;

        // Lire jusqu'au point-virgule de fin
        while (!ligne.contains("];")) {
            sb.append(ligne).append(" ");
            ligne = reader.readLine().trim();
        }
        sb.append(ligne);

        // Extraire les valeurs entre crochets
        String contenu = sb.toString();
        int debut = contenu.indexOf('[');
        int fin = contenu.indexOf(']');
        String valeurs = contenu.substring(debut + 1, fin).trim();

        String[] tokens = valeurs.split("[,\\s]+");
        int[] tableau = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            tableau[i] = Integer.parseInt(tokens[i].trim());
        }

        return tableau;
    }

    /**
     * Extrait une matrice de doubles
     */
    private double[][] extraireMatrice(BufferedReader reader, String premiereLigne) throws IOException {
        List<double[]> lignes = new ArrayList<>();
        String ligne = premiereLigne;

        // Lire jusqu'au point-virgule de fin
        while (!ligne.contains("];")) {
            ligne = reader.readLine().trim();
            if (ligne.isEmpty() || ligne.startsWith("//"))
                continue;

            if (ligne.startsWith("[") || ligne.contains(",")) {
                // Extraire les valeurs de cette ligne
                String valeurs = ligne.replace("[", "").replace("]", "").replace(",", " ").trim();
                if (!valeurs.isEmpty() && !valeurs.equals("];")) {
                    String[] tokens = valeurs.split("\\s+");
                    double[] ligneMat = new double[tokens.length];
                    for (int i = 0; i < tokens.length; i++) {
                        ligneMat[i] = Double.parseDouble(tokens[i]);
                    }
                    lignes.add(ligneMat);
                }
            }
        }

        // Convertir en tableau 2D
        double[][] matrice = new double[lignes.size()][];
        for (int i = 0; i < lignes.size(); i++) {
            matrice[i] = lignes.get(i);
        }

        return matrice;
    }

    /**
     * Affiche les données chargées
     */
    public void afficherDonnees() {
        System.out.println("=== DONNÉES DU PROBLÈME ===");
        System.out.println("Nombre de clients: " + nbClients);
        System.out.println("Nombre de véhicules: " + nbVehicules);
        System.out.println("Capacité par véhicule: " + capaciteVehicule);

        System.out.print("Demandes des clients: [");
        for (int i = 0; i < demandes.length; i++) {
            System.out.print(demandes[i]);
            if (i < demandes.length - 1)
                System.out.print(", ");
        }
        System.out.println("]");

        System.out.println("\nMatrice des distances (" + distanceMatrix.length + "x" + distanceMatrix[0].length + ")");
    }

    /**
     * Algorithme principal du recuit simulé
     */
    public Solution recuitSimule() {
        // Générer une solution initiale aléatoire
        Solution solutionCourante = genererSolutionInitiale();
        Solution meilleuresolution = solutionCourante.copier();

        double temperature = temperatureInitiale;
        int iteration = 0;

        System.out.printf("Solution initiale - Coût: %.2f\n", solutionCourante.cout);

        while (temperature > temperatureFinale) {
            for (int i = 0; i < iterationsParTemperature; i++) {
                iteration++;

                // Générer une solution voisine
                Solution solutionVoisine = genererVoisin(solutionCourante);

                if (solutionVoisine != null) {
                    double delta = solutionVoisine.cout - solutionCourante.cout;

                    // Acceptation de la solution
                    if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
                        solutionCourante = solutionVoisine;

                        // Mise à jour de la meilleure solution
                        if (solutionCourante.cout < meilleuresolution.cout) {
                            meilleuresolution = solutionCourante.copier();
                            System.out.printf("Itération %d - Nouveau meilleur: %.2f (T=%.2f)\n",
                                    iteration, meilleuresolution.cout, temperature);
                        }
                    }
                }
            }

            // Refroidissement
            temperature *= tauxRefroidissement;
        }

        return meilleuresolution;
    }

    /**
     * Génère une solution initiale aléatoire valide
     */
    private Solution genererSolutionInitiale() {
        Solution solution = new Solution(nbVehicules);
        List<Integer> clientsRestants = new ArrayList<>();

        // Tous les clients (1 à nbClients, 0 est le dépôt)
        for (int i = 1; i <= nbClients; i++) {
            clientsRestants.add(i);
        }

        Collections.shuffle(clientsRestants, random);

        int vehiculeCourant = 0;
        int chargeActuelle = 0;

        for (int client : clientsRestants) {
            int demande = demandes[client - 1]; // demandes[0] = demande du client 1

            // Si la demande dépasse la capacité restante, changer de véhicule
            if (chargeActuelle + demande > capaciteVehicule) {
                vehiculeCourant++;
                if (vehiculeCourant >= nbVehicules) {
                    // Tous les véhicules sont pleins, forcer l'ajout (solution non valide)
                    vehiculeCourant = nbVehicules - 1;
                }
                chargeActuelle = 0;
            }

            solution.tournees.get(vehiculeCourant).add(client);
            chargeActuelle += demande;
        }

        solution.cout = calculerCout(solution);
        return solution;
    }

    /**
     * Génère une solution voisine en échangeant deux clients
     */
    private Solution genererVoisin(Solution solution) {
        Solution voisin = solution.copier();

        // Choisir le type de mouvement aléatoirement
        int typeMovement = random.nextInt(3);

        switch (typeMovement) {
            case 0: // Swap dans la même tournée
                swapDansTournee(voisin);
                break;
            case 1: // Swap entre deux tournées
                swapEntreTournees(voisin);
                break;
            case 2: // Déplacer un client vers une autre tournée
                deplacerClient(voisin);
                break;
        }

        // Vérifier la validité et calculer le coût
        if (estValide(voisin)) {
            voisin.cout = calculerCout(voisin);
            return voisin;
        }

        return null;
    }

    /**
     * Échange deux clients dans la même tournée
     */
    private void swapDansTournee(Solution solution) {
        List<Integer> tourneesNonVides = new ArrayList<>();
        for (int i = 0; i < solution.tournees.size(); i++) {
            if (solution.tournees.get(i).size() > 1) {
                tourneesNonVides.add(i);
            }
        }

        if (tourneesNonVides.isEmpty())
            return;

        int tourneeIdx = tourneesNonVides.get(random.nextInt(tourneesNonVides.size()));
        List<Integer> tournee = solution.tournees.get(tourneeIdx);

        int pos1 = random.nextInt(tournee.size());
        int pos2 = random.nextInt(tournee.size());

        Collections.swap(tournee, pos1, pos2);
    }

    /**
     * Échange deux clients entre deux tournées différentes
     */
    private void swapEntreTournees(Solution solution) {
        List<Integer> tourneesNonVides = new ArrayList<>();
        for (int i = 0; i < solution.tournees.size(); i++) {
            if (!solution.tournees.get(i).isEmpty()) {
                tourneesNonVides.add(i);
            }
        }

        if (tourneesNonVides.size() < 2)
            return;

        int tournee1Idx = tourneesNonVides.get(random.nextInt(tourneesNonVides.size()));
        int tournee2Idx;
        do {
            tournee2Idx = tourneesNonVides.get(random.nextInt(tourneesNonVides.size()));
        } while (tournee2Idx == tournee1Idx);

        List<Integer> tournee1 = solution.tournees.get(tournee1Idx);
        List<Integer> tournee2 = solution.tournees.get(tournee2Idx);

        int pos1 = random.nextInt(tournee1.size());
        int pos2 = random.nextInt(tournee2.size());

        int temp = tournee1.get(pos1);
        tournee1.set(pos1, tournee2.get(pos2));
        tournee2.set(pos2, temp);
    }

    /**
     * Déplace un client d'une tournée à une autre
     */
    private void deplacerClient(Solution solution) {
        List<Integer> tourneesNonVides = new ArrayList<>();
        for (int i = 0; i < solution.tournees.size(); i++) {
            if (!solution.tournees.get(i).isEmpty()) {
                tourneesNonVides.add(i);
            }
        }

        if (tourneesNonVides.isEmpty())
            return;

        int tourneeSourceIdx = tourneesNonVides.get(random.nextInt(tourneesNonVides.size()));
        int tourneeDestIdx = random.nextInt(solution.tournees.size());

        List<Integer> tourneeSource = solution.tournees.get(tourneeSourceIdx);
        List<Integer> tourneeDest = solution.tournees.get(tourneeDestIdx);

        if (tourneeSource.isEmpty())
            return;

        int posSource = random.nextInt(tourneeSource.size());
        int client = tourneeSource.remove(posSource);

        int posDest = tourneeDest.isEmpty() ? 0 : random.nextInt(tourneeDest.size() + 1);
        tourneeDest.add(posDest, client);
    }

    /**
     * Vérifie si une solution respecte les contraintes de capacité
     */
    private boolean estValide(Solution solution) {
        for (List<Integer> tournee : solution.tournees) {
            int charge = 0;
            for (int client : tournee) {
                charge += demandes[client - 1];
            }
            if (charge > capaciteVehicule) {
                return false;
            }
        }
        return true;
    }

    /**
     * Calcule le coût total d'une solution (somme des distances)
     */
    private double calculerCout(Solution solution) {
        double coutTotal = 0.0;

        for (List<Integer> tournee : solution.tournees) {
            if (tournee.isEmpty())
                continue;

            // Distance dépôt -> premier client
            coutTotal += distanceMatrix[depot][tournee.get(0)];

            // Distances entre clients
            for (int i = 0; i < tournee.size() - 1; i++) {
                coutTotal += distanceMatrix[tournee.get(i)][tournee.get(i + 1)];
            }

            // Distance dernier client -> dépôt
            coutTotal += distanceMatrix[tournee.get(tournee.size() - 1)][depot];
        }

        return coutTotal;
    }

    /**
     * Affiche une solution de manière lisible
     */
    public void afficherSolution(Solution solution) {
        System.out.println("Coût total: " + String.format("%.2f", solution.cout));
        System.out.println("Nombre de véhicules utilisés: " + compterVehiculesUtilises(solution));
        System.out.println("\nDétail des tournées:");

        for (int i = 0; i < solution.tournees.size(); i++) {
            List<Integer> tournee = solution.tournees.get(i);
            if (tournee.isEmpty())
                continue;

            int charge = 0;
            for (int client : tournee) {
                charge += demandes[client - 1];
            }

            double distanceTournee = 0.0;
            distanceTournee += distanceMatrix[depot][tournee.get(0)];
            for (int j = 0; j < tournee.size() - 1; j++) {
                distanceTournee += distanceMatrix[tournee.get(j)][tournee.get(j + 1)];
            }
            distanceTournee += distanceMatrix[tournee.get(tournee.size() - 1)][depot];

            System.out.print("Véhicule " + (i + 1) + ": Dépôt");
            for (int client : tournee) {
                System.out.print(" -> C" + client);
            }
            System.out.printf(" -> Dépôt (Distance: %.2f, Charge: %d/%d)\n",
                    distanceTournee, charge, capaciteVehicule);
        }
    }

    /**
     * Compte le nombre de véhicules réellement utilisés
     */
    private int compterVehiculesUtilises(Solution solution) {
        int count = 0;
        for (List<Integer> tournee : solution.tournees) {
            if (!tournee.isEmpty())
                count++;
        }
        return count;
    }

    /**
     * Classe interne représentant une solution
     */
    class Solution {
        List<List<Integer>> tournees;
        double cout;

        Solution(int nbVehicules) {
            tournees = new ArrayList<>();
            for (int i = 0; i < nbVehicules; i++) {
                tournees.add(new ArrayList<>());
            }
            cout = Double.MAX_VALUE;
        }

        Solution copier() {
            Solution copie = new Solution(tournees.size());
            for (int i = 0; i < tournees.size(); i++) {
                copie.tournees.set(i, new ArrayList<>(tournees.get(i)));
            }
            copie.cout = this.cout;
            return copie;
        }
    }
}