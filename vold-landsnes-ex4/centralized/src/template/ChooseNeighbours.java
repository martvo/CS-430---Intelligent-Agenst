package template;

import java.util.ArrayList;
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
	
	public static List<COPSolution> getNeighbours(COPSolution A, List<Vehicle> v_list, TaskSet tasks) {
		List<COPSolution> neighbour_set = new ArrayList<COPSolution>();  // Want this to be a data structure that is easily sorted!
		
		// Choose one of the possible vehicles. A possible vehicle is one that don't have a plan with a distance of 0.0
		List<Integer> possible_indexes = A.get_index_of_possible_next_vechile();
		int random_index = r.nextInt(possible_indexes.size());
		Vehicle chosen_vehicle = v_list.get(random_index);
		
		// This loop is for changing a task between vehicles
		for (int i = 0; i < v_list.size(); i++) {
			Vehicle other_v = v_list.get(i);
			if (!chosen_vehicle.equals(other_v)) {
				
				// Choose a random task from the plan of the chosen_vehicle to remove
				ArrayList<Task> choosen_task_list = A.get_task_per_vehicle().get(chosen_vehicle);
				int random_task_index = r.nextInt(choosen_task_list.size());
				Task random_task = choosen_task_list.get(random_task_index);
				//System.out.println(random_task);
				
				// Check if the randomly chosen task fits
				if (other_v.capacity() >= random_task.weight) {
					ArrayList<COPSolution> neighbors_list = change_vehicle(A, random_task, chosen_vehicle, other_v, tasks);
					for (COPSolution s : neighbors_list) {
						if (s.get_cost_of_solution() >= A.get_cost_of_solution()) {
							neighbour_set.add(s);
						}
					}
				}
			}
		}
		return neighbour_set;
	}
	
	
	public static ArrayList<COPSolution> change_vehicle(COPSolution old_A, Task t, Vehicle chosen_vehicle, Vehicle other_vehicle, TaskSet tasks) {
		ArrayList<COPSolution> neighbours = new ArrayList<COPSolution>();
		
		for (int i = 0; i < old_A.get_action_task_list().size(); i++) {
			
			for (int j = i + 1; j < old_A.get_action_task_list().size(); j++) {
				COPSolution new_A = new COPSolution(old_A);
				Integer task_id = new_A.get_tasks_map().get(t);
				
				// Remove the task from chosen_vehicle and add to other_vehicle
				new_A.get_task_per_vehicle().get(chosen_vehicle).remove(t);
				new_A.get_task_per_vehicle().get(other_vehicle).add(t);
				
				// Remove the actions for the task in chosen_vehicle
				new_A.get_action_task_list().get(chosen_vehicle).remove(task_id);
				new_A.get_action_task_list().get(chosen_vehicle).remove(task_id + tasks.size());
				
				// Add the actions for task t to other vehicle in new_A (pickup on index i, delivery on index j)
				ArrayList<Integer> other_vehicle_actions = new_A.get_action_task_list().get(other_vehicle);
				other_vehicle_actions.add(i, task_id);
				other_vehicle_actions.add(j, task_id + tasks.size());
				
				// Create the plan and see if it's respects the constraints
				int capacity = other_vehicle.capacity();
				for (Integer n : other_vehicle_actions) {
					
				}
			}
		}
		
		return null;
	}


}
