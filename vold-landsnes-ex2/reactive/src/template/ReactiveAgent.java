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
	private HashMap<City, HashMap<City, Double>> rewards;
	private HashMap<City, HashMap<City, Double>> values;

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
		rewards = new HashMap<City, HashMap<City, Double>>();
		values = new HashMap<City, HashMap<City, Double>>();
		
		int costPerKm = agent.vehicles().get(0).costPerKm();
		
		for (City fromCity : topology.cities()) {
			HashMap<City, State> hashMapStates = new HashMap<City, State>();
			HashMap<City, Double> hashMapRewards = new HashMap<City, Double>();
			HashMap<City, Double> hashMapValues = new HashMap<City, Double>();
			
			// this adds a state from fromCity which have no task (needed for value iteration, will play the role as V(s'))
			hashMapStates.put(null, new State(fromCity, null, td));
			for (City toCity : topology.cities()) {
				// this adds a state which has a task from fromCity to toCity
				hashMapStates.put(toCity, new State(fromCity, toCity, td));
				
				// adds the reward for a action involving going to toCity from fromCity and picking up a task
				hashMapRewards.put(toCity, td.reward(fromCity, toCity) - (fromCity.distanceTo(toCity) * costPerKm));
				
				// initializes all values to 0.0
				hashMapValues.put(toCity, 0.0);
			}
			states.put(fromCity, hashMapStates);
			rewards.put(fromCity, hashMapRewards);
			values.put(fromCity, hashMapValues);
		}
		
		// printRewards();
		// printValues();
		// printStatesWithBestActionAndValue();
		
		valueIteration();
		
	}
	
	
	public void valueIteration() {
		for (int i = 0; i < 10000; i++) {
			// goes through every HashMap corresponding to every fromCity
			for (HashMap<City, State> cityToHashMap : states.values()) {
				
				// goes through every state corresponding to every fromCity and toCity pair
				for (State state : cityToHashMap.values()) {
					
					// goes through every 
				}
			}
		}
	}
	
	
	public class State {
		public City fromCity;
		public City toCity;
		
		public City bestAction = null;
		public double valueOfBestAction = 0.0;
		
		public State(City from, City to, TaskDistribution td) {
			fromCity = from;
			toCity = to;
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// TODO Auto-generated method stub
		// Hvis availableTask == null -> hent ut beste action fra staten vi er i nå
		// Hvis det er en avalibleTask, bestem seg for om vi skal ta tasken eller dra å hente en ny en
		// Krav for å hente en ny task: reward - cost for å bevege seg til delivery stedet < enn forventet reward ellers i miljøet.
		// Må vell sjekke om vi har nok kapasitet for å ta med pakken videre...
		 
		return null;
	}
	
	
	public void printRewards() {
		for (City city : rewards.keySet()) {
			for (City city2 : rewards.get(city).keySet()) {
				System.out.println("From " + city + " to " + city2 + " reward: " + rewards.get(city).get(city2));
			}
		}
	}
	
	public void printValues() {
		for (City city : values.keySet()) {
			for (City city2 : values.get(city).keySet()) {
				System.out.println("From " + city + " to " + city2 + " value: " + values.get(city).get(city2));
			}
		}
	}
	
	public void printStatesWithBestActionAndValue() {
		for (City city : states.keySet()) {
			for (City city2 : states.get(city).keySet()) {
				System.out.println("From " + city + " to " + city2 + " best action: " + states.get(city).get(city2).bestAction + " best value " +
						states.get(city).get(city2).valueOfBestAction);
			}
		}
	}

}
