package template;

import java.util.ArrayList;
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
	
	private List<Task> task_list;
	
	private ChooseNeighbours cn;
	private LocalChoice lc;
	
	
	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		
		// this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config//settings_default.xml");
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
        
        cn = new ChooseNeighbours();
        lc = new LocalChoice();
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
		
		COPSolution best_solution = new COPSolution(solution);
		best_solution.build_plan(vehicles, task_list);
		
		// Get the best solution for every run of the SLS algorithm
		double counter = 0;
		double init_cost = solution.get_cost_of_solution();
		for (int i = 0; i < 999999999; i++) {
			System.out.println("Iteration " + (i + 1));

			solution = stochastic_local_search(solution, vehicles, task_list, counter);
			
			if (best_solution.get_cost_of_solution() > solution.get_cost_of_solution()) {
				best_solution = new COPSolution(solution);
				best_solution.build_plan(vehicles, task_list);
			}

			counter += 1;
			if (System.currentTimeMillis() - time_start > timeout_plan) {
				break;
			}
		}
		System.out.println("Initial cost = " + init_cost);
		long duration = System.currentTimeMillis() - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		System.out.println("The best plan has a cost of: " + best_solution.get_cost_of_solution() +  "! and is:");
		for (Vehicle v : vehicles) {
			System.out.println(best_solution.get_action_task_list().get(v));
		}
		
		return best_solution.get_plans();
	}
	
	
	// Stochastic local search from the book
	public COPSolution stochastic_local_search(COPSolution solution, List<Vehicle> v_list, List<Task> task_list, double temp) {
		COPSolution old_s = solution;
		List<COPSolution> neighbours = cn.getNeighbours(old_s, v_list, task_list, temp);
		COPSolution best_solution_from_neighbours = lc.getBestSolution(neighbours, old_s, temp);
		return best_solution_from_neighbours;
	}

}
