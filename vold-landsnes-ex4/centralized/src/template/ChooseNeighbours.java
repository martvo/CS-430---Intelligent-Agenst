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
		// System.out.println("Possible indexes = " + possible_indexes.toString() + ", chosen index = " + possible_indexes.get(random_index));
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

						if (possible && s.get_cost_of_solution() < (1.1 * A.get_cost_of_solution())) {
							neighbour_set.add(s);
						}
					}
				}
				
				
			}
			
			
			// This loop is for sorting 
			// int tasks_of_vehicle = A.get_task_per_vehicle().get(chosen_vehicle).size();
			// ArrayList<Task> choosen_task_list = A.get_task_per_vehicle().get(chosen_vehicle);
			// int random_task_index = random_int(choosen_task_list.size());
			// Task random_task = choosen_task_list.get(random_task_index);
			
			
			if (counter > (3 * task_list.size())) {
				ArrayList<COPSolution> sorted_neighbours = change_task_order_vehicle(A, random_task, chosen_vehicle, task_list);
				
				for (COPSolution s : sorted_neighbours) {

					boolean possible = s.build_plan(v_list, task_list);

					if (possible && s.get_cost_of_solution() < (1.1 * A.get_cost_of_solution())) {
						neighbour_set.add(s);
					}
				}
			}
			
			
			/*
			for(Task task:A.get_task_per_vehicle().get(chosen_vehicle)) {
				
				ArrayList<COPSolution> sorted_neighbours = change_task_order_vehicle(A, task, chosen_vehicle, task_list);
				//System.out.println("call nr:" + lol + " of order changer");
				
				//System.out.println(sorted_neighbours);
				for (COPSolution s : sorted_neighbours) {
					// Builds plan for the neighbors, they should all work....
					//System.out.println("now before the the possible function");
					boolean possible = s.build_plan(v_list, task_list);
					// System.out.println("Cost for this neighbour: " + s.get_cost_of_solution());
					//System.out.println("Passed the possible function with value of, pssible =" + possible);
					if (possible && s.get_cost_of_solution() < A.get_cost_of_solution()) {
					//	System.out.println("Passed into the if sentence, now adding to neighbours");
						neighbour_set.add(s);
					//	System.out.println("Now added to the neighbour");
					}
				}
				
			}
			*/
			/*
			//other_neighbours = change_task_order_vehicle(A, random_task, chosen_vehicle, task_list);
			ArrayList<Task> choosen_task_list = A.get_task_per_vehicle().get(chosen_vehicle);
			int random_task_index = r.nextInt(choosen_task_list.size());
			Task random_task = choosen_task_list.get(random_task_index);
			System.out.println("Det random task is " + random_task + " with id: " + random_task.id);
			ArrayList<COPSolution> sorted_neighbours = change_task_order_vehicle_2(A, random_task, chosen_vehicle, task_list);
			*/
			
		}
		// System.out.println("Size of the final neighbour list " + neighbour_set.size());
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
					
					// System.out.println("New plan for other vehicle is: " + new_A.get_action_task_list().get(other_vehicle));
					
					// Create the plan and see if it's respects the constraints
					/*
					int capacity = other_vehicle.capacity();
					boolean possible = true;
					for (Integer n : new_A.get_action_task_list().get(other_vehicle)) {
						if (n < number_of_tasks) {  // pickup action
							capacity += task_list.get(n).weight;
						} else {  // delivery action
							capacity -= task_list.get(n % number_of_tasks).weight;
						}

						// Check to see if capacity is to large at any point
						if (capacity <  0) {
							possible = false;
						}
					}
					if (possible) {
						neighbours.add(new_A);
					}
					*/
					
					neighbours.add(new_A);
				}
			}
		}
		return neighbours;
	}
	
	


	public  ArrayList<COPSolution> change_task_order_vehicle(COPSolution old_A, Task t, Vehicle chosen_vehicle, List<Task> task_list) {
		
		ArrayList<COPSolution> neighbours = new ArrayList<COPSolution>();
		ArrayList<Integer> action_list = old_A.get_action_task_list().get(chosen_vehicle);		 
		int number_of_tasks = task_list.size();
		//System.out.println("In the change task order function! ");
		//Change order of pickups and deliveries and return a list of different solutions. 
		
		//If no tasks, nothing to change order of 
		if (old_A.get_task_per_vehicle().get(chosen_vehicle).isEmpty() || old_A.get_task_per_vehicle().size() <= 2) {
			COPSolution new_A = new COPSolution(old_A);				
			neighbours.add(new_A);
			//System.out.println("Empty taskset discovered in change_task_order");
			return neighbours;
			
		}	
		
		//Tasks exists for chosen_vehicle		
		else {
			
			int action_task_size = old_A.get_action_task_list().get(chosen_vehicle).size(); 
			//Integer task_id = t.id;
			
			/*
			//Get index 
			int pickup_index = -1;
			int deliver_index = -1;
			for (int x = 0; x < action_task_size; x++) {
				if (old_A.get_action_task_list().get(chosen_vehicle).get(x) == task_id) {
					pickup_index = x;
				} else if (old_A.get_action_task_list().get(chosen_vehicle).get(x) == (task_id + number_of_tasks)) {
					deliver_index = x ;
				}
			}
			*/
			/*
			COPSolution A_gen = new COPSolution(old_A);
			
			Integer pick = old_A.get_action_task_list().get(chosen_vehicle).remove(pickup_index);
			Integer deliver = old_A.get_action_task_list().get(chosen_vehicle).remove(deliver_index);
			*/
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
					
					
					//System.out.println(old_A.get_action_task_list().get(chosen_vehicle));
					
					A_gen.get_action_task_list().get(chosen_vehicle).remove(pickup_index);
					A_gen.get_action_task_list().get(chosen_vehicle).remove(deliver_index-1); //Because removed something above 
					//System.out.println(old_A.get_action_task_list().get(chosen_vehicle));
					if (i == action_task_size-1) {
						A_gen.get_action_task_list().get(chosen_vehicle).add(task_id); //Appender 
						// new_A.get_action_task_list().get(other_vehicle).add(task_id + number_of_tasks);
					} else 
						A_gen.get_action_task_list().get(chosen_vehicle).add(i, task_id); 
					//System.out.println("In the first else the vehicle tasks are " + old_A.get_action_task_list().get(chosen_vehicle));
					// if j equal to the new action_task_size -> add the deliver action to the back, else add it at index j
					if (j == action_task_size) {
						A_gen.get_action_task_list().get(chosen_vehicle).add(task_id + number_of_tasks);
					} else {
						A_gen.get_action_task_list().get(chosen_vehicle).add(j, (task_id + number_of_tasks));
					}
					neighbours.add(A_gen);
				}
				
			}
			//System.out.println("The indexes's to be changed are pickup: " + pickup_index + "and delivery: " + deliver_index);
			//System.out.println("Before reordering" + A_gen.get_action_task_list().get(chosen_vehicle));
			//System.out.println("Before reordering" + new_A.get_action_task_list().get(chosen_vehicle).size());
			
			//new_A.get_action_task_list().get(chosen_vehicle).remove(deliver_index);
			//new_A.get_action_task_list().get(chosen_vehicle).add(task_id + number_of_tasks);
			
			//System.out.println("After reordering" + A_gen.get_action_task_list().get(chosen_vehicle));
			
			/*
			
			for (int i = pickup_index; i < action_task_size-1; i++) {  //			
				
				//We're at first iteration, wait to sort 
				if(i == pickup_index) {
					//System.out.println("In the i == pickup_index");
				}
				else if(i == action_task_size - 1) {
					//No more solutions, pickup is at second last index		
					
					return neighbours;				
				}
				else{ //Don't try to access -1 element if pickup_index == 0 
					
					//Swap indexes
					
					Integer task_index_p = A_gen.get_action_task_list().get(chosen_vehicle).get(i-1);
					Integer task_index_d = A_gen.get_action_task_list().get(chosen_vehicle).get(i);
					Integer next_index_p = A_gen.get_action_task_list().get(chosen_vehicle).get(i+1);
					
					
					A_gen.get_action_task_list().get(chosen_vehicle).remove(i+1);
					A_gen.get_action_task_list().get(chosen_vehicle).add(i+1,task_index_d);
					
					A_gen.get_action_task_list().get(chosen_vehicle).remove(i);
					A_gen.get_action_task_list().get(chosen_vehicle).add(i,task_index_p);
					
						
					A_gen.get_action_task_list().get(chosen_vehicle).remove(i-1);
					A_gen.get_action_task_list().get(chosen_vehicle).add(i-1, next_index_p);
					
					//A_gen.get_action_task_list().get(chosen_vehicle).remove(i+1);
					//A_gen.get_action_task_list().get(chosen_vehicle).add(i+1, task_index_p);
					
					//Create new solution to remember the last swap for future iterations
					//System.out.println("After modifications " + A_gen.get_action_task_list().get(chosen_vehicle));
					
				}
				//
				COPSolution A_mem_p = new COPSolution(A_gen);
				COPSolution A_mem_d = new COPSolution(A_mem_p);
				
				if(isValid(A_mem_p, number_of_tasks, chosen_vehicle)){
					neighbours.add(A_mem_p);
				}
				
				
				
							
				//System.out.println("In first loop sorting process " + A_mem_d.get_action_task_list().get(chosen_vehicle));
				for (int j = i +1; j < action_task_size-1; j++) {  //+i to not deliver before pickup, -1 to be in range			
					
					
					
					//Swap indexes
					Integer task_index_d = A_mem_d.get_action_task_list().get(chosen_vehicle).get(j);
					Integer change_index_d = A_mem_d.get_action_task_list().get(chosen_vehicle).get(j+1);
					
					A_mem_d.get_action_task_list().get(chosen_vehicle).remove(j+1);
					A_mem_d.get_action_task_list().get(chosen_vehicle).add(j+1, task_index_d);
					A_mem_d.get_action_task_list().get(chosen_vehicle).remove(j);
					A_mem_d.get_action_task_list().get(chosen_vehicle).add(j, change_index_d);
					
					
					
					
					//System.out.println("In sorting process " + A_mem_d.get_action_task_list().get(chosen_vehicle));
					if(isValid(A_mem_d, number_of_tasks, chosen_vehicle)){
						neighbours.add(A_mem_d);
					}
					
				}
				
			}
			*/
			return neighbours; 
		}

	}

//Return false if the action plan is invalid according to definitions 
	public static boolean isValid(COPSolution A, int task_size, Vehicle chosen_vehicle ) {
		//check that the
		//System.out.println("In valid checker");
		boolean checker = true;
		ArrayList<Integer> action_list = A.get_action_task_list().get(chosen_vehicle);
		
		//System.out.println(action_list);
		//System.out.println("");
		
		for(int i = 0; i < action_list.size(); i++) {
			 Integer first_number = action_list.get(i);
			 //if first element is delivery 
			 if(first_number >= task_size && i == 0) {
				 return false; 
			 }
			 //Deliveries
			 else if(first_number >= task_size) {
				 for(int j = 0; j < action_list.size(); j++) {
					 //If we found the pickup and it's after the delivery 
					 //System.out.println("In the loop of checking values in isValid");					 
						if(action_list.get(j) == (first_number - task_size) && j>=i) {
							checker = false;
							//System.out.println("The valid checker is false with value:" + checker);
						}
					} 
			 }
				
		}
		//System.out.println("");
		//System.out.println("The valid checker gave following result :" + checker);
		//System.out.println("");
		return checker; 
	}
		
		
}
	



