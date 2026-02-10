package metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Algorithme du recuit simule pour le VRP
 */
public class RecuitSimule {

    private DonneesVRP donnees;
    private Random random = new Random();

    // Parametres du recuit simule
    private double temperatureInitiale = 1000.0;
    private double temperatureFinale = 0.1;
    private double tauxRefroidissement = 0.95;
    private int iterationsParTemperature = 100;
    private int maxIterationsSansAmelioration = 3000;

    // Interface de callback pour notifier la vue
    public interface RecuitListener {
        void onNouveauMeilleur(int iteration, double cout, double temperature, int nbVehicules);

        void onMiseAJour(int iteration, double temperature, double cout, int nbVehicules);

        void onSolutionMiseAJour(Solution solution);
    }

    private RecuitListener listener;

    public RecuitSimule(DonneesVRP donnees) {
        this.donnees = donnees;
    }

    public void setListener(RecuitListener listener) {
        this.listener = listener;
    }

    // Getters / Setters pour les parametres
    public double getTemperatureInitiale() {
        return temperatureInitiale;
    }

    public void setTemperatureInitiale(double t) {
        this.temperatureInitiale = t;
    }

    public double getTemperatureFinale() {
        return temperatureFinale;
    }

    public void setTemperatureFinale(double t) {
        this.temperatureFinale = t;
    }

    public double getTauxRefroidissement() {
        return tauxRefroidissement;
    }

    public void setTauxRefroidissement(double t) {
        this.tauxRefroidissement = t;
    }

    public int getIterationsParTemperature() {
        return iterationsParTemperature;
    }

    public void setIterationsParTemperature(int i) {
        this.iterationsParTemperature = i;
    }

    public int getMaxIterationsSansAmelioration() {
        return maxIterationsSansAmelioration;
    }

    public void setMaxIterationsSansAmelioration(int i) {
        this.maxIterationsSansAmelioration = i;
    }

    /**
     * Algorithme principal du recuit simule
     */
    public Solution executer() {
        Solution solutionCourante = genererSolutionInitiale();
        Solution meilleuresolution = solutionCourante.copier();

        if (listener != null)
            listener.onSolutionMiseAJour(solutionCourante);

        double temperature = temperatureInitiale;
        int iteration = 0;
        int iterationsSansAmelioration = 0;

        System.out.printf("Solution initiale - Cout: %.2f%n", solutionCourante.cout);

        while (temperature > temperatureFinale) {
            for (int i = 0; i < iterationsParTemperature; i++) {
                iteration++;

                Solution solutionVoisine = genererVoisin(solutionCourante);

                if (solutionVoisine != null) {
                    double delta = solutionVoisine.cout - solutionCourante.cout;

                    if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
                        solutionCourante = solutionVoisine;

                        if (solutionCourante.cout < meilleuresolution.cout) {
                            meilleuresolution = solutionCourante.copier();

                            iterationsSansAmelioration = 0;

                            System.out.printf("Iteration %d - Nouveau meilleur: %.2f (T=%.2f)%n",
                                    iteration, meilleuresolution.cout, temperature);

                            if (listener != null) {
                                listener.onNouveauMeilleur(iteration, meilleuresolution.cout,
                                        temperature, compterVehiculesUtilises(meilleuresolution));
                                listener.onSolutionMiseAJour(meilleuresolution);
                            }
                        } else {
                            iterationsSansAmelioration++;
                        }
                    }
                }

                // Mise a jour periodique de la visualisation
                if (iteration % 50 == 0 && listener != null) {
                    listener.onMiseAJour(iteration, temperature,
                            meilleuresolution.cout, compterVehiculesUtilises(meilleuresolution));
                    listener.onSolutionMiseAJour(meilleuresolution);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                }
            }

            if (iterationsSansAmelioration > maxIterationsSansAmelioration) {
                System.out.println("Arret anticipe : Pas d'amelioration depuis "
                        + iterationsSansAmelioration + " iterations.");
                break;
            }

            temperature *= tauxRefroidissement;
        }

        if (listener != null)
            listener.onSolutionMiseAJour(meilleuresolution);

