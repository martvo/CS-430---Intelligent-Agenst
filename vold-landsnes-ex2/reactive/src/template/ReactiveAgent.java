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
	
	private double pPickup;
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

		this.pPickup = discount;
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
			
			for (City toCity : topology.cities()) {
				// this adds a state which has a task from fromCity to toCity
				State newState = new State(fromCity, toCity, td, topology, costPerKm);
				hashMapStates.put(toCity, newState);
				
				// initializes all values to 0.0
				values.put(newState, 0.0);
				
				// initializes actions to null. This is the action for when a city has a task
				actions.put(newState, null);
			}
			states.put(fromCity, hashMapStates);
		}
		
		
		printRewards();
		System.out.println();
		printValues();
		System.out.println();
		printStatesWithBestActionAndValue();
		System.out.println();
		
		
		valueIteration(discount, td, topology);
		
		
		printRewards();
		System.out.println();
		printValues();
		System.out.println();
		printStatesWithBestActionAndValue();
		System.out.println();
		
	}
	
	
	public void valueIteration(double discount, TaskDistribution td, Topology topology) {
		for (int i = 0; i < 10000; i++) {
			
			// goes through every HashMap corresponding to every fromCity
			for (HashMap<City, State> hashMap : states.values()) {
				
				// goes through every state corresponding to every fromCity and toCity pair
				for (State currentState : hashMap.values()) {

					// Now we are accessing each state. Need to do value iteration over them
					// for each action in each state
					
					// For each action (corresponding to a new city)
					for (City action : currentState.qValues.keySet()) {
						
						// Start with the immediate reward R(s,a) in the value iteration algorithm
						double oldValue = currentState.rewards.get(action);
						City bestAction = null;
						
						// variable for the sum part of the bellman update
						double sumPart = 0.0;
						// Probability of no task in city
						double probForNoTask = 1.0;
						
						// For each state s'
						for (City toCity : topology.cities()) {
							sumPart = discount * td.probability(action, toCity) * values.get(states.get(action).get(toCity));
							probForNoTask -= td.probability(action, toCity);
						}
						
						// Need V(s') for the city to not have a task too
						sumPart += discount * probForNoTask * values.get(states.get(action).get(null));
						
						// Update qValue
						currentState.qValues.put(action, oldValue + sumPart);
					}
					
					// V(S) <- maxa Q(s,a)
					double bestValue = Double.NEGATIVE_INFINITY;
					City bestAction = null;
					for (City city : currentState.qValues.keySet()) {
						double qValue = currentState.qValues.get(city);
						if (qValue > bestValue) {
							bestValue = qValue;
							bestAction = city;
						}
					}
					if (bestAction != null) {
						values.put(currentState, bestValue);
						actions.put(currentState, bestAction);
					}
				}
			}
		}
	}
	
	
	// If the toCity variable is null it means that we are in a city and there is no task there for us
	// If the toCity variable has a City object it means that the city we are in have a task for us
	public class State {
		public City fromCity;  // task from a City fromCity
		public City deliveryTo;  // task to a City toCity
		
		public HashMap<City, Double> qValues = new HashMap<City, Double>();  // this is the Q(s,a) values for this state
		public HashMap<City, Double> rewards = new HashMap<City, Double>();  // this is the R(s,a) values for this state
		
		public State(City from, City to, TaskDistribution td, Topology topology, double kmCost) {
			fromCity = from;
			deliveryTo = to;
			
			// Add values for a deliver action, if there exists a task
			if (deliveryTo != null) {
				qValues.put(deliveryTo, 0.0);
				rewards.put(deliveryTo, td.reward(fromCity, deliveryTo) - ((td.reward(fromCity, deliveryTo) * kmCost)));
			}
			
			// The values that are added here are the values corresponding to a move action
			for (City moveToCity : fromCity.neighbors()) {
				// Initialize qValues to be 0.0 and the reward values to their respected values
				rewards.put(moveToCity, -(td.reward(fromCity, moveToCity) * kmCost));
				qValues.put(moveToCity, 0.0);
			}
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// TODO Auto-generated method stub
		// Hvis availableTask == null -> hent ut beste action fra staten vi er i nå
		// Hvis det er en avalibleTask, bestem seg for om vi skal ta tasken eller dra å hente en ny en
		// Krav for å hente en ny task: reward - cost for å bevege seg til delivery stedet < enn forventet reward ellers i miljøet.
		// Må vell sjekke om vi har nok kapasitet for å ta med pakken videre...
		
		Action action;
		
		City currentCity = vehicle.getCurrentCity();
		
		if (availableTask == null) {
			City goToCity = actions.get(states.get(currentCity).get(null));
			System.out.println("No task in " + currentCity.name + ". Going to " + goToCity);
			action = new Move(goToCity);
			System.out.println(values.get(states.get(currentCity).get(null)));
		} else {
			City goToCity = actions.get(states.get(currentCity).get(null));
			System.out.println("Task in " + currentCity.name + ". Going to " + goToCity);
			action = new Move(goToCity);
			System.out.println(values.get(states.get(currentCity).get(availableTask.deliveryCity)));
		}
		return action;
	}
	
	
	public void printRewards() {
		for (City fromCity : states.keySet()) {
			for (City toCity : states.get(fromCity).keySet()) {
				System.out.println("From " + fromCity + " to " + toCity + " reward: " + states.get(fromCity).get(toCity).rewards.get(toCity));
			}
		}
	}
	
	
	public void printValues() {
		for (State state : values.keySet()) {
			System.out.println("From " + state.fromCity + " to " + state.deliveryTo + " value: " + values.get(state));
		} 
	}
	
	public void printStatesWithBestActionAndValue() {
		for (City city : states.keySet()) {
			for (City city2 : states.get(city).keySet()) {
				System.out.println("From " + city + " to " + city2 + " best action: " + actions.get(states.get(city).get(city2)) + " best value " +
						values.get(states.get(city).get(city2)));
			}
		}
	}
	

}
