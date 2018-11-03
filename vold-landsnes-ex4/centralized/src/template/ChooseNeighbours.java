package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import logist.plan.Action;
import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

import template.COPSolution;

public class ChooseNeighbours {
	
	static Random r = new Random();
	
	public static List<COPSolution> getNeighbours(COPSolution A, List<Vehicle> v_list, TaskSet tasks, HashMap<Task, Integer> tasks_map, HashMap<Integer, Task> int_to_task) {
		List<COPSolution> neighbour_set = new ArrayList<COPSolution>();  // Want this to be a data structure that is easily sorted!
		
		// Choose one of the possible vehicles. A possible vehicle is one that don't have a plan with a distance of 0.0
		List<Integer> possible_indexes = A.get_index_of_possible_next_vechile();
		if (possible_indexes.isEmpty()) {
			return neighbour_set;
		}
		int random_index = r.nextInt(possible_indexes.size());
		Vehicle chosen_vehicle = v_list.get(possible_indexes.get(random_index));
		
		// This loop is for changing a task between vehicles
		for (int i = 0; i < v_list.size(); i++) {
			Vehicle other_v = v_list.get(i);
			
			if (!chosen_vehicle.equals(other_v)) {
				// Choose a random task from the plan of the chosen_vehicle to remove
				ArrayList<Task> choosen_task_list = A.get_task_per_vehicle().get(chosen_vehicle);
				int random_task_index = r.nextInt(choosen_task_list.size());
				Task random_task = choosen_task_list.get(random_task_index);
				
				// Check if the randomly chosen task fits
				if (other_v.capacity() >= random_task.weight) {
					ArrayList<COPSolution> neighbors_list = change_vehicle(A, random_task, chosen_vehicle, other_v, tasks_map, int_to_task);
					System.out.println("Size for returned neightbour list " + neighbors_list.size());
					for (COPSolution s : neighbors_list) {
						// Builds plan for the neighbors, they should all work....
						s.build_plan(int_to_task, v_list);
						// System.out.println("Cost for this neighbour: " + s.get_cost_of_solution());
						if (s.get_cost_of_solution() <= A.get_cost_of_solution()) {
							neighbour_set.add(s);
						}
					}
				}
			}
		}
		System.out.println("Size of the final neighbour list " + neighbour_set.size());
		return neighbour_set;
	}
	
	
	public static ArrayList<COPSolution> change_vehicle(COPSolution old_A, Task t, Vehicle chosen_vehicle, Vehicle other_vehicle, HashMap<Task, Integer> tasks_map,
			HashMap<Integer, Task> int_to_task) {
		ArrayList<COPSolution> neighbours = new ArrayList<COPSolution>();
		int number_of_tasks = tasks_map.size();
		
		// If the vehicle don't have any task, just add them if there is place
		if (old_A.get_task_per_vehicle().get(other_vehicle).isEmpty()) {
			
			COPSolution new_A = new COPSolution(old_A);
			Integer task_id = tasks_map.get(t); 
			
			// Remove the task from chosen_vehicle and add to other_vehicle
			new_A.get_task_per_vehicle().get(chosen_vehicle).remove(t);
			new_A.get_task_per_vehicle().get(other_vehicle).add(t);
			
			// Find the index for the pickup and deliver action
			int pickup_index = -1;
			int deliver_index = -1;
			for (int i = 0; i < new_A.get_action_task_list().get(chosen_vehicle).size(); i++) {
				if (new_A.get_action_task_list().get(chosen_vehicle).get(i) == task_id) {
					pickup_index = i;
				} else if (new_A.get_action_task_list().get(chosen_vehicle).get(i) == (task_id + number_of_tasks)) {
					deliver_index = i;
				}
			}
			
			// Remove the actions for the task in chosen_vehicle
			new_A.get_action_task_list().get(chosen_vehicle).remove(pickup_index);
			new_A.get_action_task_list().get(chosen_vehicle).remove(deliver_index - 1);  // -1 because after the pickup action is removed the elements on the right side get shifted one to the left
			
			// Add the actions for task t to other vehicle in new_A (pickup on index i, delivery on index j)
			new_A.get_action_task_list().get(other_vehicle).add(task_id);
			new_A.get_action_task_list().get(other_vehicle).add(task_id + number_of_tasks);
			
			// System.out.println("New plan for other vehicle is: " + new_A.get_action_task_list().get(other_vehicle));
			
			neighbours.add(new_A);
		} else {
			int action_task_size = old_A.get_action_task_list().get(other_vehicle).size();
			for (int i = 0; i < action_task_size + 1; i++) {  // +1 because we want to be able to put the action at the end
				for (int j = i + 1; j < action_task_size + 2; j++) {  // +2 because we want to be able to put the action at the end and at the end after the pickup is added at the end
					COPSolution new_A = new COPSolution(old_A);
					Integer task_id = tasks_map.get(t);
					
					// Remove the task from chosen_vehicle and add to other_vehicle
					new_A.get_task_per_vehicle().get(chosen_vehicle).remove(t);
					new_A.get_task_per_vehicle().get(other_vehicle).add(t);
					
					// Find the index for the pickup and deliver action
					int pickup_index = -1;
					int deliver_index = -1;
					for (int x = 0; x < new_A.get_action_task_list().get(chosen_vehicle).size(); x++) {
						if (new_A.get_action_task_list().get(chosen_vehicle).get(x) == task_id) {
							pickup_index = x;
						} else if (new_A.get_action_task_list().get(chosen_vehicle).get(x) == (task_id + number_of_tasks)) {
							deliver_index = x ;
						}
					}
					// Remove the actions for the task in chosen_vehicle
					new_A.get_action_task_list().get(chosen_vehicle).remove(pickup_index);
					new_A.get_action_task_list().get(chosen_vehicle).remove(deliver_index - 1); // -1 because after the pickup action is removed the elements on the right side get shifted one to the left
					
					// Add the actions for task t to other vehicle in new_A (pickup on index i, delivery on index j)
					// If i == action_task_size -> add both to the end of the list, else add pickup at index i
					if (i == action_task_size) {
						new_A.get_action_task_list().get(other_vehicle).add(task_id);
						// new_A.get_action_task_list().get(other_vehicle).add(task_id + number_of_tasks);
					} else 
						new_A.get_action_task_list().get(other_vehicle).add(i, task_id);
					
					// if j equal to the new action_task_size -> add the deliver action to the back, else add it at index j
					if (j == action_task_size + 1) {
						new_A.get_action_task_list().get(other_vehicle).add(task_id + number_of_tasks);
					} else 
						new_A.get_action_task_list().get(other_vehicle).add(j, (task_id + number_of_tasks));
					
					// System.out.println("New plan for other vehicle is: " + new_A.get_action_task_list().get(other_vehicle));
					
					// Create the plan and see if it's respects the constraints
					int capacity = other_vehicle.capacity();
					boolean possible = true;
					for (Integer n : new_A.get_action_task_list().get(other_vehicle)) {
						if (n < number_of_tasks) {  // pickup action
							capacity += int_to_task.get(n).weight;
						} else {  // delivery action
							capacity -= int_to_task.get(n % number_of_tasks).weight;
						}

						// Check to see if capacity is to large at any point
						if (capacity <  0) {
							possible = false;
						}
					}
					if (possible) {
						neighbours.add(new_A);
					}
				}
			}
		}
		return neighbours;
	}


}
