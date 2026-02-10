package controleur;

import metier.*;
import ihm.FenetreVRP;

import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * Controleur principal de l'application VRP.
 * Fait le lien entre le modele (RecuitSimule) et la vue (FenetreVRP).
 */
public class ControleurVRP implements FenetreVRP.EcouteurDemarrage, RecuitSimule.RecuitListener {

    private DonneesVRP donnees;
    private RecuitSimule recuit;
    private FenetreVRP fenetre;

    public ControleurVRP(DonneesVRP donnees) {
        this.donnees = donnees;
        this.recuit = new RecuitSimule(donnees);
        this.recuit.setListener(this);

        this.fenetre = new FenetreVRP(donnees);
        this.fenetre.setEcouteur(this);
    }

    /**
     * Lance l'interface graphique
     */
    public void demarrerApplication() {
        SwingUtilities.invokeLater(() -> fenetre.initialiser(
                recuit.getTemperatureInitiale(),
                recuit.getTemperatureFinale(),
                recuit.getTauxRefroidissement(),
                recuit.getIterationsParTemperature(),
                recuit.getMaxIterationsSansAmelioration()));
    }

    // ==================== EcouteurDemarrage ====================

    @Override
    public void onDemarrer(double t0, double tf, double alpha, int iterations, int stagnation) {
        // Appliquer les parametres
        recuit.setTemperatureInitiale(t0);
        recuit.setTemperatureFinale(tf);
        recuit.setTauxRefroidissement(alpha);
        recuit.setIterationsParTemperature(iterations);
        recuit.setMaxIterationsSansAmelioration(stagnation);

        // Lancer l'algorithme dans un thread separe
        new Thread(() -> {
            fenetre.setEnCours(true);
            fenetre.afficherEtatEnCours();

            long debut = System.currentTimeMillis();
            Solution meilleure = recuit.executer();
            long duree = System.currentTimeMillis() - debut;

            fenetre.setEnCours(false);
            fenetre.afficherEtatTermine(meilleure, recuit.compterVehiculesUtilises(meilleure), duree);
        }).start();
    }

    // ==================== RecuitListener ====================

    @Override
    public void onNouveauMeilleur(int iteration, double cout, double temperature, int nbVehicules) {
        fenetre.ajouterLog(String.format("Iter %d | Cout: %.2f | T: %.2f | Vehicules: %d\n",
                iteration, cout, temperature, nbVehicules));
    }

    @Override
    public void onMiseAJour(int iteration, double temperature, double cout, int nbVehicules) {
        fenetre.mettreAJourStats(iteration, temperature, cout, nbVehicules);
    }

    @Override
    public void onSolutionMiseAJour(Solution solution) {
        fenetre.getPanel().setSolution(solution);
        fenetre.getPanel().repaint();
    }

    // ==================== Main ====================

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java controleur.ControleurVRP <fichier.dat ou .txt>");
            System.out.println("Exemple: java controleur.ControleurVRP tai75a.txt");
            return;
        }

        try {
            String fichier = args[0];
            DonneesVRP donnees = fichier.endsWith(".txt")
                    ? ChargeurFichier.lireFichierTxt(fichier)
                    : ChargeurFichier.lireFichierDat(fichier);

            donnees.afficherDonnees();

            ControleurVRP controleur = new ControleurVRP(donnees);
            controleur.demarrerApplication();

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
