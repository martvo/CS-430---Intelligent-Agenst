package template;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
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
		return plan;
	}
	

	public Plan BFSPlan(Vehicle vehicle, TaskSet tasks)  {
		Plan plan = new Plan(vehicle.getCurrentCity());
		ArrayList<DeliberativeState> finalStates = new ArrayList<DeliberativeState>();
		LinkedList<DeliberativeState> queue = new LinkedList<DeliberativeState>();
		List<DeliberativeState> visitedStates = new ArrayList<DeliberativeState>();
		queue.addLast(new DeliberativeState(tasks, vehicle.getCurrentTasks(), capasity, vehicle.getCurrentCity()));
		while (!queue.isEmpty()) {
			DeliberativeState node = queue.getFirst();
			if (node.isFinal()) {
				finalStates.add(node);
				// add first if highest cost?? -> then the best final state is first in the list. Just use .get(0) to get it
			}
			if (!visitedStates.contains(node)) {
				visitedStates.add(node);
				for (DeliberativeState successor : node.getSuccessors(vehicle)) {  // ??????????????????????????
					queue.addLast(successor);
				}
			}
		}
		// for each action in the actions of the final state, add them
		for (Action a : finalStates.get(0).getPlanForState()) {
			plan.append(a);
		}
		return plan;
	}
	
	
	public Plan aStarPlan(Vehicle vehicle, TaskSet tasks) {
		return null;
	}

	
	@Override
	public void planCancelled(TaskSet carriedTasks) {
		// TODO Auto-generated method stub
		
	}

}
