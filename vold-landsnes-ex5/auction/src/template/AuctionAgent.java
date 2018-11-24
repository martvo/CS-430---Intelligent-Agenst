package template;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class AuctionAgent implements AuctionBehavior {
	
	
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City current_city;
	private AgentBidder bidder;
	private Solution current_solution;
	private Solution future_solution;
	private Solution adverseri_current_solution;
	private Solution adverseri_future_solution;
	private List<Task> task_list;
	private int income = 0;
	
	private long timeout_bid;
    private long timeout_plan;
    private long timeout_setup;
    

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config//settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
		 // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the setup method cannot last more than timeout_bid milliseconds
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
		long time_start_setup = System.currentTimeMillis();
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.current_city = vehicle.homeCity();
		
		this.bidder = new AgentBidder(agent.vehicles());
		this.task_list = new ArrayList<Task>();
		
		// Do we need this??
		long seed = -9019554669489983951L * current_city.hashCode() * agent.id();
		this.random = new Random(seed);
        
        // Create initial solution with a empty task list
        current_solution = new Solution(agent.vehicles(), new ArrayList<Task>());
        
        // Create vehicles for adverseri and make his plan
        double our_capacity = 0;
        double our_costPerKm = 0;
        for (Vehicle v : agent.vehicles()) {
        	our_capacity += v.capacity() / agent.vehicles().size();
        	our_costPerKm += v.costPerKm() / agent.vehicles().size();
        }
        List<Vehicle> adverseri_vehicles = new ArrayList<Vehicle>();
        for (int i = 0; i < agent.vehicles().size(); i++) {
        	City adverseri_city = topology.cities().get(random.nextInt(Integer.MAX_VALUE) % topology.size());
        	adverseri_vehicles.add(new AdverseriVehicle((int) Math.ceil(our_capacity), (int) Math.floor(our_costPerKm), adverseri_city));
        }
        
        Solution adverseri_current_solution = new Solution(adverseri_vehicles, new ArrayList<Task>());
	}

	@Override
	public Long askPrice(Task task) {
		long time_start = System.currentTimeMillis();
		future_solution = this.bidder.getNewBestPlan(task, time_start, timeout_bid, task_list);
		Long marginalCost = (long) (future_solution.get_cost_of_solution() - current_solution.get_cost_of_solution());
		return Math.max(marginalCost, 0);
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		System.out.println("Last winner was agent " + lastWinner + " with a bid of " + lastOffers[lastWinner]);
		if (lastWinner == agent.id()) {
			System.out.println("Won the task");
			task_list.add(lastTask);
			income += lastOffers[agent.id()];
			current_solution = future_solution;
		} else {
			System.out.println("Lost the task");
			adverseri_future_solution = adverseri_current_solution;
		}
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		// For some reason we have to use the tasks from the taskset and not the task we got from the askPrice function
		List<Task> new_task_list = new ArrayList<Task>();
		for (Task t : tasks) {
			new_task_list.add(t);
		}
		System.out.println("\n\n\n");
		System.out.println("The income for this auction was: " + income);
		return this.bidder.createNewBestPlan(this.timeout_plan, System.currentTimeMillis(), new_task_list).get_plans();
	}

}
