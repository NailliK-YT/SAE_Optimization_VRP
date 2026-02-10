package metier;

import java.util.ArrayList;
import java.util.List;

/**
 * Represente une solution du VRP (ensemble de tournees)
 */
public class Solution {
    public List<List<Integer>> tournees;
    public double cout;

    public Solution(int nbVehicules) {
        tournees = new ArrayList<>();
        for (int i = 0; i < nbVehicules; i++) {
            tournees.add(new ArrayList<>());
        }
        cout = Double.MAX_VALUE;
    }

    public Solution copier() {
        Solution copie = new Solution(tournees.size());
        for (int i = 0; i < tournees.size(); i++) {
            copie.tournees.set(i, new ArrayList<>(tournees.get(i)));
        }
        copie.cout = this.cout;
        return copie;
    }
}
