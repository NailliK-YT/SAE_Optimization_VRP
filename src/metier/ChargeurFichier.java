package metier;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chargement des fichiers de donnees VRP (.txt et .dat).
 */
public class ChargeurFichier {

    /**
     * Lit un fichier .txt au format Taillard.
     * Format attendu : nbClients, capacite, depot(x,y), puis lignes id x y demande.
     */
    public static DonneesVRP lireFichierTxt(String nomFichier) throws IOException {
        DonneesVRP donnees = new DonneesVRP();
        BufferedReader reader = new BufferedReader(new FileReader(nomFichier));

        // Ligne 1 : nombre de clients
        String ligne = lireLigneNonVide(reader);
        if (ligne == null)
            throw new IOException("Fichier vide");
        donnees.setNbClients(Integer.parseInt(ligne.trim().split("\\s+")[0]));

        // Ligne 2 : capacite vehicule
        ligne = lireLigneNonVide(reader);
        donnees.setCapaciteVehicule(Integer.parseInt(ligne.trim()));

        // Ligne 3 : coordonnees du depot
        ligne = lireLigneNonVide(reader);
        String[] parts = ligne.trim().split("\\s+");
        double depotX = Double.parseDouble(parts[0]);
        double depotY = Double.parseDouble(parts[1]);

        int nbClients = donnees.getNbClients();
        int[] demandes = new int[nbClients];
        double[] xCoords = new double[nbClients + 1];
        double[] yCoords = new double[nbClients + 1];
        xCoords[0] = depotX;
        yCoords[0] = depotY;

        // Lignes suivantes : id x y demande
        for (int i = 0; i < nbClients; i++) {
            ligne = lireLigneNonVide(reader);
            parts = ligne.trim().split("\\s+");
            int id = Integer.parseInt(parts[0]);
            xCoords[id] = Double.parseDouble(parts[1]);
            yCoords[id] = Double.parseDouble(parts[2]);
            demandes[id - 1] = Integer.parseInt(parts[3]);
        }
        reader.close();

        donnees.setDemandes(demandes);
        donnees.setXCoords(xCoords);
        donnees.setYCoords(yCoords);
        donnees.calculerDistancesEuclidiennes();

        // Estimation du nombre de vehicules
        int somme = Arrays.stream(demandes).sum();
        int nbVehicules = (int) (1.5 * somme / donnees.getCapaciteVehicule()) + 2;
        if (nbVehicules < 5)
            nbVehicules = 10;

        donnees.setNbVehicules(nbVehicules);

        if (donnees.getNbClients() == 0 || donnees.getCapaciteVehicule() == 0)
            throw new IOException("Donnees incompletes dans le fichier .txt");

        return donnees;
    }

    /**
     * Lit un fichier .dat au format CPLEX.
     */
    public static DonneesVRP lireFichierDat(String nomFichier) throws IOException {
        DonneesVRP donnees = new DonneesVRP();
        BufferedReader reader = new BufferedReader(new FileReader(nomFichier));
        String ligne;

        while ((ligne = reader.readLine()) != null) {
            ligne = ligne.trim();
            if (ligne.isEmpty() || ligne.startsWith("//"))
                continue;

            if (ligne.startsWith("nbClients") || ligne.startsWith("nbC"))
                donnees.setNbClients(extraireEntier(ligne));
            else if (ligne.startsWith("nbVehicules") || ligne.startsWith("nbV"))
                donnees.setNbVehicules(extraireEntier(ligne));
            else if (ligne.startsWith("capaciteVehicule") || ligne.startsWith("capacite")
                    || ligne.startsWith("CapaMax"))
                donnees.setCapaciteVehicule(extraireEntier(ligne));
            else if (ligne.startsWith("demandes") || ligne.startsWith("DemandesCantines"))
                donnees.setDemandes(extraireTableauEntiers(reader, ligne));
            else if (ligne.startsWith("distances") || ligne.startsWith("Distance"))
                donnees.setDistanceMatrix(extraireMatrice(reader, ligne));
        }
        reader.close();

        if (donnees.getNbClients() == 0 || donnees.getNbVehicules() == 0
                || donnees.getCapaciteVehicule() == 0)
            throw new IOException("Donnees incompletes dans le fichier .dat");

        return donnees;
    }

    // --- Utilitaires de parsing ---

    private static String lireLigneNonVide(BufferedReader reader) throws IOException {
        String ligne;
        while ((ligne = reader.readLine()) != null)
            if (!ligne.trim().isEmpty())
                return ligne;
        return null;
    }

    private static int extraireEntier(String ligne) {
        String[] parts = ligne.split("=");
        if (parts.length < 2)
            return 0;
        return Integer.parseInt(parts[1].trim().replace(";", "").trim());
    }

    private static int[] extraireTableauEntiers(BufferedReader reader, String premiereLigne)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        String ligne = premiereLigne;
        while (!ligne.contains("];")) {
            sb.append(ligne).append(" ");
            ligne = reader.readLine().trim();
        }
        sb.append(ligne);

        String contenu = sb.toString();
        String valeurs = contenu.substring(contenu.indexOf('[') + 1, contenu.indexOf(']')).trim();
        String[] tokens = valeurs.split("[,\\s]+");

        int[] tableau = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++)
            tableau[i] = Integer.parseInt(tokens[i].trim());
        return tableau;
    }

    private static double[][] extraireMatrice(BufferedReader reader, String premiereLigne)
            throws IOException {
        List<double[]> lignes = new ArrayList<>();
        String ligne = premiereLigne;
        while (!ligne.contains("];")) {
            ligne = reader.readLine().trim();
            if (ligne.isEmpty() || ligne.startsWith("//"))
                continue;

            if (ligne.startsWith("[") || ligne.contains(",")) {
                String valeurs = ligne.replace("[", "").replace("]", "")
                        .replace(",", " ").trim();
                if (!valeurs.isEmpty() && !valeurs.equals("];")) {
                    String[] tokens = valeurs.split("\\s+");
                    double[] ligneMat = new double[tokens.length];
                    for (int i = 0; i < tokens.length; i++)
                        ligneMat[i] = Double.parseDouble(tokens[i]);
                    lignes.add(ligneMat);
                }
            }
        }
        return lignes.toArray(new double[0][]);
    }
}
