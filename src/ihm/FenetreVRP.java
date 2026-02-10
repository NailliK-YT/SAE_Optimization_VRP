package ihm;

import metier.DonneesVRP;
import metier.Solution;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Fenetre principale de l'application VRP.
 * Gere l'affichage des parametres, statistiques, legende et journal.
 */
public class FenetreVRP {

    /**
     * Interface pour notifier le controleur du clic sur Demarrer
     */
    public interface EcouteurDemarrage {
        void onDemarrer(double t0, double tf, double alpha, int iterations, int stagnation);
    }

    // Couleurs du theme sombre
    public static final Color COULEUR_FOND = new Color(45, 52, 54);
    public static final Color COULEUR_PANNEAU = new Color(55, 65, 70);
    public static final Color COULEUR_ACCENT = new Color(0, 184, 148);
    public static final Color COULEUR_TEXTE = new Color(223, 230, 233);
    public static final Color COULEUR_TITRE = new Color(116, 185, 255);

    private JFrame frame;
    private VRPPanel panel;
    private JTextField champT0, champTfinal, champAlpha, champIterations, champStagnation;
    private JLabel labelCout, labelTemperature, labelIteration, labelVehicules;
    private JProgressBar progressBar;
    private JTextArea textAreaLog;
    private JButton btnDemarrer;
    private volatile boolean enCours = false;

    private DonneesVRP donnees;
    private EcouteurDemarrage ecouteur;

    public FenetreVRP(DonneesVRP donnees) {
        this.donnees = donnees;
    }

    public void setEcouteur(EcouteurDemarrage ecouteur) {
        this.ecouteur = ecouteur;
    }

    public VRPPanel getPanel() {
        return panel;
    }

    public boolean isEnCours() {
        return enCours;
    }

    public void setEnCours(boolean enCours) {
        this.enCours = enCours;
    }

    // ==================== Methodes de mise a jour UI ====================

    /**
     * Met a jour les labels de statistiques en temps reel
     */
    public void mettreAJourStats(int iteration, double temperature, double cout, int nbVehicules) {
        SwingUtilities.invokeLater(() -> {
            labelIteration.setText(formatLabel("Iteration", String.valueOf(iteration)));
            labelTemperature.setText(formatLabel("Temperature", String.format("%.2f", temperature)));
            labelCout.setText(formatLabelVert("Cout actuel", String.format("%.2f", cout)));
            labelVehicules.setText(formatLabel("Vehicules", String.valueOf(nbVehicules)));
        });
    }

