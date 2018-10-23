package template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;

public class DeliberativeAgent implements DeliberativeBehavior {
	
	/* Environment */
	private Topology topology;
	private TaskDistribution td;
	
	/* the properties of the agent */
	private Agent agent;
	private int capasity;
	private int costPerKm;
	
	enum Algorithm { BFS, ASTAR }
	
	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {
		this.topology = topology;
		this.td = distribution;
		this.agent = agent;
		this.capasity = agent.vehicles().get(0).capacity();
		this.costPerKm = agent.vehicles().get(0).costPerKm();
		
		// initialize the planner
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
	}

	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		
		// Compute the plan with the selected algorithm.
		long startTime = System.currentTimeMillis();
		switch (algorithm) {
		case ASTAR:
			plan = aStarPlan(vehicle, tasks);
			break;
		case BFS:
			plan = BFSPlan(vehicle, tasks);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		System.out.println("It took " + (System.currentTimeMillis() - startTime) + " milliseconds to calculate the plan");
		return plan;
	}
	

	public Plan BFSPlan(Vehicle vehicle, TaskSet tasks)  {
		Plan plan = new Plan(vehicle.getCurrentCity());
		List<DeliberativeState> finalStates = new ArrayList<DeliberativeState>();
		LinkedList<DeliberativeState> queue = new LinkedList<DeliberativeState>();
		List<DeliberativeState> visitedStates = new ArrayList<DeliberativeState>();
		queue.addLast(new DeliberativeState(vehicle.getCurrentTasks(), tasks, capasity, vehicle.getCurrentCity()));
		// for (int x = 0; x < 3; x++) {
		while (!queue.isEmpty()) {
			System.out.println("Queue: " + queue.size());
			System.out.println("Final: " + finalStates.size());
			DeliberativeState node = queue.removeFirst();
			System.out.println(node.getCityOfState() + " " + node.getCurrentTaskSize() + " " + node.getWorldTaskSize());
			// System.out.println(node);
			// System.out.println(node.getSuccessors(vehicle).size());
			if (node.isFinal()) {
				System.out.println("Final");
				if (finalStates.size() > 0 && (node.getCost() < finalStates.get(0).getCost())) {
					finalStates.add(0, node);
				} else {
					finalStates.add(node);
				}
				// add first if lowest cost?? -> then the best final state is first in the list. Just use .get(0) to get it
			}
			// ________________Test for å se om dette gjør BFS bedre_________________________
			// bytter ut en state hvis vi finner en bedre!!!! da slipper vi å sjekke om det er flere som er like
			DeliberativeState visited = null;
			for (DeliberativeState d : visitedStates) {
				if (node.isTheSame(d)) {
					visited = d;
				}
			}
			
			if (visited == null) {
				visitedStates.add(node);
				queue.addAll(node.getSuccessors(vehicle));
			} else {
				if (visited.getCost() > node.getCost()) {
					// replace old state with the new and better one and remove it's successors
					queue.removeAll(visited.getSuccessors(vehicle));
					visitedStates.remove(visited);
					
					// Add the state that was better and it's successors
					visitedStates.add(node);
					queue.addAll(node.getSuccessors(vehicle));
				}
			}
			// _________________________________________________________________________________
			
			/*
			boolean visited = false;
			for (DeliberativeState d : visitedStates) {
				if (node.isTheSame(d)) {
					visited = true;
				}
			}
			if (!visited) {  // Legg til en sjekk på om hvis vi har vært i denne staten så sjekker vi om den har bedre cost enn den som allerede ligger der!!!!!!!!!!s
				System.out.println("Not visited");
				visitedStates.add(node);
				for (DeliberativeState successor : node.getSuccessors(vehicle)) {
					queue.addLast(successor);
					System.out.println("Successor has actions: " + successor.getActionsToState());
				}
			}
			*/
			System.out.println("Visited: " + visitedStates.size());
			System.out.println(node.getActionsToState());
			System.out.println();
		}
		System.out.println(finalStates.get(0).getActionsToState());
		// for each action in the actions of the final state, add them
		for (Action a : finalStates.get(0).getPlanForState()) {
			plan.append(a);
		}
		for (DeliberativeState d : finalStates) {
			System.out.println(d.getCost());
		}
		System.out.println(finalStates.get(0).getCost());
		System.out.println(plan.totalDistance());
		return plan;
	}
	
	
	public Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		Plan plan = new Plan(vehicle.getCurrentCity());
		LinkedList<DeliberativeState> queue = new LinkedList<DeliberativeState>();
		List<DeliberativeState> visitedStates = new ArrayList<DeliberativeState>();
		queue.addLast(new DeliberativeState(vehicle.getCurrentTasks(), tasks, capasity, vehicle.getCurrentCity()));
		
