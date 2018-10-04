package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveArray implements ReactiveBehavior {
	
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	
	// These matrices will use i (horizontal) as fromCity and j (vertical) as toCity
	private ArrayList<ArrayList<Double>> values;
	private ArrayList<ArrayList<Double>> rewards;
	private ArrayList<Integer> actions;
	
	private HashMap<Integer, City> indexToCity = new HashMap<Integer, City>();

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		values = new ArrayList<ArrayList<Double>>(topology.size());
		rewards = new ArrayList<ArrayList<Double>>(topology.size());
		actions = new ArrayList<Integer>(topology.size());
		
		// Make the mapping of City to index, to be used for the values and rewards Matrices
		int n = 0;
		for (City city : topology) {
			indexToCity.put(n, city);
			n++;
		}
		
		int costPerKm = agent.vehicles().get(0).costPerKm();
		
		for (int i = 0; i < topology.size(); i++) {
			ArrayList<Double> rewardList = new ArrayList<Double>(topology.size());
			ArrayList<Double> valueList = new ArrayList<Double>(topology.size());
			for (int j = 0; j < topology.size(); j++) {
				rewardList.add(j, td.reward(indexToCity.get(i), indexToCity.get(j)) - (indexToCity.get(i).distanceTo(indexToCity.get(j)) * costPerKm));
				valueList.add(j, 0.0);
				
			}
			rewards.add(i, rewardList);
			values.add(i, valueList);
		}
		
		valueIteration(topology, td, agent, discount);
		
	}
	
	public void valueIteration(Topology topology, TaskDistribution td, Agent agent, double discount) {
		for (int x = 0; x < 10000; x++) {
			// For each state
			for (int i = 0; i < values.size(); i++) {
				for (int j = 0; j < values.get(i).size(); j++) {
					double qValue = 0.0;
					// For each action
					ArrayList<Double> qValues = new ArrayList<Double>();
					for (int a = 0; a < values.get(i).size(); a++) {
						// Hva skal V(s') være???? 
						qValue = rewards.get(i).get(j) + discount * td.probability(indexToCity.get(i), indexToCity.get(j)) * 1;
						qValues.add(a, qValue);
					}
					// Update values
					ArrayList<Double> newValueArray = values.get(i);
					newValueArray.set(j, Collections.max(qValues));
					values.set(i, newValueArray);
					
					// Update actions
					 
				}
			}
			// until good enough skall settes inn hær
		}
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		// TODO Auto-generated method stub
		// How do we find the best action???
		return null;
	}

}