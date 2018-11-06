package template;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import logist.simulation.Vehicle;
import logist.task.Task;

import template.COPSolution;

public class ChooseNeighbours {
	
	Random r = new Random(1234);
	
	
	public int random_int(int i){
        return r.nextInt(i);
    }
	
	
	public double random_double() {
		return r.nextDouble();
	}
	
	
	public List<COPSolution> getNeighbours(COPSolution A, List<Vehicle> v_list, List<Task> task_list, double counter) {
		List<COPSolution> neighbour_set = new ArrayList<COPSolution>();  // Want this to be a data structure that is easily sorted!
		
		// Choose one of the possible vehicles. A possible vehicle is one that don't have a plan with a distance of 0.0
		List<Integer> possible_indexes = A.get_index_of_possible_next_vechile(counter, task_list);
		if (possible_indexes.isEmpty()) {
			return neighbour_set;
		}
		int random_index = random_int(possible_indexes.size());
		
		Vehicle chosen_vehicle = v_list.get(possible_indexes.get(random_index));
		
		// Choose a random task from the plan of the chosen_vehicle to remove
		ArrayList<Task> choosen_task_list = A.get_task_per_vehicle().get(chosen_vehicle);
		int random_task_index = random_int(choosen_task_list.size());
		Task random_task = choosen_task_list.get(random_task_index);
		
		// This loop is for changing a task between vehicles
		for (int i = 0; i < v_list.size(); i++) {
			Vehicle other_v = v_list.get(i);
			
			if (!chosen_vehicle.equals(other_v)) {
				
				// Check if the randomly chosen task fits
				if (other_v.capacity() >= random_task.weight) {
					
					ArrayList<COPSolution> neighbors_list = change_vehicle(A, random_task, chosen_vehicle, other_v, v_list, task_list);

					for (COPSolution s : neighbors_list) {
						// Builds plan for the neighbors, they should all work....
						boolean possible = s.build_plan(v_list, task_list);

						if (possible && s.get_cost_of_solution() < (1.2 * A.get_cost_of_solution())) {
							neighbour_set.add(s);
						}
					}
				}
				
				
			}
			
			if (counter > (3 * task_list.size())) {
				ArrayList<COPSolution> sorted_neighbours = change_task_order_vehicle(A, random_task, chosen_vehicle, task_list);
				
				for (COPSolution s : sorted_neighbours) {

					boolean possible = s.build_plan(v_list, task_list);

					if (possible && s.get_cost_of_solution() < (1.2 * A.get_cost_of_solution())) {
						neighbour_set.add(s);
					}
				}
			}
		}
		return neighbour_set;
	}
	
	
	public ArrayList<COPSolution> change_vehicle(COPSolution old_A, Task t, Vehicle chosen_vehicle, Vehicle other_vehicle, List<Vehicle> v_list, List<Task> task_list) {
		ArrayList<COPSolution> neighbours = new ArrayList<COPSolution>();
		int number_of_tasks = task_list.size();
		
		// If the vehicle don't have any task, just add them if there is place
		if (old_A.get_task_per_vehicle().get(other_vehicle).isEmpty()) {
			
			COPSolution new_A = new COPSolution(old_A);
			Integer task_id = t.id; 
			
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
					Integer task_id = t.id;
					
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
					
					neighbours.add(new_A);
				}
			}
		}
		return neighbours;
	}
	
	


	public  ArrayList<COPSolution> change_task_order_vehicle(COPSolution old_A, Task t, Vehicle chosen_vehicle, List<Task> task_list) {
		
		ArrayList<COPSolution> neighbours = new ArrayList<COPSolution>();
		// ArrayList<Integer> action_list = old_A.get_action_task_list().get(chosen_vehicle);		 
		int number_of_tasks = task_list.size();
		//Change order of pickups and deliveries and return a list of different solutions. 
		
		//If no tasks, nothing to change order of 
		if (old_A.get_task_per_vehicle().get(chosen_vehicle).isEmpty() || old_A.get_task_per_vehicle().size() <= 2) {
			COPSolution new_A = new COPSolution(old_A);				
			neighbours.add(new_A);
			return neighbours;
			
		}	
		
		//Tasks exists for chosen_vehicle		
		else {
			
			int action_task_size = old_A.get_action_task_list().get(chosen_vehicle).size(); 
			for (int i = 0; i < action_task_size - 1; i++) {  //
				//old_A.get_action_task_list().get(chosen_vehicle).add(i, pick);
				for (int j = i +1; j < action_task_size; j++) {  //+i to not deliver before pickup, -1 to be in range
					
					COPSolution A_gen = new COPSolution(old_A);
					Integer task_id = t.id;
					
					int pickup_index = -1;
					int deliver_index = -1;
					for (int x = 0; x < A_gen.get_action_task_list().get(chosen_vehicle).size(); x++) {
						if (A_gen.get_action_task_list().get(chosen_vehicle).get(x) == task_id) {
							pickup_index = x;
						} else if (A_gen.get_action_task_list().get(chosen_vehicle).get(x) == (task_id + number_of_tasks)) {
							deliver_index = x ;
						}
					}
					
					A_gen.get_action_task_list().get(chosen_vehicle).remove(pickup_index);
					A_gen.get_action_task_list().get(chosen_vehicle).remove(deliver_index-1); // -1 because removed pickup task that always is in the front of the delivery

					if (i == action_task_size-1) {
						A_gen.get_action_task_list().get(chosen_vehicle).add(task_id); //Appender 
						// new_A.get_action_task_list().get(other_vehicle).add(task_id + number_of_tasks);
					} else 
						A_gen.get_action_task_list().get(chosen_vehicle).add(i, task_id); 

					// if j equal to the new action_task_size -> add the deliver action to the back, else add it at index j
					if (j == action_task_size) {
						A_gen.get_action_task_list().get(chosen_vehicle).add(task_id + number_of_tasks);
					} else {
						A_gen.get_action_task_list().get(chosen_vehicle).add(j, (task_id + number_of_tasks));
					}
					neighbours.add(A_gen);
				}
				
			}
			return neighbours; 
		}

	}
		
}
	



