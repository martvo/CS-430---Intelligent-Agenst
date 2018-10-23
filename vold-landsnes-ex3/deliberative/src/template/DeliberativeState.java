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
		this.costOfGettingToState = 0;  // If this is the initial state this will stay as 0.
		this.actionsToGetToState = new ArrayList<Action>();
	}
	
	public void addTaskToCurrent(Task t) {
		this.currentTaskSet.add(t);
	}
	
	public void removeTaskFromCurrent(Task t) {
		this.currentTaskSet.remove(t);
	}
	
	public void removeTaskFromWord(Task t) {
		this.tasksInTheTopology.remove(t);
	}
	
	public int getRemainingCapasity() {
		return capacity - currentTaskSet.weightSum();
	}
	
	public boolean isFinal() {
		return this.tasksInTheTopology.isEmpty() && this.currentTaskSet.isEmpty();
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
	
	public void addActionsToSate(Action a) {
		this.actionsToGetToState.add(a);
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
	
	public int getCurrentTaskSize() {
		return this.currentTaskSet.size();
	}
	
	public int getWorldTaskSize() {
		return this.tasksInTheTopology.size();
	}
	
	public TaskSet getCurrentTaskSet() {
		return this.currentTaskSet;
	}
	
	public TaskSet getWorldTaskSet() {
		return this.tasksInTheTopology;
	}
	
	public ArrayList<Action> getActionsToState() {
		return this.actionsToGetToState;
	}
	
	public boolean isTheSame(DeliberativeState otherState) {
		boolean out = false;
		if (this.currentCity.equals(otherState.getCityOfState()) && this.currentTaskSet.equals(otherState.getCurrentTaskSet()) && this.tasksInTheTopology.equals(otherState.getWorldTaskSet())) {
			out = true;
		}
		return out;
	}
	
	public ArrayList<DeliberativeState> getSuccessors(Vehicle vehicle) {
		ArrayList<DeliberativeState> outList = new ArrayList<DeliberativeState>();
		
		System.out.println("State: " + this + ", has currentCity: " + this.currentCity);
		// If no more tasks -> return empty ArrayList
		if (currentTaskSet.isEmpty() && tasksInTheTopology.isEmpty()) {
			return outList;  // Tror ikke denne trengs, men den øker kjøre tid....
		}
		
		// Fist we go through all tasks we can deliver and add the state we will end up in
		// System.out.println("Current tasks: " + currentTaskSet.size() + ", world tasks: " + tasksInTheTopology.size());
		for (Task t : currentTaskSet) {
			// DeliberativeState thisState = (DeliberativeState) this.clone();
			// "Copy" the state
			DeliberativeState thisState = new DeliberativeState(this.currentTaskSet.clone(), this.tasksInTheTopology.clone(), this.capacity, this.currentCity);
			// System.out.println("This state (" + thisState + ") in the agent loop has currentTasks:" + thisState.getCurrentTaskSize() + ", and worldTasks:" + thisState.getWorldTaskSize());
			// System.out.println("And this new state: " + this + ", has currentCity: " + thisState.getCityOfState());
			// thisState.setActionsToGetToState(this.actionsToGetToState);
			for (Action a : this.actionsToGetToState) {
				thisState.actionsToGetToState.add(a);
			}
			thisState.updateCost(this.costOfGettingToState);
			
			// Add actions to get to the delivery city of the task
			for (City city : this.currentCity.pathTo(t.deliveryCity)) {
				thisState.actionsToGetToState.add(new Move(city));
			}
			
			// Then we change our city to the city we moved to and add the cost for moving there
			thisState.updateCost(vehicle.costPerKm() * this.currentCity.distanceTo(t.deliveryCity));
			thisState.setCurrentCity(t.deliveryCity);
			
			// Since we are delivering a task we remove it from our task set, deliver the task and update our capacity
			thisState.actionsToGetToState.add(new Delivery(t));
			thisState.removeTaskFromCurrent(t);
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
				DeliberativeState thisState = new DeliberativeState(this.currentTaskSet.clone(), this.tasksInTheTopology.clone(), this.capacity, this.currentCity);
				// System.out.println("This state( " + thisState + ")  in the world loop has currentTasks:" + thisState.getCurrentTaskSize() + ", and worldTasks:" + thisState.getWorldTaskSize());
				// thisState.setActionsToGetToState(this.actionsToGetToState);
				for (Action a : this.actionsToGetToState) {
					thisState.actionsToGetToState.add(a);
				}
				thisState.updateCost(this.costOfGettingToState);
				System.out.println("And this new state: " + this + ", has currentCity: " + thisState.getCityOfState());
				
				// Add actions to get to the pickup city of the task
				for (City city : thisState.getCityOfState().pathTo(t.pickupCity)) {
					thisState.actionsToGetToState.add(new Move(city));
				}
				
				// Then we change our city to the city we moved to and add the cost for moving there
				thisState.updateCost(vehicle.costPerKm() * this.currentCity.distanceTo(t.pickupCity));
				thisState.setCurrentCity(t.pickupCity);
				
				// Since we are picking up a task we add it to our task set, pick it up and update our capacity
				thisState.actionsToGetToState.add(new Pickup(t));
				thisState.addTaskToCurrent(t);
				thisState.removeTaskFromWord(t);
				thisState.updateCapacity(-t.weight);
				
				// Add the new state to the outList
				outList.add(thisState);
			}
		}
		return outList;
	}
}
