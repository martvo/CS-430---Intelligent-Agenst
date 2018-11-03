package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.CentralizedBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

import template.ChooseNeighbours;

public class CentralizedAgent implements CentralizedBehavior {
	
	private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private Random random;
	
	private List<Plan> best_solution;
	private List<Task> task_list;
	
	
	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		
		// this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config\\settings_default.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
        this.topology = topology;
        this.distribution = distribution;
        this.agent = agent;
	}
	
	
	public List<Plan> select_initial_solution() {
		return null;
	}
	

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		// This method gets called after setup is done
		long time_start = System.currentTimeMillis();
		
		task_list = new ArrayList<Task>();
		// Build the task list
		for (Task t : tasks) {
			task_list.add(t);
		}
		
		COPSolution solution = new COPSolution(vehicles, task_list);
		
		System.out.println("Initial cost is: " + solution.get_cost_of_solution());
		
		// Get the best solution for every run of the SLS algorithm
		int counter = 0;
		for (int i = 0; i < 999999999; i++) {
			System.out.println("Iteration " + (i + 1));
			// System.out.println("Initial total distance for current solution = " + solution.get_cost_of_solution());
			solution = stochastic_local_search(solution, vehicles, task_list, counter);
			for (Vehicle v : vehicles) {
				System.out.println(solution.get_action_task_list().get(v));
			}
			System.out.println();
			counter++;
			if (System.currentTimeMillis() - time_start > timeout_plan - 100000) {
				break;
			}
		}
		
		long duration = System.currentTimeMillis() - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		System.out.println("The plan has a cost of: " + solution.get_cost_of_solution() +  "! and is:");
		for (Vehicle v : vehicles) {
			System.out.println(solution.get_action_task_list().get(v));
		}
		
		return solution.get_plans();
	}
	
	
	// Stochastic local search from the book
	public COPSolution stochastic_local_search(COPSolution solution, List<Vehicle> v_list, List<Task> task_list, int counter) {
		COPSolution old_s = solution;
		List<COPSolution> neighbours = ChooseNeighbours.getNeighbours(old_s, v_list, task_list, counter);
		// System.out.println("neighbours size in SLS is " + neighbours.size());
		COPSolution best_solution_from_neighbours = LocalChoice.getBestSolution(neighbours, old_s);
		return best_solution_from_neighbours;
	}

}
