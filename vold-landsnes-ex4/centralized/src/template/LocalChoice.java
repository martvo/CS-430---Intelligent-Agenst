package template;

import java.util.List;
import java.util.Random;

public class LocalChoice {
	
	Random r = new Random(1234);
	private double p = 0.3;  // Probability of returning the old_A
	
	public  COPSolution getBestSolution(List<COPSolution> neighbours, COPSolution old_A, double temp) {
		if (neighbours.size() == 0) {
			System.out.println("No neighbours to choose from......");
			return old_A;
		}
		
		COPSolution best = neighbours.get(0);
		
		double random_double = random_double();
		double t = random_double();
		
		if (p >= random_double) {  // Returns old_A
			System.out.println("Chose old A");
			return old_A;
		} else {  // Returns the best neighbor
			if ((1/Math.sqrt(temp)) >= t) {
				System.out.println("Choose a random neighbour");
				return neighbours.get(random_int(neighbours.size()));
			} else {
				for (COPSolution c : neighbours) {
					if (best.get_cost_of_solution() > c.get_cost_of_solution()) {
						best = c;
					}
				}
			}
		}
		System.out.println("Cost of best solution after local choice=" + best.get_cost_of_solution());
		return best;
	}
	
	
	public int random_int(int i){
        return r.nextInt(i);
    }
	
	
	public double random_double() {
		return r.nextDouble();
	}

}
