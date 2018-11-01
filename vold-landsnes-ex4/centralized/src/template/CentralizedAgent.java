package template;

import java.util.ArrayList;
import java.util.List;

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
		
		long duration = System.currentTimeMillis() - time_start;
		System.out.println("The plan was generated in " + duration + " milliseconds.");
		return solution.get_plans();
	}

}
