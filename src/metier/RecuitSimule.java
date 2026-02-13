package metier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Recuit Simule pour le VRP.
 *
 * 1. Initialisation gloutonne (Plus Proche Voisin)
 * 2. Boucle du recuit : operateurs Swap et Relocate
 * 3. Refroidissement geometrique : T(k+1) = alpha * T(k)
 * 4. Recherche locale 2-opt finale
 */
public class RecuitSimule {

    private DonneesVRP donnees;
    private Random random = new Random();

    // --- Parametres ---
    private double temperatureInitiale = 200.0;
    private double temperatureFinale = 0.1;
    private double tauxRefroidissement = 0.9;
    private int iterationsParTemperature = 3000;
    private int maxIterationsSansAmelioration = 100000;
    private int frequenceAffichage = 50;

    // --- Listener (callback vers la vue) ---

    public interface RecuitListener {
        void onNouveauMeilleur(int iteration, double cout, double temperature, int nbVehicules);

        void onMiseAJour(int iteration, double temperature, double cout, int nbVehicules);

        void onSolutionMiseAJour(Solution solution);
    }

    private RecuitListener listener;

    // --- Constructeur ---

    public RecuitSimule(DonneesVRP donnees) {
        this.donnees = donnees;
    }

    public void setListener(RecuitListener l) {
        this.listener = l;
    }

    // --- Getters / Setters ---

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

    public int getFrequenceAffichage() {
        return frequenceAffichage;
    }

    public void setFrequenceAffichage(int f) {
        this.frequenceAffichage = f;
    }

    // ====================================================================
    // ALGORITHME PRINCIPAL
    // ====================================================================

    /**
     * Execute le recuit simule et retourne la meilleure solution trouvee.
     */
    public Solution executer() {

        // Verification : le nombre de vehicules est-il suffisant ?
        int sommeDemandes = 0;
        for (int d : donnees.getDemandes())
            sommeDemandes += d;
        int capaciteMax = donnees.getNbVehicules() * donnees.getCapaciteVehicule();
        if (sommeDemandes > capaciteMax) {
            throw new RuntimeException(
                    String.format("Nombre de vehicules insuffisant ! " +
                            "Demande totale = %d, capacite totale = %d (%d vehicules x %d). " +
                            "Il faut au minimum %d vehicules.",
                            sommeDemandes, capaciteMax,
                            donnees.getNbVehicules(), donnees.getCapaciteVehicule(),
                            (int) Math.ceil((double) sommeDemandes / donnees.getCapaciteVehicule())));
        }

        // Etape 1 — Solution initiale (Plus Proche Voisin)
        Solution courante = genererSolutionInitiale();
        System.out.printf("Solution initiale: %.2f%n", courante.cout);

        Solution meilleure = courante.copier();
        if (listener != null)
            listener.onSolutionMiseAJour(courante);

        // Etape 2 — Temperature initiale (valeur utilisateur)
        double temperature = temperatureInitiale;

        int iteration = 0;
        int sansAmelioration = 0;

        // Etape 3 — Boucle du recuit
        while (temperature > temperatureFinale
                && sansAmelioration < maxIterationsSansAmelioration) {

            for (int i = 0; i < iterationsParTemperature; i++) {
                iteration++;
                sansAmelioration++;

                Solution voisin = genererVoisin(courante);
                if (voisin == null)
                    continue;

                double delta = voisin.cout - courante.cout;

                // Critere de Metropolis
                if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
                    courante = voisin;

                    if (courante.cout < meilleure.cout) {
                        meilleure = courante.copier();
                        sansAmelioration = 0;
                        if (listener != null) {
                            listener.onNouveauMeilleur(iteration, meilleure.cout,
                                    temperature, compterVehicules(meilleure));
                            listener.onSolutionMiseAJour(meilleure);
                        }
                    }
                }

                // Rafraichir l'affichage
                if (iteration % frequenceAffichage == 0 && listener != null) {
                    listener.onMiseAJour(iteration, temperature,
                            meilleure.cout, compterVehicules(meilleure));
                    listener.onSolutionMiseAJour(meilleure);
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                }
            }

            // Refroidissement geometrique
            temperature *= tauxRefroidissement;
        }

