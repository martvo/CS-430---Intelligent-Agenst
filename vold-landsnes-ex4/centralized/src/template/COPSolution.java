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

public class COPSolution {
	
	private List<Plan> plans;
	private LinkedHashMap<Vehicle, ArrayList<Integer>> action_task_list;  // Keeps the sequence of pickup and deliver actions
	private LinkedHashMap<Vehicle, ArrayList<Task>> vehicle_tasks;  // Keeps the task per vehicle
	private double cost_of_all_plans;
	
	public COPSolution(List<Vehicle> vehicles, List<Task> task_list) {
		cost_of_all_plans = 0;
		this.plans = new ArrayList<Plan>();
		action_task_list = new LinkedHashMap<Vehicle, ArrayList<Integer>>();
		

		vehicle_tasks = new LinkedHashMap<Vehicle, ArrayList<Task>>();
		
		// Create an empty plan for all vehicles and find the vehicle with the biggest capacity
		int biggest_capacity = 0;
		int index_of_biggest_capacity = -1;
		int counter = 0;
		for (Vehicle v : vehicles) {
			// Add empty tasks
			action_task_list.put(v, new ArrayList<Integer>());
			vehicle_tasks.put(v, new ArrayList<Task>());
			
			// Add empty plan
			this.plans.add(Plan.EMPTY);
			
			// Find biggest vehicle
			int capacity_of_v = v.capacity();
			if (capacity_of_v > biggest_capacity) {
				biggest_capacity = capacity_of_v;
				index_of_biggest_capacity = counter;
			}
			counter++;
		}
		
		// Add each task to the vehicle with the biggest capacity
		Vehicle biggest_vehicle = vehicles.get(index_of_biggest_capacity);
		City current_city_for_vehicle = biggest_vehicle.getCurrentCity();
		Plan new_plan = new Plan(current_city_for_vehicle);
		ArrayList<Integer> partial_actions_for_vehicle = new ArrayList<Integer>();
		ArrayList<Task> task_per_vehicle = new ArrayList<Task>();
		for (Task t : task_list) {
			// move: current city to pickup location
			task_per_vehicle.add(t);
            for (City city : current_city_for_vehicle.pathTo(t.pickupCity)) {
            	new_plan.appendMove(city);
            }

            new_plan.appendPickup(t);
            partial_actions_for_vehicle.add(t.id);
            

            // move: pickup location to delivery location
            for (City city : t.path()) {
            	new_plan.appendMove(city);
            }

            new_plan.appendDelivery(t);
            partial_actions_for_vehicle.add(t.id + task_list.size());

            // set current city
            current_city_for_vehicle = t.deliveryCity;
		}

		this.plans.set(index_of_biggest_capacity, new_plan);
		action_task_list.replace(biggest_vehicle, partial_actions_for_vehicle);
		vehicle_tasks.replace(biggest_vehicle, task_per_vehicle);
		
		// Add the cost for this COPSolution
		set_solution_cost(this.plans, vehicles);
	}
	
	
	public COPSolution(COPSolution s) {
		this.plans = new ArrayList<Plan>();
		action_task_list = new LinkedHashMap<Vehicle, ArrayList<Integer>>();
		vehicle_tasks = new LinkedHashMap<Vehicle, ArrayList<Task>>();
		
		// Add action per tasks
		for (Entry<Vehicle, ArrayList<Integer>> entry : s.get_action_task_list().entrySet()) {
			ArrayList<Integer> l = new ArrayList<Integer>();
			Vehicle v = entry.getKey();
			for (Integer i : entry.getValue()) {
				l.add(i);
			}
			action_task_list.put(v, l);
		}
		
		// Add task per vehicle
		for (Entry<Vehicle, ArrayList<Task>> entry : s.get_task_per_vehicle().entrySet()) {
			ArrayList<Task> t = new ArrayList<Task>();
			Vehicle v = entry.getKey();
			for (Task i : entry.getValue()) {
				t.add(i);
			}
			vehicle_tasks.put(v, t);
		}
		
		// Add cost
		cost_of_all_plans = s.get_cost_of_solution();
	}
	
	
	public double set_solution_cost(List<Plan> plan_list, List<Vehicle> v_list) {
		double set_value = 0;
		
			for (int i = 0; i < plan_list.size(); i++) {
				set_value += (plan_list.get(i).totalDistance() * v_list.get(i).costPerKm());

			}
		this.cost_of_all_plans = set_value;
		return set_value;
	}
	
	
	public double get_cost_of_solution() {
		return cost_of_all_plans;
	}
	
	
	public List<Plan> get_plans() {
		return this.plans;
	}
	
	
	public Plan get_vehicle_plan(int index) {
		return this.plans.get(index);
	}
	
	
	public HashMap<Vehicle, ArrayList<Integer>> get_action_task_list() {
		return action_task_list;
	}
	
	
	public HashMap<Vehicle, ArrayList<Task>> get_task_per_vehicle() {
		return vehicle_tasks;
	}
	
	
	public List<Integer> get_index_of_possible_next_vechile(double counter, List<Task> task_list) {
		List<Integer> indexes = new ArrayList<Integer>();
		/* This can be used, but we decided on not.
		if (counter < (5 * task_list.size())) {
			int i = 0;
			int index = -1;
			int task_num = 0;
			for (Entry<Vehicle, ArrayList<Task>> entry : vehicle_tasks.entrySet()) {
				if (task_num < entry.getValue().size()) {
					task_num = entry.getValue().size();
					index = i;
				}
				i++;
			}
			indexes.add(index);
		} else { */
		
		for (int i = 0; i < this.plans.size(); i++) {
			if (this.plans.get(i).totalDistance() > 0) {
				indexes.add(i);
			}
		}
		

		return indexes;
	}
	
	
	// Buid the plans for this solution
	public boolean build_plan(List<Vehicle> v_list, List<Task> task_list) {
		// List<Plan> new_plans = new ArrayList<Plan>();
		boolean correct = true;
		int number_of_tasks = task_list.size();
		for (Vehicle v : v_list) {

			City cur_city = v.getCurrentCity();
			Plan new_plan = new Plan(cur_city);

			if (action_task_list.get(v).isEmpty()) {  // Add empty plan for vehicles with empty action_task_list
				this.plans.add(Plan.EMPTY);
			} else {
				int capacity = v.capacity();
				for (Integer i : action_task_list.get(v)) {
					if (i < task_list.size()) {  // pickup 
						capacity -= task_list.get(i).weight;
						for (City cp : cur_city.pathTo(task_list.get(i).pickupCity)) {
							new_plan.appendMove(cp);
						}
						cur_city = task_list.get(i).pickupCity;
						new_plan.appendPickup(task_list.get(i));
					} else {  // Delivery
						capacity += task_list.get(i % number_of_tasks).weight;
						for (City cd : cur_city.pathTo(task_list.get(i % number_of_tasks).deliveryCity)) {
							new_plan.appendMove(cd);
						}
						cur_city = task_list.get(i % number_of_tasks).deliveryCity;
						new_plan.appendDelivery(task_list.get(i % number_of_tasks));
					}
					if (capacity < 0) {
						return false;
					}
				}
				this.plans.add(new_plan);
			}
		}
		// plans = new_plans;
		set_solution_cost(this.plans, v_list);
		
		return correct;
	}
	
}
