import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    // Structures pour stocker les coordonnées
    private double[] xCoords;
    private double[] yCoords;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java RecuitSimuleVRP <fichier.dat ou .txt>");
            System.out.println("Exemple: java RecuitSimuleVRP tai75a.txt");
            return;
        }

        RecuitSimuleVRP vrp = new RecuitSimuleVRP();
        try {
            String fichier = args[0];
            if (fichier.endsWith(".txt")) {
                vrp.lireFichierTxt(fichier);
            } else {
                vrp.lireFichierDat(fichier);
            }

            vrp.afficherDonnees();

            System.out.println("\n=== DÉBUT DU RECUIT SIMULÉ ===\n");

            // Initialiser la visualisation
            vrp.initialiserVisualisation();

            long startTime = System.currentTimeMillis();
            Solution meilleuresolution = vrp.recuitSimule();
            long endTime = System.currentTimeMillis();

            System.out.println("\n=== SOLUTION OPTIMALE ===");
            vrp.afficherSolution(meilleuresolution);
            System.out.println("Temps d'exécution: " + (endTime - startTime) + " ms");

        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture du fichier: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Lit un fichier .txt format Taillard/Standard
     */
    public void lireFichierTxt(String nomFichier) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(nomFichier));

        // 1. Lire nbClients et meilleur coût (on ignore le coût pour l'instant)
        String ligne = lireLigneNonVide(reader);
        if (ligne == null)
            throw new IOException("Fichier vide");
        String[] parts = ligne.trim().split("\\s+");
        nbClients = Integer.parseInt(parts[0]);

        // 2. Lire capacité
        ligne = lireLigneNonVide(reader);
        capaciteVehicule = Integer.parseInt(ligne.trim());

        // 3. Lire coordonnées du dépôt
        ligne = lireLigneNonVide(reader);
        parts = ligne.trim().split("\\s+");
        double depotX = Double.parseDouble(parts[0]);
        double depotY = Double.parseDouble(parts[1]);

        // Initialisation des tableaux
        demandes = new int[nbClients];
        xCoords = new double[nbClients + 1];
        yCoords = new double[nbClients + 1];

        // Stocker le dépôt (indice 0)
        xCoords[0] = depotX;
        yCoords[0] = depotY;

        // 4. Lire les clients
        for (int i = 0; i < nbClients; i++) {
            ligne = lireLigneNonVide(reader);
            parts = ligne.trim().split("\\s+");

            int id = Integer.parseInt(parts[0]); // Devrait être i+1
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            int demande = Integer.parseInt(parts[3]);

            // Stocker
            xCoords[id] = x;
            yCoords[id] = y;
            demandes[id - 1] = demande; // demandes[0] pour client 1
        }

        reader.close();

        // Calculer la matrice des distances
        calculerDistancesEuclidiennes();

        // Estimation du nombre de véhicules
        nbVehicules = (int) (1.5 * Arrays.stream(demandes).sum() / capaciteVehicule) + 2;
        if (nbVehicules < 5)
            nbVehicules = 10;

        // Validation
        if (nbClients == 0 || capaciteVehicule == 0) {
            throw new IOException("Données incomplètes dans le fichier .txt");
        }
    }

    private String lireLigneNonVide(BufferedReader reader) throws IOException {
        String ligne;
        while ((ligne = reader.readLine()) != null) {
            if (!ligne.trim().isEmpty()) {
                return ligne;
            }
        }
        return null; // Fin de fichier
    }

    private void calculerDistancesEuclidiennes() {
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

    private int maxIterationsSansAmelioration = 3000;

    // Pour la visualisation
    private JFrame frame;
    private VRPPanel panel;
    private Solution solutionAVisualiser;

    // Champs de saisie pour les paramètres
    private JTextField champT0;
    private JTextField champTfinal;
    private JTextField champAlpha;
    private JTextField champIterations;
    private JTextField champStagnation;
    private JLabel labelCout;
    private JLabel labelTemperature;
    private JLabel labelIteration;
    private JLabel labelVehicules;
    private JProgressBar progressBar;
    private JTextArea textAreaLog;
    private volatile boolean enCours = false;

    // Couleurs du thème
    private static final Color COULEUR_FOND = new Color(45, 52, 54);
    private static final Color COULEUR_PANNEAU = new Color(55, 65, 70);
    private static final Color COULEUR_ACCENT = new Color(0, 184, 148);
    private static final Color COULEUR_TEXTE = new Color(223, 230, 233);
    private static final Color COULEUR_TITRE = new Color(116, 185, 255);

    public void initialiserVisualisation() {
        if (xCoords == null || xCoords.length == 0)
            return;

        // Configuration du Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        frame = new JFrame("VRP - Recuit Simule");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 850);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(COULEUR_FOND);

        // Panneau de visualisation avec bordure
        panel = new VRPPanel();
        panel.setBackground(new Color(30, 39, 46));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 5),
                BorderFactory.createLineBorder(COULEUR_ACCENT, 2, true)));
        frame.add(panel, BorderLayout.CENTER);

        // Panneau de contrôle principal
        JPanel panneauDroit = new JPanel();
        panneauDroit.setLayout(new BoxLayout(panneauDroit, BoxLayout.Y_AXIS));
        panneauDroit.setBackground(COULEUR_FOND);
        panneauDroit.setPreferredSize(new Dimension(320, 0));
        panneauDroit.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        // === Section Paramètres ===
        JPanel sectionParams = creerSection("Parametres de l'algorithme");

        champT0 = creerChampTexte(String.valueOf(temperatureInitiale));
        champTfinal = creerChampTexte(String.valueOf(temperatureFinale));
        champAlpha = creerChampTexte(String.valueOf(tauxRefroidissement));
        champIterations = creerChampTexte(String.valueOf(iterationsParTemperature));
        champStagnation = creerChampTexte(String.valueOf(maxIterationsSansAmelioration));

        sectionParams.add(creerLigneParametre("T₀ (Température initiale)", champT0));
        sectionParams.add(Box.createVerticalStrut(8));
        sectionParams.add(creerLigneParametre("Tₓ (Température finale)", champTfinal));
        sectionParams.add(Box.createVerticalStrut(8));
        sectionParams.add(creerLigneParametre("α (Taux de refroidissement)", champAlpha));
        sectionParams.add(Box.createVerticalStrut(8));
        sectionParams.add(creerLigneParametre("Itérations par palier", champIterations));
        sectionParams.add(Box.createVerticalStrut(8));
        sectionParams.add(creerLigneParametre("Arrêt si stagnation (itér.)", champStagnation));

        panneauDroit.add(sectionParams);
        panneauDroit.add(Box.createVerticalStrut(15));

        // === Section Statistiques ===
        JPanel sectionStats = creerSection("Statistiques en temps reel");

        labelCout = creerLabelInfo("Cout actuel", "---");
        labelTemperature = creerLabelInfo("Temperature", "---");
        labelIteration = creerLabelInfo("Iteration", "---");
        labelVehicules = creerLabelInfo("Vehicules", "---");

        JPanel wrapCout = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapCout.setBackground(COULEUR_PANNEAU);
        wrapCout.add(labelCout);
        wrapCout.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JPanel wrapTemp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapTemp.setBackground(COULEUR_PANNEAU);
        wrapTemp.add(labelTemperature);
        wrapTemp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JPanel wrapIter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapIter.setBackground(COULEUR_PANNEAU);
        wrapIter.add(labelIteration);
        wrapIter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JPanel wrapVeh = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrapVeh.setBackground(COULEUR_PANNEAU);
        wrapVeh.add(labelVehicules);
        wrapVeh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        sectionStats.add(wrapCout);
        sectionStats.add(wrapTemp);
        sectionStats.add(wrapIter);
        sectionStats.add(wrapVeh);

        panneauDroit.add(sectionStats);
        panneauDroit.add(Box.createVerticalStrut(15));

        // === Section Contrôle ===
        JPanel sectionControle = creerSection("Controle");

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("En attente...");
        progressBar.setBackground(COULEUR_PANNEAU);
        progressBar.setForeground(COULEUR_ACCENT);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton btnDemarrer = new JButton("Demarrer l'optimisation");
        btnDemarrer.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btnDemarrer.setBackground(COULEUR_ACCENT);
        btnDemarrer.setForeground(Color.WHITE);
        btnDemarrer.setFocusPainted(false);
        btnDemarrer.setOpaque(true);
        btnDemarrer.setContentAreaFilled(true);
        btnDemarrer.setBorderPainted(false);
        btnDemarrer.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btnDemarrer.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnDemarrer.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        btnDemarrer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));

        btnDemarrer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!enCours) {
                    try {
                        temperatureInitiale = Double.parseDouble(champT0.getText());
                        temperatureFinale = Double.parseDouble(champTfinal.getText());
                        tauxRefroidissement = Double.parseDouble(champAlpha.getText());
                        iterationsParTemperature = Integer.parseInt(champIterations.getText());
                        maxIterationsSansAmelioration = Integer.parseInt(champStagnation.getText());
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame,
                                "Veuillez entrer des valeurs numeriques valides.",
                                "Erreur de saisie", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    new Thread(() -> {
                        enCours = true;
                        SwingUtilities.invokeLater(() -> {
                            btnDemarrer.setEnabled(false);
                            btnDemarrer.setText("Optimisation en cours...");
                            progressBar.setIndeterminate(true);
                            progressBar.setString("Calcul en cours...");
                        });

                        long startTime = System.currentTimeMillis();
                        Solution meilleure = recuitSimule();
                        long endTime = System.currentTimeMillis();

                        enCours = false;
                        SwingUtilities.invokeLater(() -> {
                            btnDemarrer.setEnabled(true);
                            btnDemarrer.setText("Demarrer l'optimisation");
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(100);
                            progressBar.setString("Termine en " + (endTime - startTime) + " ms");

                            labelCout.setText(
                                    "<html><b style='color:#74b9ff;'>Cout actuel:</b> <span style='color:#00d26a;'>"
                                            + String.format("%.2f", meilleure.cout) + "</span></html>");
                            int nbVehicules = compterVehiculesUtilises(meilleure);
                            labelVehicules.setText(
                                    "<html><b style='color:#74b9ff;'>Vehicules:</b> <span style='color:#dfe6e9;'>"
                                            + nbVehicules + "</span></html>");
                            labelIteration.setText(
                                    "<html><b style='color:#74b9ff;'>Iteration:</b> <span style='color:#dfe6e9;'>Termine</span></html>");
                            labelTemperature.setText(
                                    "<html><b style='color:#74b9ff;'>Temperature:</b> <span style='color:#dfe6e9;'>Finale</span></html>");
                        });
                    }).start();
                }
            }
        });

        sectionControle.add(progressBar);
        sectionControle.add(Box.createVerticalStrut(15));
        sectionControle.add(btnDemarrer);

        panneauDroit.add(sectionControle);
        panneauDroit.add(Box.createVerticalGlue());

        JPanel legende = creerSection("Legende");
        legende.add(creerItemLegende("Depot", new Color(231, 76, 60)));
        legende.add(creerItemLegende("Clients", new Color(52, 73, 94)));
        legende.add(creerItemLegende("Routes", COULEUR_ACCENT));
        panneauDroit.add(legende);

        frame.add(panneauDroit, BorderLayout.EAST);

        // === Panneau de log en bas ===
        JPanel panneauLog = new JPanel(new BorderLayout());
        panneauLog.setBackground(COULEUR_FOND);
        panneauLog.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        panneauLog.setPreferredSize(new Dimension(0, 150));

        JLabel labelLog = new JLabel("  Journal des iterations");
        labelLog.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        labelLog.setForeground(COULEUR_TITRE);
        labelLog.setOpaque(true);
        labelLog.setBackground(COULEUR_PANNEAU);
        labelLog.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panneauLog.add(labelLog, BorderLayout.NORTH);

        textAreaLog = new JTextArea();
        textAreaLog.setEditable(false);
        textAreaLog.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 11));
        textAreaLog.setBackground(new Color(30, 39, 46));
        textAreaLog.setForeground(COULEUR_TEXTE);
        textAreaLog.setCaretColor(COULEUR_ACCENT);

        JScrollPane scrollLog = new JScrollPane(textAreaLog);
        scrollLog.setBorder(BorderFactory.createLineBorder(new Color(80, 90, 100)));
        scrollLog.getVerticalScrollBar().setUnitIncrement(16);
        panneauLog.add(scrollLog, BorderLayout.CENTER);

        frame.add(panneauLog, BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel creerSection(String titre) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(COULEUR_PANNEAU);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 100), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        section.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        JLabel labelTitre = new JLabel(titre, SwingConstants.CENTER);
        labelTitre.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
        labelTitre.setForeground(COULEUR_TITRE);
        labelTitre.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        labelTitre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        section.add(labelTitre);
        section.add(Box.createVerticalStrut(10));

        return section;
    }

    private JTextField creerChampTexte(String valeur) {
        JTextField champ = new JTextField(valeur);
        champ.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 12));
        champ.setBackground(new Color(35, 45, 50));
        champ.setForeground(COULEUR_TEXTE);
        champ.setCaretColor(COULEUR_TEXTE);
        champ.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 90)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        return champ;
    }

    private JPanel creerLigneParametre(String label, JTextField champ) {
        JPanel ligne = new JPanel(new BorderLayout(10, 0));
        ligne.setBackground(COULEUR_PANNEAU);
        ligne.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        lbl.setForeground(COULEUR_TEXTE);
        ligne.add(lbl, BorderLayout.WEST);

        champ.setPreferredSize(new Dimension(100, 28));
        ligne.add(champ, BorderLayout.EAST);

        return ligne;
    }

    private JLabel creerLabelInfo(String titre, String valeur) {
        JLabel label = new JLabel("<html><b style='color:#74b9ff;'>" + titre + ":</b> <span style='color:#dfe6e9;'>"
                + valeur + "</span></html>");
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return label;
    }

    private JPanel creerItemLegende(String texte, Color couleur) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        item.setBackground(COULEUR_PANNEAU);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JPanel carre = new JPanel();
        carre.setBackground(couleur);
        carre.setPreferredSize(new Dimension(12, 12));
        item.add(carre);

        JLabel lbl = new JLabel(texte);
        lbl.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        lbl.setForeground(COULEUR_TEXTE);
        item.add(lbl);

        return item;
    }

    /**
     * Algorithme principal du recuit simulé
     */
    public Solution recuitSimule() {
        // Générer une solution initiale aléatoire
        Solution solutionCourante = genererSolutionInitiale();
        Solution meilleuresolution = solutionCourante.copier();
        solutionAVisualiser = solutionCourante;

        double temperature = temperatureInitiale;
        int iteration = 0;
        int iterationsSansAmelioration = 0;

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
                            solutionAVisualiser = meilleuresolution; // Visualiser la meilleure

                            iterationsSansAmelioration = 0; // Reset

                            System.out.printf("Iteration %d - Nouveau meilleur: %.2f (T=%.2f)\n",
                                    iteration, meilleuresolution.cout, temperature);

                            // Ajouter au log graphique
                            if (textAreaLog != null) {
                                final String logLine = String.format("Iter %d | Cout: %.2f | T: %.2f | Vehicules: %d\n",
                                        iteration, meilleuresolution.cout, temperature,
                                        compterVehiculesUtilises(meilleuresolution));
                                SwingUtilities.invokeLater(() -> {
                                    textAreaLog.append(logLine);
                                    textAreaLog.setCaretPosition(textAreaLog.getDocument().getLength());
                                });
                            }

                        } else {
                            iterationsSansAmelioration++;
                        }
                    }
                }

                // Mettre a jour la visualisation tous les X iterations pour ne pas ralentir
                if (iteration % 50 == 0 && panel != null) {
                    final int currentIteration = iteration;
                    final double currentTemp = temperature;
                    final double currentCout = meilleuresolution.cout;
                    final int nbVeh = compterVehiculesUtilises(meilleuresolution);
                    SwingUtilities.invokeLater(() -> {
                        labelIteration
                                .setText("<html><b style='color:#74b9ff;'>Iteration:</b> <span style='color:#dfe6e9;'>"
                                        + currentIteration + "</span></html>");
                        labelTemperature.setText(
                                "<html><b style='color:#74b9ff;'>Temperature:</b> <span style='color:#dfe6e9;'>"
                                        + String.format("%.2f", currentTemp) + "</span></html>");
                        labelCout.setText(
                                "<html><b style='color:#74b9ff;'>Cout actuel:</b> <span style='color:#00d26a;'>"
                                        + String.format("%.2f", currentCout) + "</span></html>");
                        labelVehicules
                                .setText("<html><b style='color:#74b9ff;'>Vehicules:</b> <span style='color:#dfe6e9;'>"
                                        + nbVeh + "</span></html>");
                    });
                    panel.repaint();
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                    }
                }
            }

            // Critère d'arrêt supplémentaire : stagnation
            if (iterationsSansAmelioration > maxIterationsSansAmelioration) {
                System.out.println(
                        "Arrêt anticipé : Pas d'amélioration depuis " + iterationsSansAmelioration + " itérations.");
                break;
            }

            // Refroidissement
            temperature *= tauxRefroidissement;
        }

        // Affichage final
        if (panel != null) {
            solutionAVisualiser = meilleuresolution;
            panel.repaint();
        }

        return meilleuresolution;
    }

    /**
     * Panneau de visualisation pour le VRP
     */
    class VRPPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (solutionAVisualiser == null || xCoords == null)
                return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int margin = 50;

            // Trouver min/max pour mise à l'échelle
            double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
            double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;

            for (int i = 0; i < nbClients + 1; i++) {
                if (xCoords[i] < minX)
                    minX = xCoords[i];
                if (xCoords[i] > maxX)
                    maxX = xCoords[i];
                if (yCoords[i] < minY)
                    minY = yCoords[i];
                if (yCoords[i] > maxY)
                    maxY = yCoords[i];
            }

            double scaleX = (width - 2 * margin) / (maxX - minX);
            double scaleY = (height - 2 * margin) / (maxY - minY);
            double scale = Math.min(scaleX, scaleY);

            // Fonction de conversion
            // Attention: Y inversé en Swing (0 en haut)
            // On veut afficher Y croissant vers le haut ? Souvent standard math.
            // Mais pour coordonnées de map c'est souvent Y vers bas ou haut.
            // On va faire Y vers le haut (donc height - y)

            // Couleurs vives pour les vehicules
            Color[] colors = {
                    new Color(255, 107, 107), // Rouge vif
                    new Color(78, 205, 196), // Turquoise
                    new Color(255, 230, 109), // Jaune
                    new Color(199, 128, 232), // Violet
                    new Color(255, 159, 67), // Orange
                    new Color(116, 185, 255), // Bleu clair
                    new Color(162, 217, 206), // Vert menthe
                    new Color(255, 177, 193) // Rose
            };

            for (int i = 0; i < solutionAVisualiser.tournees.size(); i++) {
                List<Integer> tournee = solutionAVisualiser.tournees.get(i);
                if (tournee.isEmpty())
                    continue;

                g2.setColor(colors[i % colors.length]);
                g2.setStroke(new BasicStroke(3));

                // Dépôt -> Premier client
                int x1 = (int) (margin + (xCoords[0] - minX) * scale);
                int y1 = (int) (height - margin - (yCoords[0] - minY) * scale);

                int first = tournee.get(0);
                int x2 = (int) (margin + (xCoords[first] - minX) * scale);
                int y2 = (int) (height - margin - (yCoords[first] - minY) * scale);

                g2.drawLine(x1, y1, x2, y2);

                // Clients entre eux
                for (int j = 0; j < tournee.size() - 1; j++) {
                    int c1 = tournee.get(j);
                    int c2 = tournee.get(j + 1);

                    x1 = (int) (margin + (xCoords[c1] - minX) * scale);
                    y1 = (int) (height - margin - (yCoords[c1] - minY) * scale);
                    x2 = (int) (margin + (xCoords[c2] - minX) * scale);
                    y2 = (int) (height - margin - (yCoords[c2] - minY) * scale);

                    g2.drawLine(x1, y1, x2, y2);
                }

                // Dernier client -> Dépôt
                int last = tournee.get(tournee.size() - 1);
                x1 = (int) (margin + (xCoords[last] - minX) * scale);
                y1 = (int) (height - margin - (yCoords[last] - minY) * scale);

                x2 = (int) (margin + (xCoords[0] - minX) * scale);
                y2 = (int) (height - margin - (yCoords[0] - minY) * scale);

                g2.drawLine(x1, y1, x2, y2);
            }

            // Dessiner les clients (cercles avec numero a l'interieur)
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 10));
            for (int i = 1; i <= nbClients; i++) {
                int x = (int) (margin + (xCoords[i] - minX) * scale);
                int y = (int) (height - margin - (yCoords[i] - minY) * scale);

                int radius = 12;
                // Fond du cercle
                g2.setColor(new Color(52, 73, 94));
                g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                // Bordure
                g2.setColor(Color.WHITE);
                g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);
                // Numero centre
                String num = String.valueOf(i);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int textWidth = fm.stringWidth(num);
                int textHeight = fm.getAscent();
                g2.drawString(num, x - textWidth / 2, y + textHeight / 2 - 2);
            }

            // Dessiner le depot (Carre rouge)
            int dx = (int) (margin + (xCoords[0] - minX) * scale);
            int dy = (int) (height - margin - (yCoords[0] - minY) * scale);
            g2.setColor(new Color(231, 76, 60));
            g2.fillRect(dx - 8, dy - 8, 16, 16);
            g2.setColor(Color.WHITE);
            g2.drawRect(dx - 8, dy - 8, 16, 16);
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
            g2.drawString("DEPOT", dx + 12, dy + 4);

            // Afficher infos en haut a gauche
            g2.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
            g2.setColor(new Color(46, 204, 113));
            g2.drawString("Cout: " + String.format("%.2f", solutionAVisualiser.cout), 15, 25);
        }
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