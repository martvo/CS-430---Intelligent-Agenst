package template;

import logist.simulation.Vehicle;

import java.util.HashMap;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveAgent implements ReactiveBehavior {
	
	private int numActions;
	private Agent myAgent;
	
	private HashMap<City, HashMap<City, State>> states; 
	private HashMap<State, Double> values;  // This is the V(s) values
	private HashMap<State, City> actions;  // This is the best action for a state

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);
		System.out.println("Discount factor is: " + discount);
		System.out.println();
		this.numActions = 0;
		this.myAgent = agent;
		
		// HashMaps
		states = new HashMap<City, HashMap<City, State>>();
		values = new HashMap<State, Double>();
		actions = new HashMap<State, City>();
		
		int costPerKm = agent.vehicles().get(0).costPerKm();
		
		for (City fromCity : topology.cities()) {
			HashMap<City, State> hashMapStates = new HashMap<City, State>();
			
			// this adds a state from fromCity which have no task (needed for value iteration, will play the role as V(s'))
			State newNullState = new State(fromCity, null, td, topology, costPerKm);
			hashMapStates.put(null, newNullState);
			
			// add action and value to the new null state
			actions.put(newNullState, null);
			values.put(newNullState, 0.0);
			
			for (City deliveToCity : topology.cities()) {
				// this adds a state which has a task from fromCity to toCity
				
				State newState = new State(fromCity, deliveToCity, td, topology, costPerKm);
				hashMapStates.put(deliveToCity, newState);
				
				// initializes all values to 0.0
				values.put(newState, 0.0);
					
				// initializes actions to null. This is the action for when a city has a task
				actions.put(newState, null);
			}
			states.put(fromCity, hashMapStates);
		}
		
		printRewards();
		System.out.println();
		
		valueIteration(discount, td, topology);
		
		printStatesWithBestActionAndValue();
		System.out.println();
		
	}
	
	
	public void valueIteration(double discount, TaskDistribution td, Topology topology) {
		for (int i = 0; i < 10000000; i++) {
			double conv = 0;
			
			// goes through every HashMap corresponding to every fromCity
			for (HashMap<City, State> hashMap : states.values()) {
				
				// goes through every state corresponding to every fromCity and toCity pair
				for (State currentState : hashMap.values()) {

					// Now we are accessing each state. Need to do value iteration over them
					// for each action in each state
					updateQValues(currentState, td, topology, discount);
					
					// Used to calculate the absolute change in value for a state
					double oldBestValue = values.get(currentState);
					
					// Update the value and action for current state
					updateBestValueAndAction(currentState);
					
					// New best value
					double newBestValue = values.get(currentState);
					
					// set new conv value if it's higher than the last one
					if (conv < (Math.abs(oldBestValue - newBestValue))) {
						conv = Math.abs(oldBestValue - newBestValue);
					}
				}
			}
			System.out.println("After iteration " + i + ", the max conv value is: " + conv);
			if (conv <= 1/1000000000) {
				System.out.println("Value Iteration done after: " + i + " iterations");
				System.out.println();
				break;
			}
		}
	}
	
	
	public void updateQValues(State currentState, TaskDistribution td, Topology topology, double discount) {
		// For each action (corresponding to a new city)
		// Action is the new city we end up in
		for (City action : currentState.qValues.keySet()) {
			
			// Start with the immediate reward R(s,a) in the value iteration algorithm
			double oldValue = currentState.rewards.get(action);
			
			// variable for the sum part of the bellman update
			double sumPart = 0.0;
			// Probability of no task in city
			double probForNoTask = 1.0;
			
			// Possible deliveryTo cities for a state s'
			for (City toCity : topology.cities()) {
				
				sumPart = discount * td.probability(action, toCity) * values.get(states.get(action).get(toCity));
				probForNoTask -= td.probability(action, toCity);
			}
			
			// Need V(s') for the city to not have a task too
			sumPart += discount * probForNoTask * values.get(states.get(action).get(null));
			
			// Update qValue
			currentState.qValues.put(action, (oldValue + sumPart));
		}
	}
	
	
	public void updateBestValueAndAction(State currentState) {
		// V(S) <- maxa Q(s,a)
		double bestValue = Double.NEGATIVE_INFINITY;
		for (City city : currentState.qValues.keySet()) {
			double qValue = currentState.qValues.get(city);
			if (qValue > bestValue) {
				values.put(currentState, qValue);
				bestValue = qValue;
				actions.put(currentState, city);
			}
		}
	}
	
	
	// If the toCity variable is null it means that we are in a city and there is no task there for us
	// If the toCity variable has a City object it means that the city we are in have a task for us to that city
	public class State {
		public City fromCity;  // task from a City fromCity
		public City deliveryTo;  // task to a City toCity
		
		public HashMap<City, Double> qValues = new HashMap<City, Double>();  // this is the Q(s,a) values for this state
		public HashMap<City, Double> rewards = new HashMap<City, Double>();  // this is the R(s,a) values for this state
		
		public State(City from, City to, TaskDistribution td, Topology topology, double kmCost) {
			fromCity = from;
			deliveryTo = to;
			
			// The values that are added here are the values corresponding to a move action
			
			// Have to go to a neighbor because going straight to a no-neighbor is not allowed
			for (City moveToCity : fromCity.neighbors()) {
				// Initialize qValues to be 0.0 and the reward values to their respected values
				rewards.put(moveToCity, -(fromCity.distanceTo(moveToCity) * kmCost));
				qValues.put(moveToCity, 0.0);
			}
			
			// Add values for a deliver action, if there exists a task
			// Has to be under the for loop! Or else we will write over the reward when there is a task
			if (deliveryTo != null) {
				qValues.put(deliveryTo, 0.0);
				rewards.put(deliveryTo, td.reward(fromCity, deliveryTo) - (fromCity.distanceTo(deliveryTo) * kmCost));
			}
		}
	}

	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;
		
		City currentCity = vehicle.getCurrentCity();
		
		// No task in city, pick the best one to go to
		if (availableTask == null) {
			City goToCity = actions.get(states.get(currentCity).get(null));
			System.out.println("No task in " + currentCity.name + ". Going to " + goToCity);
			action = new Move(goToCity);
			System.out.println(values.get(states.get(currentCity).get(null)));
		} else {
			// City has a task. Find out if we want to take it or not
			City bestActionForState = actions.get(states.get(currentCity).get(availableTask.deliveryCity));
			
			// Pick up task if the best action for the state is the same as the delivery city of the task and we have capasity for it
			if (bestActionForState == availableTask.deliveryCity && availableTask.weight <= vehicle.capacity()) {
				action = new Pickup(availableTask);
				System.out.println("Decided to take the task, going to: " + availableTask.deliveryCity + " to deliver");
			} else {  // skip the task and try another city
				action = new Move(bestActionForState);
				System.out.println("Decided not to take the task, going to: " + bestActionForState);
			}
		}
		if (numActions >= 1) {
			System.out.println("Reactive Agent");
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		return action;
	}
	
	
	public void printRewards() {
		System.out.println("Rewards:");
		for (City fromCity : states.keySet()) {
			for (City toCity : states.get(fromCity).keySet()) {
				System.out.println("From " + fromCity + " to " + toCity + " reward: " + states.get(fromCity).get(toCity).rewards.get(toCity));
			}
		}
	}
	
	
	public void printStatesWithBestActionAndValue() {
		System.out.println("Best Values and Best Actions:");
		for (City city : states.keySet()) {
			for (City city2 : states.get(city).keySet()) {
				System.out.println("From " + city + " to " + city2 + " best action: " + actions.get(states.get(city).get(city2)) + ", best value: " +
						values.get(states.get(city).get(city2)));
			}
		}
	}
}
