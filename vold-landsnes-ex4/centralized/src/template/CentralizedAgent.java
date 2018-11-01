package template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

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

public class CentralizedAgent implements CentralizedBehavior {
	
	private Topology topology;
    private TaskDistribution distribution;
    private Agent agent;
    private long timeout_setup;
    private long timeout_plan;
    private Random random;
	
	private List<Plan> best_solution;
	

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
        
		// Define X, D, C and f
		
		// then do:
		// best_solution = select_initial_solution();		
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
			task_list.add(t);
		}
		COPSolution solution = new COPSolution(vehicles, task_list);
		int a = 1;
		
		// Dette skal være i en while loop*******************
		solution = stochastic_local_search(solution, vehicles, tasks);
		
		long duration = System.currentTimeMillis() - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		return solution.get_plans();
	}
	
	
	// Stochastic local search from the book
	public COPSolution stochastic_local_search(COPSolution solution, List<Vehicle> v_list, TaskSet tasks) {
		// Dette skal være inne i en while loop**********************
		COPSolution old_s = solution;
		Set<COPSolution> neighbours = chooseNeighbours(old_s, v_list, tasks);
		return null;
	}
	
	
	public Set<COPSolution> chooseNeighbours(COPSolution A, List<Vehicle> v_list, TaskSet tasks) {
		Random r = new Random();
		Set<COPSolution> neighbour_set = new TreeSet<COPSolution>();
		
		// Choose one of the possible vehicles. A possible vehicle is one that don't have a plan with a distance of 0.0
		List<Integer> possible_indexes = A.get_index_of_possible_next_vechile();
		int random_index = r.nextInt((possible_indexes.size() - 0) + 1) + 0;
		Vehicle chosen_vehicle = v_list.get(random_index);
		
		// This loop is for changing a task between vehicles
		for (int i = 0; i < v_list.size(); i++) {
			Vehicle other_v = v_list.get(i);
			if (!chosen_vehicle.equals(other_v)) {
				// Bør vell lage en kopi av nåværenede solution hær! så jeg kan endre på den så my som jeg vil uten at det påvirker den vi har hær....
				COPSolution s = new COPSolution(null, null);  // Må kunne lage en ny instans med egne lister!!
				
				// Choose a random task from the plan of the chosen_vehicle to remove
				Task task = choose_random_task(s.get_plans().get(random_index));
				
				// Check if this task fits the other_v vehicle
			}
		}
		return null;
	}
	
	
	public Task choose_random_task(Plan plan) {
		
		return null;
	}

}