		while (!queue.isEmpty()) {
			DeliberativeState node = queue.removeFirst();
			
			if (node.isFinal()) {
				System.out.println("Found optimal plan");
				for (Action a : node.getPlanForState()) {
					plan.append(a);
				}
				System.out.println(node.getCost());
				System.out.println(plan.totalDistance());
				return plan;
			}
			
			// Algorithm in the slides allows us to have more than one state that is equal
			ArrayList<DeliberativeState> equalStates = new ArrayList<DeliberativeState>();
			for (DeliberativeState d : visitedStates) {
				if (node.isTheSame(d)) {
					equalStates.add(d);
				}
			}
			
			if (equalStates.isEmpty()) {
				visitedStates.add(node);
				queue.addAll(node.getSuccessors(vehicle));
			} else {
				// Get the state with highest f from equalStates
				DeliberativeState bestState = equalStates.get(0);
				for (DeliberativeState s : equalStates) {
					if (getF(bestState, vehicle.costPerKm()) > getF(s, vehicle.costPerKm())) {
						bestState = s;
					}
				}
				
				if (bestState.getCost() > node.getCost()) {
					visitedStates.add(node);
					queue.addAll(node.getSuccessors(vehicle));
				}
			}
			
			// Sort queue
			// queue = sortedQueue(queue, vehicle.costPerKm());
			Collections.sort(queue, new Comparator<DeliberativeState>() {
			    @Override
			    public int compare(DeliberativeState s1, DeliberativeState s2) {
			        return (int) (getF(s1, vehicle.costPerKm()) - getF(s2, vehicle.costPerKm()));
			        // return (int) (getF(s2, vehicle.costPerKm()) - getF(s1, vehicle.costPerKm()));
			    }
			});
			System.out.println();
			
			// Må sortere listne med visited p f(x) og bytte hvis den er bedre
			// Husk å gange h med costPerKm!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		}
		return plan;
	}
	
	public LinkedList<DeliberativeState> sortedQueue(LinkedList<DeliberativeState> queue, int costPerKm) {
		return null;
	}
	
	// If no task to pick up: return the cost of delivering the task with the longest distance
	// If items to pickup: return the cost of picking up the task that is farthest away, picking it up, and delivering it
	public double getH(DeliberativeState state, int costPerKm) {
		double highestValue = 0;
		if (!(state.getWorldTaskSize() == 0)) {
			for (Task task : state.getWorldTaskSet()) {
				double travellDistance = state.getCityOfState().distanceTo(task.pickupCity) + state.getCityOfState().distanceTo(task.deliveryCity);
				if (highestValue < travellDistance) {
					highestValue = travellDistance;
				}
			}
		} else {
			for (Task task : state.getCurrentTaskSet()) {
				double travellDistance = state.getCityOfState().distanceTo(task.deliveryCity);
				if (highestValue < travellDistance) {
					highestValue = travellDistance;
				}
			}
		}
		return highestValue * costPerKm;
	}
	
	public double getF(DeliberativeState state, int costPerKm) {
		return state.getCost() + getH(state, costPerKm);
	}
	
	// Lurer på om jeg trenger denne???
	/*
	public ArrayList<DeliberativeState> sortByF(ArrayList<DeliberativeState> stateList) {
		//sort by f
		Collections.sort(stateList, new Comparator<DeliberativeState>() {
			@Override
			public int compare(DeliberativeState s1, DeliberativeState s2) {
				return (int) (getF(s1) - getF(s2));
			}
		});
		return stateList.sort((DeliberativeState s1, DeliberativeState s2)->(getF(s1) - getF(s2)));
	}
	*/
	
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		// TODO Auto-generated method stub
		
	}

}
