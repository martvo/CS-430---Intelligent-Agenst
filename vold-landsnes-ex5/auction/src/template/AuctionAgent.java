package template;

import java.awt.Label;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.management.timer.TimerNotification;

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
	private Solution adversary_current_solution;
	private Solution adversary_future_solution;
	private List<Task> task_list;
	private List<Task> adversary_task_list;
	private List<Vehicle> adverseri_vehicles;
	
	private int income = 0;
	private long initial_low_bid = 0;
	private int tasks_auctioned = 0;
	private double extra_bid = 0.0;
	private double learning_rate = 0.2;
	
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
        
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.current_city = vehicle.homeCity();
		
		this.bidder = new AgentBidder(agent.vehicles());
		this.task_list = new ArrayList<Task>();
		
		long seed = 12345;
		this.random = new Random(seed);
        
        // Create initial solution with a empty task list
        current_solution = new Solution(agent.vehicles(), new ArrayList<Task>(), 0);
        
        // Create vehicles for adversary and make his plan
        adversary_task_list = new ArrayList<Task>();
        double our_capacity = 0;
        double our_costPerKm = 0;
        for (Vehicle v : agent.vehicles()) {
        	our_capacity += v.capacity();
        	our_costPerKm += v.costPerKm();
        }
        
        adverseri_vehicles = new ArrayList<Vehicle>();
        for (int i = 0; i < agent.vehicles().size(); i++) {
        	City adverseri_city = topology.cities().get(random.nextInt(topology.size()));
        	adverseri_vehicles.add(new AdverseriVehicle((int) Math.ceil(our_capacity/agent.vehicles().size()), 
        			(int) Math.floor(our_costPerKm/agent.vehicles().size()), adverseri_city));
        }
        
        adversary_current_solution = new Solution(adverseri_vehicles, adversary_task_list, 0);
        
        // Create a list of all possible task in the topology, they can be used to calculate the expected value of 
        // getting future tasks or to get a estimated cost for our plan if we get the first 10 tasks
        List<Task> possible_tasks_in_topology = new ArrayList<Task>();
        int task_id = 0;
        for (City from_city : topology.cities()) {
        	for (City to_city : topology.cities()) {
        		possible_tasks_in_topology.add(new Task(task_id++, from_city, to_city, 
        				distribution.reward(from_city, to_city), distribution.weight(from_city, to_city)));
        	}
        } 
        int number_of_random_tasks = 15;
        int number_of_samples = 3;
        List<Task> task_to_estimate = new ArrayList<Task>();
        for (int i = 0; i < number_of_samples; i++) {
        	for (int j = 0; j < number_of_random_tasks; j++) {
            	// Choose a random task
            	Task rand_task = possible_tasks_in_topology.get(random.nextInt(possible_tasks_in_topology.size()));
            	task_to_estimate.add(rand_task);
            }
        	long start_time_setup = System.currentTimeMillis();
        	Solution new_estimated_solution = this.bidder.createNewBestPlan((long) (this.timeout_setup * (0.95/number_of_samples)), start_time_setup, task_to_estimate, adverseri_vehicles, possible_tasks_in_topology.size());
        	initial_low_bid += (long) (new_estimated_solution.get_cost_of_solution() / number_of_random_tasks);
        }
        
        initial_low_bid = initial_low_bid / number_of_samples;
        System.out.println("Low bid is: " + initial_low_bid);
	}

	
	@Override
	public Long askPrice(Task task) {
		// Our future plan
		tasks_auctioned++;
		future_solution = this.bidder.getNewBestPlan(task, (long) (timeout_bid * 0.47), task_list, agent.vehicles(), tasks_auctioned);
		double marginalCost = Math.max(future_solution.get_cost_of_solution() - current_solution.get_cost_of_solution(), 0);
		
		// Adversaries future plan
		adversary_future_solution = this.bidder.getNewBestPlan(task, (long) (timeout_bid * 0.47), adversary_task_list, adverseri_vehicles, tasks_auctioned);
		double adversary_marginalcost = Math.max(adversary_future_solution.get_cost_of_solution() - adversary_current_solution.get_cost_of_solution(), 0);
		
		// Create bid based on estimated adversary bid 
		double marginal_bid = 0.5 * (marginalCost + adversary_marginalcost) + extra_bid;
		
		// Combine the initial bid with the marginal bid
		double weight = weight_function(tasks_auctioned);
		long bid = (long) (weight * initial_low_bid + (1 - weight) * marginal_bid);
		return bid;
	}
	
	
	public double weight_function(int interation) {
		return 1 / (1 + Math.exp(interation - 8));
	}

	
	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		System.out.println("Last winner was agent " + lastWinner + " with a bid of " + lastOffers[lastWinner]);
		if (lastWinner == agent.id()) {
			System.out.println("Won the task");
			task_list.add(lastTask);
			income += lastOffers[agent.id()];
			current_solution = future_solution;
			double lowers_bid_that_is_not_ours = Double.POSITIVE_INFINITY;;
			for (int i = 0; i < lastOffers.length; i++) {
				if (i != agent.id() && lastOffers[i] < lowers_bid_that_is_not_ours) {
					lowers_bid_that_is_not_ours = lastOffers[i];
				}
			}
			
			// want to increase the bid 
			extra_bid += learning_rate * (lowers_bid_that_is_not_ours - lastOffers[lastWinner]);
		} else {
			System.out.println("Lost the task");
			adversary_current_solution = adversary_future_solution;
			
			double our_bid = 0;
			for (int i = 0; i < lastOffers.length; i++) {
				if (i == agent.id()) {
					our_bid = lastOffers[i];
				}
			}
			
			// want to decrease the bid
			extra_bid += learning_rate * (lastOffers[lastWinner] - our_bid);
		}
		System.out.println();
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
