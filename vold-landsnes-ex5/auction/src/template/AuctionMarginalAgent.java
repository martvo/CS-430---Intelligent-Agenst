package template;

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

public class AuctionMarginalAgent implements AuctionBehavior {
	
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private AgentBidder bidder;
	private Solution currentSolution;
	private Solution futureSolution;
	private List<Task> task_list;
	private int income = 0;
	private int tasks_auctioned = 0;
	
	private long timeout_bid;
    private long timeout_plan;
    private long timeout_setup;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// TODO Auto-generated method stub
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		
		this.bidder = new AgentBidder(agent.vehicles());
		this.task_list = new ArrayList<Task>();
		
		// Do we need this??
		long seed = 12345;
		this.random = new Random(seed);
		
		// this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config//settings_auction.xml");
        }
        catch (Exception exc) {
            System.out.println("There was a problem loading the configuration file.");
        }
        
        // Create initial solution with a empty task list
        currentSolution = new Solution(agent.vehicles(), new ArrayList<Task>(), 0);
        
        // the setup method cannot last more than timeout_setup milliseconds
        timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
        // the setup method cannot last more than timeout_bid milliseconds
        timeout_bid = ls.get(LogistSettings.TimeoutKey.BID);
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
	}

	@Override
	public Long askPrice(Task task) {
		tasks_auctioned++;
		futureSolution = this.bidder.getNewBestPlan(task, (long) (timeout_bid * 0.95), task_list, agent.vehicles(), tasks_auctioned);
		Long marginalCost = (long) (futureSolution.get_cost_of_solution() - currentSolution.get_cost_of_solution());
		return marginalCost;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		System.out.println("Last winner was agent " + lastWinner + " with a bid of " + lastOffers[lastWinner]);
		if (lastWinner == agent.id()) {
			task_list.add(lastTask);
			income += lastOffers[agent.id()];
			currentSolution = futureSolution;
		}
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long start_time = System.currentTimeMillis();
		// For some reason we have to use the tasks from the taskset and not the task we got from the askPrice function
		List<Task> new_task_list = new ArrayList<Task>();
		for (Task t : tasks) {
			new_task_list.add(t);
		}
		System.out.println("\n\n\n");
		System.out.println("The income for this auction was: " + income);
		return this.bidder.createNewBestPlan((long) (this.timeout_plan * 0.95), start_time, new_task_list, agent.vehicles(), tasks_auctioned).get_plans();
	}

}
