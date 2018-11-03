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
	private HashMap<Task, Integer> tasks_map;
	private HashMap<Integer, Task> int_to_task;
	

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
        
        tasks_map = new HashMap<Task, Integer>();
		int_to_task = new HashMap<Integer, Task>();	
	}
	
	
	public List<Plan> select_initial_solution() {
		return null;
	}
	

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		// This method gets called after setup is done
		long time_start = System.currentTimeMillis();
		
		List<Task> task_list = new ArrayList<Task>();
		for (Task t : tasks) {
			tasks_map.put(t, t.id);
			int_to_task.put(t.id, t);
			task_list.add(t);
		}
		COPSolution solution = new COPSolution(vehicles, task_list);
		
		// Get the best solution for every run of the SLS algorithm
		for (int i = 0; i < 2; i++) {
			System.out.println("Iteration " + (i + 1));
			System.out.println("Initial total distance for current solution = " + solution.get_cost_of_solution());
			solution = stochastic_local_search(solution, vehicles, tasks, tasks_map, int_to_task);
			System.out.println();
			
			if (System.currentTimeMillis() - time_start > timeout_plan - 290000) {
				break;
			}
		}
		
		long duration = System.currentTimeMillis() - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		System.out.println("The number of plans: " + solution.get_plans().size());
		System.out.println("The plan is:");
		for (Vehicle v : vehicles) {
		// for (int i = 0; i < vehicles.size(); i++) {
			System.out.println(solution.get_action_task_list().get(v));
		}
		System.out.println(task_list);
		return solution.get_plans();
	}
	
	
	// Stochastic local search from the book
	public COPSolution stochastic_local_search(COPSolution solution, List<Vehicle> v_list, TaskSet tasks, HashMap<Task, Integer> tm_map, HashMap<Integer, Task> it_map) {
		COPSolution old_s = solution;
		List<COPSolution> neighbours = ChooseNeighbours.getNeighbours(old_s, v_list, tasks, tm_map, it_map);
		System.out.println("neighbours size in SLS is " + neighbours.size());
		COPSolution best_solution_from_neighbours = LocalChoice.getBestSolution(neighbours, old_s);
		return best_solution_from_neighbours;
	}

}
