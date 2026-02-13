package controleur;

import metier.*;
import ihm.FenetreVRP;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * Controleur MVC : fait le lien entre le modele (RecuitSimule) et la vue
 * (FenetreVRP).
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

    /** Lance l'interface graphique. */
    public void demarrerApplication() {
        SwingUtilities.invokeLater(() -> fenetre.initialiser(
                recuit.getTemperatureInitiale(),
                recuit.getTemperatureFinale(),
                recuit.getTauxRefroidissement(),
                recuit.getIterationsParTemperature(),
                recuit.getMaxIterationsSansAmelioration()));
    }

    // --- EcouteurDemarrage ---

    @Override
    public void onDemarrer(double t0, double tf, double alpha,
            int iterations, int stagnation, int frequence) {
        recuit.setTemperatureInitiale(t0);
        recuit.setTemperatureFinale(tf);
        recuit.setTauxRefroidissement(alpha);
        recuit.setIterationsParTemperature(iterations);
        recuit.setMaxIterationsSansAmelioration(stagnation);
        recuit.setFrequenceAffichage(frequence);

        new Thread(() -> {
            fenetre.setEnCours(true);
            fenetre.afficherEtatEnCours();

            try {
                long debut = System.currentTimeMillis();
                Solution meilleure = recuit.executer();
                long duree = System.currentTimeMillis() - debut;

                fenetre.setEnCours(false);
                fenetre.afficherEtatTermine(meilleure, recuit.compterVehicules(meilleure), duree);
            } catch (RuntimeException ex) {
                fenetre.setEnCours(false);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null,
                            ex.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                    fenetre.afficherEtatEnCours();
                });
            }
        }).start();
    }

    // --- RecuitListener ---

    @Override
    public void onNouveauMeilleur(int iter, double cout, double temp, int nbV) {
        fenetre.ajouterLog(String.format("Iter %d | Cout: %.2f | T: %.2f | Vehicules: %d\n",
                iter, cout, temp, nbV));
    }

    @Override
    public void onMiseAJour(int iter, double temp, double cout, int nbV) {
        fenetre.mettreAJourStats(iter, temp, cout, nbV);
    }

    @Override
    public void onSolutionMiseAJour(Solution solution) {
        fenetre.getPanel().setSolution(solution);
        fenetre.getPanel().repaint();
    }

    // --- Main ---

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java controleur.ControleurVRP <fichier.dat ou .txt>");
            return;
        }
        try {
            String fichier = args[0];
            DonneesVRP donnees = fichier.endsWith(".txt")
                    ? ChargeurFichier.lireFichierTxt(fichier)
                    : ChargeurFichier.lireFichierDat(fichier);
            donnees.afficherDonnees();
            new ControleurVRP(donnees).demarrerApplication();
        } catch (IOException e) {
            System.err.println("Erreur lecture fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
