package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.List;
import java.util.Map.Entry;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

import java.util.Random;

import logist.agent.Agent;
import logist.behavior.AuctionBehavior;
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
	private City currentCity;
	
	private List<Task> auctionedTasks;
	private ChooseNeighbours cn;
	private LocalChoice lc;
	
	//OUR AGENT
	private List<Task> ourTaskList; //Holds the tasks of our agent!  
	private COPSolution lastSolution; 
	private double lastCostOfPlan; //Updates the lastCostOfPlan every askPrice iteration (every aution) 
	
	//OTHER AGENT
	private otherAgent enemy; 
	private COPSolution enemyLastSolution; 
	private double enemyLastCostOfPlan;
	private int auctionCount; //vurder å dropp 
	
	//GLOBAL BID PERCENTAGES 
	double INITIAL_UNDERBID_TRESHOLD=-0.1;//if we lose 10 percent we won't bid  initially
	double INITIAL_UNDERBID_PERCENT=1.01+INITIAL_UNDERBID_TRESHOLD;	 //we bid for 1% better than underbid treshold
	double LOW_UNDERBID_TRESHOLD=-0.05; //random choice atm -> 5% LOSS AND WE STILL BID WHEN VERY FEW TASKS
	double LOW_UNDERBID_PERCENT=1.01+LOW_UNDERBID_TRESHOLD; //random choice atm 
	int	LOW_TASK_TRESHOLD_BID=3;
	
	
	
	class otherAgent{	//Retains the taskList and bidList of the other agent! 
		int agentId; 
		private List<Task> taskList; 
		private List<Long> bidList; 
		
		//first time constructor 
		otherAgent(int Id, List<Task> tasks, Long bids){
			this.agentId = Id; 
			this.taskList = tasks; 
			List<Long> currBidList = new ArrayList<Long>();
			currBidList.add(bids);
			this.bidList = currBidList;
		}
		
		otherAgent(otherAgent last_agent, Task task, Long bids){
			last_agent.taskList.add(task);
			this.taskList =last_agent.taskList;
			last_agent.bidList.add(bids);
			this.bidList = last_agent.bidList;
		}
		
		public otherAgent copyAgent(otherAgent last_agent) {
			this.agentId = last_agent.agentId;
			this.taskList = last_agent.taskList;
			this.bidList = last_agent.bidList;
			return this; 
		}
	}
	

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		// TODO Auto-generated method stub
		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();
		this.auctionedTasks = new ArrayList<Task>();
		this.ourTaskList = new ArrayList<Task>(); //Gets methods of both Lists and ArrayLists 
		this.lastCostOfPlan= Integer.MAX_VALUE; //initially super high cost -> Works with Integer?
		this.auctionCount = 0;
		
		//No need to initialize a COPSolution here as there are no tasks published anyways? 
		
		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
	}

	@Override
	public Long askPrice(Task task) {
		// Published task is input parameter, 
		 List<Task> tempTaskList = ourTaskList; 
		 tempTaskList.add(task);
		//if capacity is impossible -> bid null!
		 
		if(get_largest_vehicle_capacity(agent.vehicles())< task.weight){
			return null; 
		}
		
		//IF NO TASKS , bid low to get started (also consider using future auction utility to decide;how low.			
		if (ourTaskList.isEmpty()) { //can also add to check for small amount of tasks but add marginalCost 
			
			//Initial COPSolution 
			COPSolution newSolution = new COPSolution(agent.vehicles(), tempTaskList); 
			//Finding "best" future solution
			int counter = 0;
			for (int i = 1; i < 100; i++) {
				newSolution = stochastic_local_search(newSolution, agent.vehicles(), tempTaskList, counter);
				counter++;
			}
			
			
			double futureCostOfPlan = newSolution.get_cost_of_solution();			
			double futureReward = get_reward(tempTaskList);
			
			 //How much we will gain by taking on this task based on optimal cost routes.  //can make one for empty taskset whitout all factors, and another with this formula
			double margGain = (futureReward - futureCostOfPlan);
			
			double percentMargGain = margGain / futureReward;
			//If more than 10% future loss then choose what to do
			if( percentMargGain <= INITIAL_UNDERBID_TRESHOLD) {
				return null;
			}
			
			else {//DON'T CARE ABOUT ENEMY HERE, IF THEY'RE BIDDING LOWER THAN THIS THEY CAN'T HAVE GAIN > 0 ???? 
				
				double bid = margGain*INITIAL_UNDERBID_PERCENT; //Test factors				
				return (long) (bid);
				
			}
			
			
			
		}
		//OTHER OPTION IF WE HAVE FEW TASKS, BUT SOME -> TEST DIFFERENT TASK_TRESHOLDS
		else if(ourTaskList.size() < LOW_TASK_TRESHOLD_BID) {
			//to retrieve new taskset
			
			 
			//FIND BETTER CONSTRUCTOR SO RE-USING LAST SOLUTION AND ONLY CHANGING TASKSET..
			COPSolution newSolution = new COPSolution(agent.vehicles(), tempTaskList); 
			//Finding "best" future solution
			int counter = 0;
			for (int i = 1; i < 100; i++) {
				newSolution = stochastic_local_search(newSolution, agent.vehicles(), tempTaskList, counter);
				counter++;
			}
			
			
			double futureCostOfPlan = newSolution.get_cost_of_solution();
			double lastReward = get_reward(ourTaskList);
			double futureReward = get_reward(tempTaskList);
			
			 //How much we will gain by taking on this task based on optimal cost routes.  //can make one for empty taskset whitout all factors, and another with this formula
			double margGain = (lastReward - lastCostOfPlan) - (futureReward - futureCostOfPlan);
			
			double percentMargGain = margGain / (lastReward - lastCostOfPlan);
			
			if( percentMargGain <= LOW_UNDERBID_TRESHOLD) {//won't bid if below treshold
				return null;
			}
			
			else {//DON'T CARE ABOUT ENEMY HERE, IF THEY'RE BIDDING LOWER THAN THIS THEY CAN'T HAVE GAIN > 0 ???? 
				
				double bid = margGain*LOW_UNDERBID_PERCENT; //Test factors				
				return (long) (bid);
				
			}
		}
		
		//MORE TASKS THAN "FEW" TASKS -> NEW STRATEGY HERE LOOKING INTO MARGINAL COST! 
		
				
		// bid in the following manner:
		
			
			//if no prior offer information -> we have all former tasks, can't use enemy information
		else if (enemy.bidList.isEmpty()) {
			
			//COPSolution nextSolution = new COPSolution(lastSolution);
			//Don't start from scrath with nextSolution, use the last and just append the next task in taskSet!
			//bestSolution = stochastic_local_search(nextSolution, agent.vehicles(), ourTaskList,0);
			
		}
		
				//create basic COPSolution with new Taskset and run for best plan with best solution.
				//compare with old solution which is stored in temp variable in this class
		
				//calculate reinforcement utility with new taskSet and see if reward increases 
		
				//check oldReward vs. newReward(with new task) + increased future utility 
					
					//bid with: increase in reward(margReward) - increased utility.(never below margReward)
		
					//if bid is lower than margReward then return null (no possible gain here) 
		
		
			//if prior info and were in bid mode to not underbid anymore! 
		else {
			
		}
				//do same as last if-statement and also assess last bids somehow...???  
		
					//look into tasks other agents have 
					//calculate the margCost they should have -> 
						//other strategy -> Always bid super low? to take all tasks but still gain? 
					//take the lowest margCost and multiply with 0.99 as long as > margReward 
		
				//never bid negative according to enemy agents! 
		
		return null;
	}

	@Override
	public void auctionResult(Task lastTask, int lastWinner, Long[] lastOffers) {
		// TODO Auto-generated method stub
		auctionCount++;
		auctionedTasks.add(lastTask); // update the last auctioned task 
		
		//update OtherAgent class!
		if ( auctionCount == 1){ //first auction
			//Other agent 
			if(lastWinner != agent.id()) {
				List<Task> taskList = new ArrayList<Task>(); 
				taskList.add(lastTask);
				otherAgent enemy = new otherAgent(lastWinner, taskList, lastOffers[lastWinner]);
				 
				//create a COP solution for this agent ID with current tasks.(used later when bidding) ?????				
			}
			else if(lastWinner == agent.id()) {
				//Add one task for our agent...?? think more 
			}
			 
		}
		
		
		if(lastWinner != agent.id()) {
			//update enemy with other constructor than the above, 
			//update data-structure with previous offers! (all offers here)
			otherAgent last_enemy = enemy.copyAgent(enemy);  //IS THIS LEGAL COPYING????? 
			//updates bidList and taskList of enemy-agent 
			otherAgent enemy = new otherAgent(last_enemy,lastTask, lastOffers[lastWinner]); 
			
			//create a COP solution for this agent ID with current tasks.(used later when bidding) ?????
		}
		
		
		//if id is our agent 
		else if(lastWinner == agent.id()) {
			//update our agent!
			
			//add task to taskSet of agent 
			ourTaskList.add(lastTask);
			//consider looking into differenece with out bid and other agent's bid to assess ??? might be stupid as well!
			
			//lag heller ny copSolution som er privatvariabel, har da en gammel en + at man har en plan! 
			//COPSolution lastSolution = new COPSolution();//lastCOPSolution -> oppdaterer 			
			//create COPsolution plan with this taskSet
		
		}
		
		
		
			
		
		
			 
		
		
		
	}
/*
	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
		// Build plan as in centralized
		
		//Build plan here with latest decent plan from bidFunction 
		
		return null;
	}
	*/
	
public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	
	public COPSolution stochastic_local_search(COPSolution solution, List<Vehicle> v_list, List<Task> task_list, int counter) {
		COPSolution old_s = solution;		
		List<COPSolution> neighbours = cn.getNeighbours(old_s, v_list, task_list, counter);
		// System.out.println("neighbours size in SLS is " + neighbours.size());
		COPSolution best_solution_from_neighbours = lc.getBestSolution(neighbours, old_s, counter);
		return best_solution_from_neighbours;
	}
	
	
	public int get_largest_vehicle_capacity(List <Vehicle> vehicle_list) {
		int largest_capacity = 0;		
		for(Vehicle v:vehicle_list) {
			if (v.capacity() > largest_capacity){
				largest_capacity = v.capacity();				
			}
		}
		return largest_capacity;
		
	}
	
	public double get_reward(List <Task> task_list) {
		long sum = 0;
		for(Task t:task_list) {
			sum+= t.reward;
		}
		return sum;
	}
}
