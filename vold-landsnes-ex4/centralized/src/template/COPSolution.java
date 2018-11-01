package template;

import java.util.ArrayList;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.topology.Topology.City;

public class COPSolution {
	
	private List<Plan> plans;
	private double cost_of_all_plans;
	
	
	public COPSolution(List<Vehicle> vehicles, List<Task> tasks) {
		cost_of_all_plans = 0;
		plans = new ArrayList<Plan>();
		
		// Create an empty plan for all vehicles and find the vehicle with the biggest capacity
		int biggest_capacity = 0;
		int index_of_biggest_capacity = -1;
		int counter = 0;
		for (Vehicle v : vehicles) {
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
		for (Task t : tasks) {
			// move: current city to pickup location
            for (City city : current_city_for_vehicle.pathTo(t.pickupCity)) {
            	new_plan.appendMove(city);
            }

            new_plan.appendPickup(t);

            // move: pickup location to delivery location
            for (City city : t.path()) {
            	new_plan.appendMove(city);
            }

            new_plan.appendDelivery(t);

            // set current city
            current_city_for_vehicle = t.deliveryCity;
		}
		plans.set(index_of_biggest_capacity, new_plan);
		
		// *********************
		// DO WE NEED TO ADD THE EMPTY ACTION TO THE PLAN?????????????
		// *************
		
		// Add the cost for this COPSolution
		set_solution_cost(plans, vehicles);
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

}