    /**
     * Ajoute une ligne au journal des iterations
     */
    public void ajouterLog(String texte) {
        if (textAreaLog != null) {
            SwingUtilities.invokeLater(() -> {
                textAreaLog.append(texte);
                textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength());
            });
        }
    }

    /**
     * Affiche l'etat "en cours" sur les boutons et la progress bar
     */
    public void afficherEtatEnCours() {
        SwingUtilities.invokeLater(() -> {
            btnDemarrer.setEnabled(false);
            btnDemarrer.setText("Optimisation en cours...");
            progressBar.setIndeterminate(true);
            progressBar.setString("Calcul en cours...");
        });
    }

    /**
     * Affiche l'etat "termine" avec les resultats finaux
     */
    public void afficherEtatTermine(Solution meilleure, int nbVehicules, long dureeMs) {
        SwingUtilities.invokeLater(() -> {
            btnDemarrer.setEnabled(true);
            btnDemarrer.setText("Demarrer l'optimisation");
            progressBar.setIndeterminate(false);
            progressBar.setValue(100);
            progressBar.setString("Termine en " + dureeMs + " ms");

            labelCout.setText(formatLabelVert("Cout actuel", String.format("%.2f", meilleure.cout)));
            labelVehicules.setText(formatLabel("Vehicules", String.valueOf(nbVehicules)));
            labelIteration.setText(formatLabel("Iteration", "Termine"));
            labelTemperature.setText(formatLabel("Temperature", "Finale"));

            // Afficher le resume dans le journal
            afficherResultatsFinaux(meilleure, nbVehicules, dureeMs);
        });
    }

    /**
     * Affiche les tournees detaillees dans le journal
     */
    private void afficherResultatsFinaux(Solution meilleure, int nbVehicules, long dureeMs) {
        if (textAreaLog == null)
            return;

        textAreaLog.append("\n========== RESULTATS FINAUX ==========\n");
        textAreaLog.append(String.format("Cout total: %.2f | Vehicules: %d | Temps: %d ms\n\n",
                meilleure.cout, nbVehicules, dureeMs));

        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();

        for (int i = 0; i < meilleure.tournees.size(); i++) {
            List<Integer> tournee = meilleure.tournees.get(i);
            if (tournee.isEmpty())
                continue;

            int charge = 0;
            for (int client : tournee) {
                charge += donnees.getDemandes()[client - 1];
            }

            double distanceTournee = dist[depot][tournee.get(0)];
            for (int j = 0; j < tournee.size() - 1; j++) {
                distanceTournee += dist[tournee.get(j)][tournee.get(j + 1)];
            }
            distanceTournee += dist[tournee.get(tournee.size() - 1)][depot];

            StringBuilder sb = new StringBuilder();
            sb.append("Vehicule ").append(i + 1).append(": Depot");
            for (int client : tournee) {
                sb.append(" -> C").append(client);
            }
            sb.append(String.format(" -> Depot (Distance: %.2f, Charge: %d/%d)\n",
                    distanceTournee, charge, donnees.getCapaciteVehicule()));
            textAreaLog.append(sb.toString());
        }
        textAreaLog.append("\n======================================\n");
        textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength());
    }

    // ==================== Formatage HTML ====================

    private static String formatLabel(String titre, String valeur) {
        return "<html><b style='color:#74b9ff;'>" + titre + ":</b> <span style='color:#dfe6e9;'>"
                + valeur + "</span></html>";
    }

    private static String formatLabelVert(String titre, String valeur) {
        return "<html><b style='color:#74b9ff;'>" + titre + ":</b> <span style='color:#00d26a;'>"
                + valeur + "</span></html>";
    }

    // ==================== Initialisation ====================

    /**
     * Construit et affiche la fenetre principale
     */
    public void initialiser(double t0, double tf, double alpha, int iterations, int stagnation) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        frame = new JFrame("VRP - Recuit Simule");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 850);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(COULEUR_FOND);

        // Panneau de visualisation
        panel = new VRPPanel(donnees);
        panel.setBackground(new Color(30, 39, 46));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 5),
                BorderFactory.createLineBorder(COULEUR_ACCENT, 2, true)));
        frame.add(panel, BorderLayout.CENTER);

        // Panneau de controle a droite
        JPanel panneauDroit = creerPanneauDroit(t0, tf, alpha, iterations, stagnation);
        frame.add(panneauDroit, BorderLayout.EAST);

        // Panneau de log en bas
        JPanel panneauLog = creerPanneauLog();
        frame.add(panneauLog, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel creerPanneauDroit(double t0, double tf, double alpha, int iterations, int stagnation) {
        JPanel panneauDroit = new JPanel();
        panneauDroit.setLayout(new BoxLayout(panneauDroit, BoxLayout.Y_AXIS));
        panneauDroit.setBackground(COULEUR_FOND);
        panneauDroit.setPreferredSize(new Dimension(320, 0));
        panneauDroit.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        // Parametres
        panneauDroit.add(creerSectionParametres(t0, tf, alpha, iterations, stagnation));
        panneauDroit.add(Box.createVerticalStrut(15));

        // Statistiques
        panneauDroit.add(creerSectionStatistiques());
        panneauDroit.add(Box.createVerticalStrut(15));

        // Controle
        panneauDroit.add(creerSectionControle());
        panneauDroit.add(Box.createVerticalStrut(15));

        // Legende
        panneauDroit.add(creerSectionLegende());

        return panneauDroit;
    }

    private JPanel creerSectionParametres(double t0, double tf, double alpha, int iterations, int stagnation) {
        JPanel section = creerSection("Parametres de l'algorithme");

        champT0 = creerChampTexte(String.valueOf(t0));
        champTfinal = creerChampTexte(String.valueOf(tf));
        champAlpha = creerChampTexte(String.valueOf(alpha));
        champIterations = creerChampTexte(String.valueOf(iterations));
        champStagnation = creerChampTexte(String.valueOf(stagnation));

        section.add(creerLigneParametre("T0 (Temperature initiale)", champT0));
        section.add(Box.createVerticalStrut(8));
        section.add(creerLigneParametre("Tf (Temperature finale)", champTfinal));
        section.add(Box.createVerticalStrut(8));
        section.add(creerLigneParametre("Alpha (Refroidissement)", champAlpha));
        section.add(Box.createVerticalStrut(8));
        section.add(creerLigneParametre("Iterations par palier", champIterations));
        section.add(Box.createVerticalStrut(8));
        section.add(creerLigneParametre("Arret si stagnation", champStagnation));

        return section;
    }

    private JPanel creerSectionStatistiques() {
        JPanel section = creerSection("Statistiques en temps reel");

        labelCout = creerLabelInfo("Cout actuel", "---");
        labelTemperature = creerLabelInfo("Temperature", "---");
        labelIteration = creerLabelInfo("Iteration", "---");
        labelVehicules = creerLabelInfo("Vehicules", "---");

        section.add(creerWrapLabel(labelCout));
        section.add(creerWrapLabel(labelTemperature));
        section.add(creerWrapLabel(labelIteration));
        section.add(creerWrapLabel(labelVehicules));

        return section;
    }

    private JPanel creerSectionControle() {
        JPanel section = creerSection("Controle");

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("En attente...");
        progressBar.setBackground(COULEUR_PANNEAU);
        progressBar.setForeground(COULEUR_ACCENT);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        btnDemarrer = new JButton("Demarrer l'optimisation");
        btnDemarrer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDemarrer.setBackground(COULEUR_ACCENT);
        btnDemarrer.setForeground(Color.WHITE);
        btnDemarrer.setFocusPainted(false);
        btnDemarrer.setOpaque(true);
        btnDemarrer.setContentAreaFilled(true);
        btnDemarrer.setBorderPainted(false);
        btnDemarrer.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btnDemarrer.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDemarrer.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnDemarrer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        btnDemarrer.addActionListener(e -> traiterClicDemarrer());

        section.add(progressBar);
        section.add(Box.createVerticalStrut(15));
        section.add(btnDemarrer);

        return section;
    }

    private JPanel creerSectionLegende() {
        JPanel legende = creerSection("Legende");
        legende.add(creerItemLegende("Depot", new Color(231, 76, 60)));
        legende.add(creerItemLegende("Clients", new Color(52, 73, 94)));
        legende.add(creerItemLegende("Routes", COULEUR_ACCENT));
        return legende;
    }

    private JPanel creerPanneauLog() {
        JPanel panneauLog = new JPanel(new BorderLayout());
        panneauLog.setBackground(COULEUR_FOND);
        panneauLog.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        panneauLog.setPreferredSize(new Dimension(0, 180));

        JLabel labelLog = new JLabel("  Journal des iterations");
        labelLog.setFont(new Font("Segoe UI", Font.BOLD, 12));
        labelLog.setForeground(COULEUR_TITRE);
        labelLog.setOpaque(true);
        labelLog.setBackground(COULEUR_PANNEAU);
        labelLog.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panneauLog.add(labelLog, BorderLayout.NORTH);

        textAreaLog = new JTextArea();
        textAreaLog.setEditable(false);
        textAreaLog.setFont(new Font("Consolas", Font.PLAIN, 11));
        textAreaLog.setBackground(new Color(30, 39, 46));
        textAreaLog.setForeground(COULEUR_TEXTE);
        textAreaLog.setCaretColor(COULEUR_ACCENT);

        JScrollPane scrollLog = new JScrollPane(textAreaLog);
        scrollLog.setBorder(BorderFactory.createLineBorder(new Color(80, 90, 100)));
        scrollLog.getVerticalScrollBar().setUnitIncrement(16);
        panneauLog.add(scrollLog, BorderLayout.CENTER);

        return panneauLog;
    }

    // ==================== Validation ====================

    /**
     * Valide les champs et delegue au controleur
     */
    private void traiterClicDemarrer() {
        if (enCours || ecouteur == null)
            return;

        double valT0 = validerDouble(champT0, "T0");
        if (Double.isNaN(valT0))
            return;
        if (valT0 <= 0) {
            erreur("T0 doit etre strictement positif.");
            champT0.requestFocus();
            return;
        }

        double valTf = validerDouble(champTfinal, "Tfinal");
        if (Double.isNaN(valTf))
            return;
        if (valTf <= 0) {
            erreur("Tfinal doit etre strictement positif.");
            champTfinal.requestFocus();
            return;
        }
        if (valT0 <= valTf) {
            erreur("T0 doit etre superieur a Tfinal.");
            champT0.requestFocus();
            return;
        }

        double valAlpha = validerDouble(champAlpha, "Alpha");
        if (Double.isNaN(valAlpha))
            return;
        if (valAlpha <= 0 || valAlpha >= 1) {
            erreur("Alpha doit etre entre 0 et 1 (exclus).");
            champAlpha.requestFocus();
            return;
        }

        int valIter = validerEntier(champIterations, "Iterations");
        if (valIter < 0)
            return;
        if (valIter == 0) {
            erreur("Iterations doit etre strictement positif.");
            champIterations.requestFocus();
            return;
        }

        int valStag = validerEntier(champStagnation, "Stagnation");
        if (valStag < 0)
            return;
        if (valStag == 0) {
            erreur("Stagnation doit etre strictement positif.");
            champStagnation.requestFocus();
            return;
        }

        ecouteur.onDemarrer(valT0, valTf, valAlpha, valIter, valStag);
    }

    private double validerDouble(JTextField champ, String nom) {
        try {
            return Double.parseDouble(champ.getText().trim());
        } catch (NumberFormatException ex) {
            erreur(nom + " doit etre un nombre valide.");
            champ.requestFocus();
            return Double.NaN;
        }
    }

    private int validerEntier(JTextField champ, String nom) {
        try {
            return Integer.parseInt(champ.getText().trim());
        } catch (NumberFormatException ex) {
            erreur(nom + " doit etre un entier valide.");
            champ.requestFocus();
            return -1;
        }
    }

    private void erreur(String message) {
        JOptionPane.showMessageDialog(frame, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    // ==================== Composants UI ====================

    private JPanel creerWrapLabel(JLabel label) {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setBackground(COULEUR_PANNEAU);
        wrap.add(label);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        return wrap;
    }

    private JPanel creerSection(String titre) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(COULEUR_PANNEAU);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 100), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelTitre = new JLabel(titre, SwingConstants.CENTER);
        labelTitre.setFont(new Font("Segoe UI", Font.BOLD, 13));
        labelTitre.setForeground(COULEUR_TITRE);
        labelTitre.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelTitre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        section.add(labelTitre);
        section.add(Box.createVerticalStrut(10));

        return section;
    }

    private JTextField creerChampTexte(String valeur) {
        JTextField champ = new JTextField(valeur);
        champ.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        champ.setPreferredSize(new Dimension(100, 28));
        champ.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        champ.setBackground(new Color(40, 48, 52));
        champ.setForeground(COULEUR_TEXTE);
        champ.setCaretColor(COULEUR_ACCENT);
        champ.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 100)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return champ;
    }

    private JPanel creerLigneParametre(String label, JTextField champ) {
        JPanel ligne = new JPanel(new BorderLayout(8, 0));
        ligne.setBackground(COULEUR_PANNEAU);
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(COULEUR_TEXTE);

        ligne.add(lbl, BorderLayout.CENTER);
        ligne.add(champ, BorderLayout.EAST);
        return ligne;
    }

    private JLabel creerLabelInfo(String titre, String valeur) {
        JLabel label = new JLabel(formatLabel(titre, valeur));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return label;
    }

    private JPanel creerItemLegende(String texte, Color couleur) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        item.setBackground(COULEUR_PANNEAU);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JPanel carre = new JPanel();
        carre.setPreferredSize(new Dimension(12, 12));
        carre.setBackground(couleur);
        carre.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

        JLabel lbl = new JLabel(texte);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(COULEUR_TEXTE);

        item.add(carre);
        item.add(lbl);
        return item;
    }
}
