package ihm;

import metier.DonneesVRP;
import metier.Solution;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panneau de visualisation graphique des tournees VRP.
 */
public class VRPPanel extends JPanel {

    private DonneesVRP donnees;
    private Solution solution;

    private static final Color[] COULEURS = {
            new Color(255, 107, 107), new Color(78, 205, 196),
            new Color(255, 230, 109), new Color(199, 128, 232),
            new Color(255, 159, 67), new Color(116, 185, 255),
            new Color(162, 217, 206), new Color(255, 177, 193)
    };

    public VRPPanel(DonneesVRP donnees) {
        this.donnees = donnees;
    }

    public void setSolution(Solution s) {
        this.solution = s;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (solution == null || donnees.getXCoords() == null)
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight(), m = 50;
        double[] xc = donnees.getXCoords(), yc = donnees.getYCoords();
        int n = donnees.getNbClients();

        // Echelle
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i <= n; i++) {
            minX = Math.min(minX, xc[i]);
            maxX = Math.max(maxX, xc[i]);
            minY = Math.min(minY, yc[i]);
            maxY = Math.max(maxY, yc[i]);
        }
        double sc = Math.min((w - 2.0 * m) / (maxX - minX),
                (h - 2.0 * m) / (maxY - minY));

        // Routes
        for (int i = 0; i < solution.tournees.size(); i++) {
            List<Integer> route = solution.tournees.get(i);
            if (route.isEmpty())
                continue;
            g2.setColor(COULEURS[i % COULEURS.length]);
            g2.setStroke(new BasicStroke(3));
            int px = tx(xc[0], minX, sc, m), py = ty(yc[0], minY, sc, h, m);
            for (int c : route) {
                int cx = tx(xc[c], minX, sc, m), cy = ty(yc[c], minY, sc, h, m);
                g2.drawLine(px, py, cx, cy);
                px = cx;
                py = cy;
            }
            g2.drawLine(px, py, tx(xc[0], minX, sc, m), ty(yc[0], minY, sc, h, m));
        }

        // Clients
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        for (int i = 1; i <= n; i++) {
            int x = tx(xc[i], minX, sc, m), y = ty(yc[i], minY, sc, h, m);
            g2.setColor(new Color(52, 73, 94));
            g2.fillOval(x - 12, y - 12, 24, 24);
            g2.setColor(Color.WHITE);
            g2.drawOval(x - 12, y - 12, 24, 24);
            String s = String.valueOf(i);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(s, x - fm.stringWidth(s) / 2, y + fm.getAscent() / 2 - 2);
        }

        // Depot
        int dx = tx(xc[0], minX, sc, m), dy = ty(yc[0], minY, sc, h, m);
        g2.setColor(new Color(231, 76, 60));
        g2.fillRect(dx - 8, dy - 8, 16, 16);
        g2.setColor(Color.WHITE);
        g2.drawRect(dx - 8, dy - 8, 16, 16);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("DEPOT", dx + 12, dy + 4);

        // Cout
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(46, 204, 113));
        g2.drawString("Cout: " + String.format("%.2f", solution.cout), 15, 25);
    }

    private int tx(double v, double min, double sc, int m) {
        return (int) (m + (v - min) * sc);
    }

    private int ty(double v, double min, double sc, int h, int m) {
        return (int) (h - m - (v - min) * sc);
    }
}
