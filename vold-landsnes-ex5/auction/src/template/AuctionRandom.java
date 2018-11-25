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

public class AuctionRandom implements AuctionBehavior {
	
	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private AgentBidder bidder;
	private long timeout_plan;
	private long income = 0;
	private int tasks_auctioned;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		this.bidder = new AgentBidder(agent.vehicles());
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
        
        // the plan method cannot execute more than timeout_plan milliseconds
        timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);
        
	}

	@Override
	public Long askPrice(Task task) {
		tasks_auctioned++;
		return (long) random.nextInt(1500) + 500;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		if (lastWinner == agent.id()) {
			income += lastOffers[agent.id()];
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
