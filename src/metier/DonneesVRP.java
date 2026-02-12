package metier;

/**
 * Donnees du probleme VRP : clients, vehicules, distances, demandes.
 */
public class DonneesVRP {

    private int nbClients;
    private int nbVehicules;
    private int capaciteVehicule;
    private double[][] distanceMatrix;
    private int[] demandes;
    private int depot = 0;
    private double[] xCoords;
    private double[] yCoords;

    // --- Getters ---

    public int getNbClients() {
        return nbClients;
    }

    public int getNbVehicules() {
        return nbVehicules;
    }

    public int getCapaciteVehicule() {
        return capaciteVehicule;
    }

    public double[][] getDistanceMatrix() {
        return distanceMatrix;
    }

    public int[] getDemandes() {
        return demandes;
    }

    public int getDepot() {
        return depot;
    }

    public double[] getXCoords() {
        return xCoords;
    }

    public double[] getYCoords() {
        return yCoords;
    }

    // --- Setters ---

    public void setNbClients(int n) {
        this.nbClients = n;
    }

    public void setNbVehicules(int n) {
        this.nbVehicules = n;
    }

    public void setCapaciteVehicule(int c) {
        this.capaciteVehicule = c;
    }

    public void setDistanceMatrix(double[][] m) {
        this.distanceMatrix = m;
    }

    public void setDemandes(int[] d) {
        this.demandes = d;
    }

    public void setXCoords(double[] x) {
        this.xCoords = x;
    }

    public void setYCoords(double[] y) {
        this.yCoords = y;
    }

    /**
     * Calcule la matrice des distances euclidiennes a partir des coordonnees.
     */
    public void calculerDistancesEuclidiennes() {
        int n = nbClients + 1;
        distanceMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double dx = xCoords[i] - xCoords[j];
                double dy = yCoords[i] - yCoords[j];
                distanceMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
            }
        }
    }

    /**
     * Affiche un resume des donnees dans la console.
     */
    public void afficherDonnees() {
        System.out.println("=== DONNEES DU PROBLEME ===");
        System.out.println("Nombre de clients: " + nbClients);
        System.out.println("Nombre de vehicules: " + nbVehicules);
        System.out.println("Capacite par vehicule: " + capaciteVehicule);

        System.out.print("Demandes des clients: [");
        for (int i = 0; i < demandes.length; i++) {
            System.out.print(demandes[i]);
            if (i < demandes.length - 1)
                System.out.print(", ");
        }
        System.out.println("]");

        System.out.println("\nMatrice des distances (" +
                distanceMatrix.length + "x" + distanceMatrix[0].length + ")");
    }
}
