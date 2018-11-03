package template;

import java.util.List;
import java.util.Random;

import logist.plan.Plan;

public class LocalChoice {
	
	static double p = 0.0;  // Probability of returning the old_A
	
	public static COPSolution getBestSolution(List<COPSolution> neighbours, COPSolution old_A) {
		if (neighbours.size() == 0) {
			System.out.println("No neighbours to choose from......");
			return old_A;
		}
		
		Random r = new Random();
		COPSolution best = neighbours.get(0);
		
		double random_double = r.nextDouble();
		
		if (p >= random_double) {  // Returns old_A
			System.out.println("Chose old A");
			return old_A;
		} else {  // Returns the best neighbor
			for (COPSolution c : neighbours) {
				if (best.get_cost_of_solution() > c.get_cost_of_solution()) {
					best = c;
				}
			}
		}
		System.out.println("Cost of best solution after local choice=" + best.get_cost_of_solution());
		return best;
	}

}
