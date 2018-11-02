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
	private HashMap<Task, Integer> tasks_map;
	private double cost_of_all_plans;
	
	
	public COPSolution(List<Vehicle> vehicles, List<Task> tasks) {
		cost_of_all_plans = 0;
		plans = new ArrayList<Plan>();
		action_task_list = new LinkedHashMap<Vehicle, ArrayList<Integer>>();
		vehicle_tasks = new LinkedHashMap<Vehicle, ArrayList<Task>>();
		tasks_map = new HashMap<Task, Integer>();
		
		// Create an empty plan for all vehicles and find the vehicle with the biggest capacity
		int biggest_capacity = 0;
		int index_of_biggest_capacity = -1;
		int counter = 0;
		for (Vehicle v : vehicles) {
			// Add empty tasks
			action_task_list.put(v, new ArrayList<Integer>());
			vehicle_tasks.put(v, new ArrayList<Task>());
			
			// Add empty plan
			plans.add(Plan.EMPTY);
			
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
		for (Task t : tasks) {
			// move: current city to pickup location
			tasks_map.put(t, t.id);
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
            partial_actions_for_vehicle.add(t.id + tasks.size());

            // set current city
            current_city_for_vehicle = t.deliveryCity;
		}
		plans.set(index_of_biggest_capacity, new_plan);
		action_task_list.replace(biggest_vehicle, partial_actions_for_vehicle);
		vehicle_tasks.replace(biggest_vehicle, task_per_vehicle);
		
		// *********************
		// DO WE NEED TO ADD THE EMPTY ACTION TO THE PLAN?????????????
		// *************
		
		// Add the cost for this COPSolution
		set_solution_cost(plans, vehicles);
	}
	
	
	public COPSolution(COPSolution s) {
		plans = new ArrayList<Plan>();
		action_task_list = new LinkedHashMap<Vehicle, ArrayList<Integer>>();
		vehicle_tasks = new LinkedHashMap<Vehicle, ArrayList<Task>>();
		
		// Add plans
		for (Plan p : s.get_plans()) {
			plans.add(p);
		}
		
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
		
		// Add task map
		tasks_map = s.get_tasks_map();
		
		// Add cost
		cost_of_all_plans = s.get_cost_of_solution();
	}
	
	
	public double set_solution_cost(List<Plan> plan_list, List<Vehicle> v_lsit) {
		double set_value = 0;
			for (int i = 0; i < plan_list.size(); i++) {
				set_value += (plan_list.get(i).totalDistance() * v_lsit.get(i).costPerKm());
			}
		this.cost_of_all_plans = set_value;
		return set_value;
	}
	
	
	public double get_cost_of_solution() {
		return cost_of_all_plans;
	}
	
	
	public List<Plan> get_plans() {
		return plans;
	}
	
	
	public Plan get_vehicle_plan(int index) {
		return plans.get(index);
	}
	
	
	public HashMap<Vehicle, ArrayList<Integer>> get_action_task_list() {
		return action_task_list;
	}
	
	
	public HashMap<Vehicle, ArrayList<Task>> get_task_per_vehicle() {
		return vehicle_tasks;
	}
	
	
	public HashMap<Task, Integer> get_tasks_map() {
		return tasks_map;
	}
	
	
	public List<Integer> get_index_of_possible_next_vechile() {
		List<Integer> indexes = new ArrayList<Integer>();
		for (int i = 0; i < this.plans.size(); i++) {
			if (plans.get(i).totalDistance() > 0) {
				indexes.add(i);
			}
		}
		return indexes;
	}
	
	// ********* FÅR SE OM DENNE TRENGS, MULIG JEG GJØR DET I ChooseNeighbours.java
	public void change_vehicle(Task t, Vehicle choosen_vehicle, Vehicle other_vehicle) {
		// Remove the task from choosen_vehicle and add to other_vehicle
		vehicle_tasks.get(choosen_vehicle).remove(t);
		vehicle_tasks.get(other_vehicle).add(t);
		 
		// Update the action_task_list for both 
		
	}

}
