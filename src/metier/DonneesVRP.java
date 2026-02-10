package metier;

/**
 * Donnees du probleme VRP (clients, distances, demandes, coordonnees)
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

    // Getters
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

    // Setters
    public void setNbClients(int nbClients) {
        this.nbClients = nbClients;
    }

    public void setNbVehicules(int nbVehicules) {
        this.nbVehicules = nbVehicules;
    }

    public void setCapaciteVehicule(int capaciteVehicule) {
        this.capaciteVehicule = capaciteVehicule;
    }

    public void setDistanceMatrix(double[][] distanceMatrix) {
        this.distanceMatrix = distanceMatrix;
    }

    public void setDemandes(int[] demandes) {
        this.demandes = demandes;
    }

    public void setXCoords(double[] xCoords) {
        this.xCoords = xCoords;
    }

    public void setYCoords(double[] yCoords) {
        this.yCoords = yCoords;
    }

    /**
     * Calcule la matrice des distances euclidiennes
     */
    public void calculerDistancesEuclidiennes() {
        int taille = nbClients + 1;
        distanceMatrix = new double[taille][taille];

        for (int i = 0; i < taille; i++) {
            for (int j = 0; j < taille; j++) {
                double dx = xCoords[i] - xCoords[j];
                double dy = yCoords[i] - yCoords[j];
                distanceMatrix[i][j] = Math.sqrt(dx * dx + dy * dy);
            }
        }
    }

    /**
     * Affiche les donnees chargees
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

        System.out.println("\nMatrice des distances (" + distanceMatrix.length + "x" + distanceMatrix[0].length + ")");
    }
}
