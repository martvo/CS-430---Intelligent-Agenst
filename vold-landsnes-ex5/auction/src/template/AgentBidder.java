package template;

import java.util.ArrayList;
import java.util.List;

import logist.agent.Agent;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;

public class AgentBidder {
	
	private ChooseNeighbours cn;
	private LocalChoice lc;
	private List<Vehicle> vehicles;

	public AgentBidder(List<Vehicle> vehicles) {
		cn = new ChooseNeighbours();
        lc = new LocalChoice();
        this.vehicles = vehicles;
	}
	
	
	public Solution getNewBestPlan(Task task, long timeout_bid, List<Task> task_list, List<Vehicle> vehicle_list, int tasks_auctioned) {
		long time_start = System.currentTimeMillis();
		Long bid = null;
		
		// Create a new task list containing the new task
		List<Task> possible_task_list = new ArrayList<Task>();
		for (Task t : task_list) {
			possible_task_list.add(t);
		}
		possible_task_list.add(task);
		
		Solution newSolution = createNewBestPlan(timeout_bid, time_start, possible_task_list, vehicle_list, tasks_auctioned);
		return newSolution;
	}
	
	
	public Solution createNewBestPlan(long timeout, long time_start, List<Task> task_list, List<Vehicle> vehicle_list, int tasks_auctioned) {
		Solution solution = new Solution(vehicle_list, task_list, tasks_auctioned);
		
		Solution best_solution = new Solution(solution);
		best_solution.build_plan(vehicle_list, task_list, tasks_auctioned);
		
		// Get the best solution for every run of the SLS algorithm
		double counter = 0;
		double init_cost = solution.get_cost_of_solution();
		for (int i = 0; i < 999999999; i++) {
			// System.out.println("Iteration " + (i + 1));

			solution = stochastic_local_search(solution, vehicle_list, task_list, counter, tasks_auctioned);
			
			if (best_solution.get_cost_of_solution() > solution.get_cost_of_solution()) {
				best_solution = new Solution(solution);
				best_solution.build_plan(vehicle_list, task_list, tasks_auctioned);
			}

			counter += 1;
			if (System.currentTimeMillis() - time_start > timeout) {
				break;
			}
		}
		System.out.println("Initial cost = " + init_cost);
		long duration = System.currentTimeMillis() - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		System.out.println("The best plan has a cost of: " + best_solution.get_cost_of_solution() +  "! and is:");
		for (Vehicle v : vehicle_list) {
			System.out.println(best_solution.get_action_task_list().get(v));
		}
		
		return best_solution;
	}
	
	// Stochastic local search from the book
	public Solution stochastic_local_search(Solution solution, List<Vehicle> v_list, List<Task> task_list, double temp, int tasks_auctioned) {
		Solution old_s = solution;
		old_s.get_action_task_list();
		List<Solution> neighbours = cn.getNeighbours(old_s, v_list, task_list, temp, tasks_auctioned);
		Solution best_solution_from_neighbours = lc.getBestSolution(neighbours, old_s, temp);
		return best_solution_from_neighbours;
	}

}
