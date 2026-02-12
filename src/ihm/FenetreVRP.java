package ihm;

import metier.DonneesVRP;
import metier.Solution;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Fenetre principale de l'application VRP.
 */
public class FenetreVRP {

    /** Interface pour notifier le controleur du clic sur Demarrer. */
    public interface EcouteurDemarrage {
        void onDemarrer(double t0, double tf, double alpha,
                int iterations, int stagnation, int frequence);
    }

    // Theme sombre
    static final Color FOND = new Color(45, 52, 54);
    static final Color PANNEAU = new Color(55, 65, 70);
    static final Color ACCENT = new Color(0, 184, 148);
    static final Color TEXTE = new Color(223, 230, 233);
    static final Color TITRE = new Color(116, 185, 255);

    private JFrame frame;
    private VRPPanel panel;
    private JTextField champT0, champTf, champAlpha, champIter, champStag, champFreq;
    private JLabel lblCout, lblTemp, lblIter, lblVeh;
    private JProgressBar progress;
    private JTextArea log;
    private JButton btnStart;
    private volatile boolean enCours = false;

    private DonneesVRP donnees;
    private EcouteurDemarrage ecouteur;

    public FenetreVRP(DonneesVRP donnees) {
        this.donnees = donnees;
    }

    public void setEcouteur(EcouteurDemarrage e) {
        this.ecouteur = e;
    }

    public VRPPanel getPanel() {
        return panel;
    }

    public void setEnCours(boolean b) {
        this.enCours = b;
    }

    // --- Mises a jour UI ---

    public void mettreAJourStats(int iter, double temp, double cout, int nbV) {
        SwingUtilities.invokeLater(() -> {
            lblIter.setText(html("Iteration", String.valueOf(iter)));
            lblTemp.setText(html("Temperature", String.format("%.2f", temp)));
            lblCout.setText(htmlVert("Cout actuel", String.format("%.2f", cout)));
            lblVeh.setText(html("Vehicules", String.valueOf(nbV)));
        });
    }

    public void ajouterLog(String texte) {
        if (log != null)
            SwingUtilities.invokeLater(() -> {
                log.append(texte);
                log.setCaretPosition(log.getDocument().getLength());
            });
    }