        // Etape 4 — Recherche locale 2-opt finale
        meilleure = rechercheLocale2Opt(meilleure);

        System.out.printf("Resultat final: %.2f%n", meilleure.cout);
        if (listener != null) {
            listener.onNouveauMeilleur(iteration, meilleure.cout, 0, compterVehicules(meilleure));
            listener.onSolutionMiseAJour(meilleure);
        }
        return meilleure;
    }

    // ====================================================================
    // SOLUTION INITIALE — Plus Proche Voisin
    // ====================================================================

    /**
     * Construit une solution en visitant toujours le client non-visite
     * le plus proche dont la demande ne depasse pas la capacite restante.
     */
    private Solution genererSolutionInitiale() {
        int nbV = donnees.getNbVehicules();
        int nbC = donnees.getNbClients();
        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();

        Solution sol = new Solution(nbV);
        boolean[] visite = new boolean[nbC + 1];
        int affectes = 0, v = 0;

        while (affectes < nbC && v < nbV) {
            int charge = 0, dernier = depot;

            while (true) {
                int meilleur = -1;
                double minDist = Double.MAX_VALUE;
                for (int c = 1; c <= nbC; c++) {
                    if (!visite[c]
                            && charge + donnees.getDemandes()[c - 1] <= donnees.getCapaciteVehicule()
                            && dist[dernier][c] < minDist) {
                        minDist = dist[dernier][c];
                        meilleur = c;
                    }
                }
                if (meilleur == -1)
                    break;

                sol.tournees.get(v).add(meilleur);
                visite[meilleur] = true;
                charge += donnees.getDemandes()[meilleur - 1];
                dernier = meilleur;
                affectes++;
            }
            v++;
        }

        // Clients restants -> dernier vehicule
        for (int c = 1; c <= nbC; c++)
            if (!visite[c])
                sol.tournees.get(nbV - 1).add(c);

        sol.cout = calculerCout(sol);
        return sol;
    }

    // ====================================================================
    // GENERATION DE VOISIN
    // ====================================================================

    /**
     * Genere un voisin aleatoire (Swap 50% / Relocate 50%).
     * Retourne null si le mouvement viole la capacite.
     */
    private Solution genererVoisin(Solution solution) {
        Solution voisin = solution.copier();

        if (random.nextBoolean())
            operateurSwap(voisin);
        else
            operateurRelocate(voisin);

        if (!estValide(voisin))
            return null;
        voisin.cout = calculerCout(voisin);
        return voisin;
    }

    /**
     * Swap : echange 2 clients (intra ou inter-tournees).
     */
    private void operateurSwap(Solution sol) {
        List<Integer> nonVides = tourneesNonVides(sol);
        if (nonVides.isEmpty())
            return;

        if (nonVides.size() >= 2 && random.nextBoolean()) {
            // Swap inter-tournees
            int t1 = nonVides.get(random.nextInt(nonVides.size()));
            int t2;
            do {
                t2 = nonVides.get(random.nextInt(nonVides.size()));
            } while (t2 == t1);

            List<Integer> r1 = sol.tournees.get(t1), r2 = sol.tournees.get(t2);
            int p1 = random.nextInt(r1.size()), p2 = random.nextInt(r2.size());
            int tmp = r1.get(p1);
            r1.set(p1, r2.get(p2));
            r2.set(p2, tmp);
        } else {
            // Swap intra-tournee
            List<Integer> grands = tourneesAvecMin(sol, 2);
            if (grands.isEmpty())
                return;

            List<Integer> route = sol.tournees.get(grands.get(random.nextInt(grands.size())));
            int p1 = random.nextInt(route.size()), p2;
            do {
                p2 = random.nextInt(route.size());
            } while (p2 == p1);
            Collections.swap(route, p1, p2);
        }
    }

    /**
     * Relocate : retire un client d'une tournee et l'insere dans une autre.
     */
    private void operateurRelocate(Solution sol) {
        List<Integer> nonVides = tourneesNonVides(sol);
        if (nonVides.isEmpty())
            return;

        List<Integer> src = sol.tournees.get(nonVides.get(random.nextInt(nonVides.size())));
        int client = src.remove(random.nextInt(src.size()));

        List<Integer> dst = sol.tournees.get(random.nextInt(sol.tournees.size()));
        dst.add(dst.isEmpty() ? 0 : random.nextInt(dst.size() + 1), client);
    }

    // ====================================================================
    // RECHERCHE LOCALE 2-OPT
    // ====================================================================

    /**
     * 2-opt intra-tournee : inverse les segments pour eliminer les croisements.
     */
    private Solution rechercheLocale2Opt(Solution solution) {
        Solution res = solution.copier();
        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();

        for (List<Integer> route : res.tournees) {
            if (route.size() < 3)
                continue;
            boolean amelioration = true;
            while (amelioration) {
                amelioration = false;
                for (int i = 0; i < route.size() - 1; i++) {
                    for (int j = i + 1; j < route.size(); j++) {
                        int avI = (i == 0) ? depot : route.get(i - 1);
                        int apJ = (j == route.size() - 1) ? depot : route.get(j + 1);

                        double avant = dist[avI][route.get(i)] + dist[route.get(j)][apJ];
                        double apres = dist[avI][route.get(j)] + dist[route.get(i)][apJ];

                        if (apres < avant - 1e-10) {
                            int g = i, d = j;
                            while (g < d) {
                                Collections.swap(route, g++, d--);
                            }
                            amelioration = true;
                        }
                    }
                }
            }
        }
        res.cout = calculerCout(res);
        return res;
    }

    // ====================================================================
    // UTILITAIRES
    // ====================================================================

    private List<Integer> tourneesNonVides(Solution sol) {
        return tourneesAvecMin(sol, 1);
    }

    private List<Integer> tourneesAvecMin(Solution sol, int min) {
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < sol.tournees.size(); i++)
            if (sol.tournees.get(i).size() >= min)
                res.add(i);
        return res;
    }

    /** Verifie que chaque tournee respecte la capacite Qmax. */
    public boolean estValide(Solution sol) {
        for (List<Integer> route : sol.tournees) {
            int charge = 0;
            for (int c : route)
                charge += donnees.getDemandes()[c - 1];
            if (charge > donnees.getCapaciteVehicule())
                return false;
        }
        return true;
    }

    /** Calcule le cout total = somme des distances depot -> clients -> depot. */
    public double calculerCout(Solution sol) {
        double total = 0;
        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();
        for (List<Integer> route : sol.tournees) {
            if (route.isEmpty())
                continue;
            total += dist[depot][route.get(0)];
            for (int i = 0; i < route.size() - 1; i++)
                total += dist[route.get(i)][route.get(i + 1)];
            total += dist[route.get(route.size() - 1)][depot];
        }
        return total;
    }

    /** Compte les vehicules utilises (tournees non-vides). */
    public int compterVehicules(Solution sol) {
        int n = 0;
        for (List<Integer> route : sol.tournees)
            if (!route.isEmpty())
                n++;
        return n;
    }

    /** Affiche une solution dans la console. */
    public void afficherSolution(Solution sol) {
        System.out.printf("Cout total: %.2f%n", sol.cout);
        System.out.println("Vehicules: " + compterVehicules(sol));
        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();

        for (int i = 0; i < sol.tournees.size(); i++) {
            List<Integer> route = sol.tournees.get(i);
            if (route.isEmpty())
                continue;
            int charge = 0;
            for (int c : route)
                charge += donnees.getDemandes()[c - 1];
            double d = dist[depot][route.get(0)];
            for (int j = 0; j < route.size() - 1; j++)
                d += dist[route.get(j)][route.get(j + 1)];
            d += dist[route.get(route.size() - 1)][depot];
            System.out.printf("V%d: Depot", i + 1);
            for (int c : route)
                System.out.print(" -> C" + c);
            System.out.printf(" -> Depot (Dist: %.2f, Charge: %d/%d)%n",
                    d, charge, donnees.getCapaciteVehicule());
        }
    }
}
