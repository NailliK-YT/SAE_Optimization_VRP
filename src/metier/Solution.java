package metier;

import java.util.ArrayList;
import java.util.List;

/**
 * Represente une solution VRP : un ensemble de tournees et leur cout total.
 */
public class Solution {

    public List<List<Integer>> tournees;
    public double cout;

    public Solution(int nbVehicules) {
        this.tournees = new ArrayList<>();
        for (int i = 0; i < nbVehicules; i++)
            this.tournees.add(new ArrayList<>());
        this.cout = Double.MAX_VALUE;
    }

    /**
     * Retourne une copie profonde de la solution.
     */
    public Solution copier() {
        Solution copie = new Solution(tournees.size());
        for (int i = 0; i < tournees.size(); i++)
            copie.tournees.set(i, new ArrayList<>(tournees.get(i)));
        copie.cout = this.cout;
        return copie;
    }
}
