package template;

import java.util.ArrayList;

import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Delivery;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class DeliberativeState {
	
	private City currentCity;
	private float costOfGettingToState;
	private TaskSet currentTaskSet;
	private TaskSet tasksInTheTopology;
	private int capacity;
	
	// This list is the action sequence to get to this state
	private ArrayList<Action> actionsToGetToState;
	
	public DeliberativeState(TaskSet currentTasks, TaskSet worldTasks, int capacity, City currentCity) {
		this.currentTaskSet = currentTasks;
		this.tasksInTheTopology = worldTasks;
		this.capacity = capacity;
		this.currentCity = currentCity;
		costOfGettingToState = 0;  // If this is the initial state this will stay as 0.
		actionsToGetToState = new ArrayList<Action>();
	}
	
	public int getRemainingCapasity() {
		return capacity - currentTaskSet.weightSum();
	}
	
	public boolean isFinal() {
		return (tasksInTheTopology.isEmpty() && currentTaskSet.isEmpty());
	}
	
	public void setCurrentCity(City newCity) {
		this.currentCity = newCity;
	}
	
	public void updateCapacity(int weight) {
		this.capacity += weight;
	}
	
	public void setActionsToGetToState(ArrayList<Action> actionList) {
		this.actionsToGetToState = actionList;
	}
	
	public ArrayList<Action> getPlanForState() {
		return actionsToGetToState;
	}
	
	public void updateCost(double value) {
		this.costOfGettingToState += value;
	}
	
	public double getCost() {
		return this.costOfGettingToState;
	}
	
	public City getCityOfState() {
		return currentCity;
	}
	
	public ArrayList<DeliberativeState> getSuccessors(Vehicle vehicle) {
		ArrayList<DeliberativeState> outList = new ArrayList<DeliberativeState>();
		
		// If no more tasks -> return empty ArrayList
		if (currentTaskSet.isEmpty() && tasksInTheTopology.isEmpty()) {
			return outList;  // Tror ikke denne trengs, men den øker kjøre tid....
		}
		
		// Fist we go through all tasks we can deliver and add the state we will end up in
		for (Task t : currentTaskSet) {
			// DeliberativeState thisState = (DeliberativeState) this.clone();
			// "Copy" the state
			DeliberativeState thisState = new DeliberativeState(currentTaskSet, tasksInTheTopology, capacity, currentCity);
			thisState.setActionsToGetToState(this.actionsToGetToState);
			thisState.updateCost(this.costOfGettingToState);
			
			// Add actions to get to the delivery city of the task
			for (City city : currentCity.pathTo(t.deliveryCity)) {
				actionsToGetToState.add(new Move(city));
			}
			
			// Then we change our city to the city we moved to and add the cost for moving there
			thisState.updateCost(vehicle.costPerKm() * this.currentCity.distanceTo(t.deliveryCity));
			thisState.setCurrentCity(t.deliveryCity);
		
			// Since we are delivering a task we remove it from our task set, deliver the task and update our capacity
			actionsToGetToState.add(new Delivery(t));
			thisState.currentTaskSet.remove(t);
			thisState.updateCapacity(t.weight);
			
			// Add the new state to the outList
			outList.add(thisState);
		}
		
		// Then we go through all the task we can pick up around in the topology
		for (Task t : tasksInTheTopology) {
			// Add only states that have a valid capacity
			if (t.weight <= this.capacity) {
				// DeliberativeState thisState = (DeliberativeState) this.clone();
				// "Copy" the state
				DeliberativeState thisState = new DeliberativeState(currentTaskSet, tasksInTheTopology, capacity, currentCity);
				thisState.setActionsToGetToState(this.actionsToGetToState);
				thisState.updateCost(this.costOfGettingToState);
				
				// Add actions to get to the delivery city of the task
				for (City city : currentCity.pathTo(t.deliveryCity)) {
					actionsToGetToState.add(new Move(city));
				}
				
				// Then we change our city to the city we moved to and add the cost for moving there
				thisState.updateCost(vehicle.costPerKm() * this.currentCity.distanceTo(t.deliveryCity));
				thisState.setCurrentCity(t.deliveryCity);
				
				// Since we are picking up a task we add it to our task set, pick it up and update our capacity
				actionsToGetToState.add(new Pickup(t));
				thisState.currentTaskSet.add(t);
				thisState.updateCapacity(-t.weight);
				
				// Add the new state to the outList
				outList.add(thisState);
			}
		}
		return outList;
	}
}