        return meilleuresolution;
    }

    /**
     * Genere une solution initiale aleatoire valide
     */
    private Solution genererSolutionInitiale() {
        int nbVehicules = donnees.getNbVehicules();
        Solution solution = new Solution(nbVehicules);
        List<Integer> clientsRestants = new ArrayList<>();

        for (int i = 1; i <= donnees.getNbClients(); i++) {
            clientsRestants.add(i);
        }

        Collections.shuffle(clientsRestants, random);

        int vehiculeCourant = 0;
        int chargeActuelle = 0;

        for (int client : clientsRestants) {
            int demande = donnees.getDemandes()[client - 1];

            if (chargeActuelle + demande > donnees.getCapaciteVehicule()) {
                vehiculeCourant++;
                if (vehiculeCourant >= nbVehicules) {
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
     * Genere une solution voisine
     */
    private Solution genererVoisin(Solution solution) {
        Solution voisin = solution.copier();

        int typeMovement = random.nextInt(3);
        switch (typeMovement) {
            case 0:
                swapDansTournee(voisin);
                break;
            case 1:
                swapEntreTournees(voisin);
                break;
            case 2:
                deplacerClient(voisin);
                break;
        }

        if (estValide(voisin)) {
            voisin.cout = calculerCout(voisin);
            return voisin;
        }
        return null;
    }

    private void swapDansTournee(Solution solution) {
        List<Integer> tourneesNonVides = new ArrayList<>();
        for (int i = 0; i < solution.tournees.size(); i++) {
            if (solution.tournees.get(i).size() > 1)
                tourneesNonVides.add(i);
        }
        if (tourneesNonVides.isEmpty())
            return;

        int tourneeIdx = tourneesNonVides.get(random.nextInt(tourneesNonVides.size()));
        List<Integer> tournee = solution.tournees.get(tourneeIdx);
        int pos1 = random.nextInt(tournee.size());
        int pos2 = random.nextInt(tournee.size());
        Collections.swap(tournee, pos1, pos2);
    }

    private void swapEntreTournees(Solution solution) {
        List<Integer> tourneesNonVides = new ArrayList<>();
        for (int i = 0; i < solution.tournees.size(); i++) {
            if (!solution.tournees.get(i).isEmpty())
                tourneesNonVides.add(i);
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

    private void deplacerClient(Solution solution) {
        List<Integer> tourneesNonVides = new ArrayList<>();
        for (int i = 0; i < solution.tournees.size(); i++) {
            if (!solution.tournees.get(i).isEmpty())
                tourneesNonVides.add(i);
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
     * Verifie si une solution respecte les contraintes de capacite
     */
    public boolean estValide(Solution solution) {
        for (List<Integer> tournee : solution.tournees) {
            int charge = 0;
            for (int client : tournee) {
                charge += donnees.getDemandes()[client - 1];
            }
            if (charge > donnees.getCapaciteVehicule())
                return false;
        }
        return true;
    }

    /**
     * Calcule le cout total d'une solution (somme des distances)
     */
    public double calculerCout(Solution solution) {
        double coutTotal = 0.0;
        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();

        for (List<Integer> tournee : solution.tournees) {
            if (tournee.isEmpty())
                continue;
            coutTotal += dist[depot][tournee.get(0)];
            for (int i = 0; i < tournee.size() - 1; i++) {
                coutTotal += dist[tournee.get(i)][tournee.get(i + 1)];
            }
            coutTotal += dist[tournee.get(tournee.size() - 1)][depot];
        }
        return coutTotal;
    }

    /**
     * Compte le nombre de vehicules utilises
     */
    public int compterVehiculesUtilises(Solution solution) {
        int count = 0;
        for (List<Integer> tournee : solution.tournees) {
            if (!tournee.isEmpty())
                count++;
        }
        return count;
    }

    /**
     * Affiche une solution de maniere lisible
     */
    public void afficherSolution(Solution solution) {
        System.out.println("Cout total: " + String.format("%.2f", solution.cout));
        System.out.println("Nombre de vehicules utilises: " + compterVehiculesUtilises(solution));
        System.out.println("\nDetail des tournees:");

        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();

        for (int i = 0; i < solution.tournees.size(); i++) {
            List<Integer> tournee = solution.tournees.get(i);
            if (tournee.isEmpty())
                continue;

            int charge = 0;
            for (int client : tournee) {
                charge += donnees.getDemandes()[client - 1];
            }

            double distanceTournee = 0.0;
            distanceTournee += dist[depot][tournee.get(0)];
            for (int j = 0; j < tournee.size() - 1; j++) {
                distanceTournee += dist[tournee.get(j)][tournee.get(j + 1)];
            }
            distanceTournee += dist[tournee.get(tournee.size() - 1)][depot];

            System.out.print("Vehicule " + (i + 1) + ": Depot");
            for (int client : tournee) {
                System.out.print(" -> C" + client);
            }
            System.out.printf(" -> Depot (Distance: %.2f, Charge: %d/%d)%n",
                    distanceTournee, charge, donnees.getCapaciteVehicule());
        }
    }
}