    public void afficherEtatEnCours() {
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(false);
            btnStart.setText("Optimisation en cours...");
            progress.setIndeterminate(true);
            progress.setString("Calcul en cours...");
        });
    }

    public void afficherEtatTermine(Solution best, int nbV, long ms) {
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(true);
            btnStart.setText("Demarrer l'optimisation");
            progress.setIndeterminate(false);
            progress.setValue(100);
            progress.setString("Termine en " + ms + " ms");
            lblCout.setText(htmlVert("Cout actuel", String.format("%.2f", best.cout)));
            lblVeh.setText(html("Vehicules", String.valueOf(nbV)));
            lblIter.setText(html("Iteration", "Termine"));
            lblTemp.setText(html("Temperature", "Finale"));
            afficherResultats(best, nbV, ms);
        });
    }

    private void afficherResultats(Solution best, int nbV, long ms) {
        if (log == null)
            return;
        log.append("\n========== RESULTATS FINAUX ==========\n");
        log.append(String.format("Cout: %.2f | Vehicules: %d | Temps: %d ms\n\n",
                best.cout, nbV, ms));

        double[][] dist = donnees.getDistanceMatrix();
        int depot = donnees.getDepot();
        for (int i = 0; i < best.tournees.size(); i++) {
            List<Integer> route = best.tournees.get(i);
            if (route.isEmpty())
                continue;
            int charge = 0;
            for (int c : route)
                charge += donnees.getDemandes()[c - 1];
            double d = dist[depot][route.get(0)];
            for (int j = 0; j < route.size() - 1; j++)
                d += dist[route.get(j)][route.get(j + 1)];
            d += dist[route.get(route.size() - 1)][depot];
            StringBuilder sb = new StringBuilder("V" + (i + 1) + ": Depot");
            for (int c : route)
                sb.append(" -> C").append(c);
            sb.append(String.format(" -> Depot (Dist: %.2f, Charge: %d/%d)\n",
                    d, charge, donnees.getCapaciteVehicule()));
            log.append(sb.toString());
        }
        log.append("======================================\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    // --- HTML labels ---

    private static String html(String t, String v) {
        return "<html><b style='color:#74b9ff;'>" + t + ":</b> " +
                "<span style='color:#dfe6e9;'>" + v + "</span></html>";
    }

    private static String htmlVert(String t, String v) {
        return "<html><b style='color:#74b9ff;'>" + t + ":</b> " +
                "<span style='color:#00d26a;'>" + v + "</span></html>";
    }

    // --- Construction de la fenetre ---

    public void initialiser(double t0, double tf, double alpha, int iter, int stag) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        frame = new JFrame("VRP - Recuit Simule");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 850);
        frame.setLayout(new BorderLayout(10, 10));
        frame.getContentPane().setBackground(FOND);

        panel = new VRPPanel(donnees);
        panel.setBackground(new Color(30, 39, 46));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 5),
                BorderFactory.createLineBorder(ACCENT, 2, true)));
        frame.add(panel, BorderLayout.CENTER);
        frame.add(creerDroit(t0, tf, alpha, iter, stag), BorderLayout.EAST);
        frame.add(creerLog(), BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel creerDroit(double t0, double tf, double alpha, int iter, int stag) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(FOND);
        p.setPreferredSize(new Dimension(320, 0));
        p.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));

        p.add(creerParams(t0, tf, alpha, iter, stag));
        p.add(Box.createVerticalStrut(15));
        p.add(creerStats());
        p.add(Box.createVerticalStrut(15));
        p.add(creerControle());
        p.add(Box.createVerticalStrut(15));
        p.add(creerLegende());
        return p;
    }

    private JPanel creerParams(double t0, double tf, double alpha, int iter, int stag) {
        JPanel s = section("Parametres");
        champT0 = champ(String.valueOf(t0));
        champTf = champ(String.valueOf(tf));
        champAlpha = champ(String.valueOf(alpha));
        champIter = champ(String.valueOf(iter));
        champStag = champ(String.valueOf(stag));
        champFreq = champ("50");
        s.add(ligne("T0 (Temperature initiale)", champT0));
        s.add(Box.createVerticalStrut(8));
        s.add(ligne("Tf (Temperature finale)", champTf));
        s.add(Box.createVerticalStrut(8));
        s.add(ligne("Alpha (Refroidissement)", champAlpha));
        s.add(Box.createVerticalStrut(8));
        s.add(ligne("Iterations par palier", champIter));
        s.add(Box.createVerticalStrut(8));
        s.add(ligne("Arret si stagnation", champStag));
        s.add(Box.createVerticalStrut(8));
        s.add(ligne("Frequence affichage", champFreq));
        return s;
    }

    private JPanel creerStats() {
        JPanel s = section("Statistiques");
        lblCout = info("Cout actuel", "---");
        lblTemp = info("Temperature", "---");
        lblIter = info("Iteration", "---");
        lblVeh = info("Vehicules", "---");
        s.add(wrap(lblCout));
        s.add(wrap(lblTemp));
        s.add(wrap(lblIter));
        s.add(wrap(lblVeh));
        return s;
    }

    private JPanel creerControle() {
        JPanel s = section("Controle");
        progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setString("En attente...");
        progress.setBackground(PANNEAU);
        progress.setForeground(ACCENT);
        progress.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        btnStart = new JButton("Demarrer l'optimisation");
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnStart.setBackground(ACCENT);
        btnStart.setForeground(Color.WHITE);
        btnStart.setFocusPainted(false);
        btnStart.setOpaque(true);
        btnStart.setContentAreaFilled(true);
        btnStart.setBorderPainted(false);
        btnStart.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        btnStart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStart.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        btnStart.addActionListener(e -> lancerOptimisation());

        s.add(progress);
        s.add(Box.createVerticalStrut(15));
        s.add(btnStart);
        return s;
    }

    private JPanel creerLegende() {
        JPanel s = section("Legende");
        s.add(legendeItem("Depot", new Color(231, 76, 60)));
        s.add(legendeItem("Clients", new Color(52, 73, 94)));
        s.add(legendeItem("Routes", ACCENT));
        return s;
    }

    private JPanel creerLog() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(FOND);
        p.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        p.setPreferredSize(new Dimension(0, 180));

        JLabel titre = new JLabel("  Journal");
        titre.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titre.setForeground(TITRE);
        titre.setOpaque(true);
        titre.setBackground(PANNEAU);
        titre.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        p.add(titre, BorderLayout.NORTH);

        log = new JTextArea();
        log.setEditable(false);
        log.setFont(new Font("Consolas", Font.PLAIN, 11));
        log.setBackground(new Color(30, 39, 46));
        log.setForeground(TEXTE);
        log.setCaretColor(ACCENT);
        JScrollPane sp = new JScrollPane(log);
        sp.setBorder(BorderFactory.createLineBorder(new Color(80, 90, 100)));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    // --- Validation et lancement ---

    private void lancerOptimisation() {
        if (enCours || ecouteur == null)
            return;
        try {
            double t0 = Double.parseDouble(champT0.getText().trim());
            double tf = Double.parseDouble(champTf.getText().trim());
            double alpha = Double.parseDouble(champAlpha.getText().trim());
            int iter = Integer.parseInt(champIter.getText().trim());
            int stag = Integer.parseInt(champStag.getText().trim());
            int freq = Integer.parseInt(champFreq.getText().trim());

            if (t0 <= 0 || tf <= 0 || t0 <= tf) {
                erreur("T0 > Tf > 0");
                return;
            }
            if (alpha < 0.1 || alpha > 0.99) {
                erreur("Alpha entre 0.1 et 0.99");
                return;
            }
            if (iter <= 0 || stag <= 0) {
                erreur("Iterations et stagnation > 0");
                return;
            }

            ecouteur.onDemarrer(t0, tf, alpha, iter, stag, freq);
        } catch (NumberFormatException ex) {
            erreur("Verifiez les valeurs numeriques.");
        }
    }

    private void erreur(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    // --- Composants UI ---

    private JPanel section(String titre) {
        JPanel s = new JPanel();
        s.setLayout(new BoxLayout(s, BoxLayout.Y_AXIS));
        s.setBackground(PANNEAU);
        s.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 100), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel l = new JLabel(titre, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TITRE);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        s.add(l);
        s.add(Box.createVerticalStrut(10));
        return s;
    }

    private JTextField champ(String val) {
        JTextField c = new JTextField(val);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.setPreferredSize(new Dimension(100, 28));
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        c.setBackground(new Color(40, 48, 52));
        c.setForeground(TEXTE);
        c.setCaretColor(ACCENT);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 100)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        return c;
    }

    private JPanel ligne(String label, JTextField champ) {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setBackground(PANNEAU);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(TEXTE);
        p.add(l, BorderLayout.CENTER);
        p.add(champ, BorderLayout.EAST);
        return p;
    }

    private JLabel info(String titre, String val) {
        JLabel l = new JLabel(html(titre, val));
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return l;
    }

    private JPanel wrap(JLabel label) {
        JPanel w = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        w.setBackground(PANNEAU);
        w.add(label);
        w.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        return w;
    }

    private JPanel legendeItem(String texte, Color couleur) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        p.setBackground(PANNEAU);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JPanel carre = new JPanel();
        carre.setPreferredSize(new Dimension(12, 12));
        carre.setBackground(couleur);
        carre.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));
        JLabel l = new JLabel(texte);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(TEXTE);
        p.add(carre);
        p.add(l);
        return p;
    }
}
