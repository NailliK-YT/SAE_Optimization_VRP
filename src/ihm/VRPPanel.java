package ihm;

import metier.DonneesVRP;
import metier.Solution;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Panneau de visualisation du graphe VRP
 */
public class VRPPanel extends JPanel {

    private DonneesVRP donnees;
    private Solution solutionAVisualiser;

    public VRPPanel(DonneesVRP donnees) {
        this.donnees = donnees;
    }

    public void setSolution(Solution solution) {
        this.solutionAVisualiser = solution;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (solutionAVisualiser == null || donnees.getXCoords() == null)
            return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int margin = 50;

        double[] xCoords = donnees.getXCoords();
        double[] yCoords = donnees.getYCoords();
        int nbClients = donnees.getNbClients();

        // Trouver min/max pour mise a l'echelle
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

        // Couleurs vives pour les vehicules
        Color[] colors = {
                new Color(255, 107, 107),
                new Color(78, 205, 196),
                new Color(255, 230, 109),
                new Color(199, 128, 232),
                new Color(255, 159, 67),
                new Color(116, 185, 255),
                new Color(162, 217, 206),
                new Color(255, 177, 193)
        };

        for (int i = 0; i < solutionAVisualiser.tournees.size(); i++) {
            List<Integer> tournee = solutionAVisualiser.tournees.get(i);
            if (tournee.isEmpty())
                continue;

            g2.setColor(colors[i % colors.length]);
            g2.setStroke(new BasicStroke(3));

            int x1 = (int) (margin + (xCoords[0] - minX) * scale);
            int y1 = (int) (height - margin - (yCoords[0] - minY) * scale);

            int first = tournee.get(0);
            int x2 = (int) (margin + (xCoords[first] - minX) * scale);
            int y2 = (int) (height - margin - (yCoords[first] - minY) * scale);
            g2.drawLine(x1, y1, x2, y2);

            for (int j = 0; j < tournee.size() - 1; j++) {
                int c1 = tournee.get(j);
                int c2 = tournee.get(j + 1);
                x1 = (int) (margin + (xCoords[c1] - minX) * scale);
                y1 = (int) (height - margin - (yCoords[c1] - minY) * scale);
                x2 = (int) (margin + (xCoords[c2] - minX) * scale);
                y2 = (int) (height - margin - (yCoords[c2] - minY) * scale);
                g2.drawLine(x1, y1, x2, y2);
            }

            int last = tournee.get(tournee.size() - 1);
            x1 = (int) (margin + (xCoords[last] - minX) * scale);
            y1 = (int) (height - margin - (yCoords[last] - minY) * scale);
            x2 = (int) (margin + (xCoords[0] - minX) * scale);
            y2 = (int) (height - margin - (yCoords[0] - minY) * scale);
            g2.drawLine(x1, y1, x2, y2);
        }

        // Dessiner les clients
        g2.setFont(new Font("Arial", Font.BOLD, 10));
        for (int i = 1; i <= nbClients; i++) {
            int x = (int) (margin + (xCoords[i] - minX) * scale);
            int y = (int) (height - margin - (yCoords[i] - minY) * scale);

            int radius = 12;
            g2.setColor(new Color(52, 73, 94));
            g2.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            g2.setColor(Color.WHITE);
            g2.drawOval(x - radius, y - radius, radius * 2, radius * 2);
            String num = String.valueOf(i);
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(num);
            int textHeight = fm.getAscent();
            g2.drawString(num, x - textWidth / 2, y + textHeight / 2 - 2);
        }

        // Dessiner le depot
        int dx = (int) (margin + (xCoords[0] - minX) * scale);
        int dy = (int) (height - margin - (yCoords[0] - minY) * scale);
        g2.setColor(new Color(231, 76, 60));
        g2.fillRect(dx - 8, dy - 8, 16, 16);
        g2.setColor(Color.WHITE);
        g2.drawRect(dx - 8, dy - 8, 16, 16);
        g2.setFont(new Font("Arial", Font.BOLD, 12));
        g2.drawString("DEPOT", dx + 12, dy + 4);

        // Afficher cout
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        g2.setColor(new Color(46, 204, 113));
        g2.drawString("Cout: " + String.format("%.2f", solutionAVisualiser.cout), 15, 25);
    }
}
