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
	
	public static Set<COPSolution> getNeighbours(COPSolution A, List<Vehicle> v_list, TaskSet tasks) {
		List<COPSolution> neighbour_set = new ArrayList<COPSolution>();  // Want this to be a data structure that is easily sorted!
		
		// Choose one of the possible vehicles. A possible vehicle is one that don't have a plan with a distance of 0.0
		List<Integer> possible_indexes = A.get_index_of_possible_next_vechile();
		int random_index = r.nextInt(possible_indexes.size());
		Vehicle chosen_vehicle = v_list.get(random_index);
		// This loop is for changing a task between vehicles
		for (int i = 0; i < v_list.size(); i++) {
			Vehicle other_v = v_list.get(i);
			if (!chosen_vehicle.equals(other_v)) {
				// Bør vell lage en kopi av nåværenede solution hær! så jeg kan endre på den så my som jeg vil uten at det påvirker den vi har hær....
				COPSolution new_solution = new COPSolution(A);  // Må kunne lage en ny instans med egne lister!!
				// Usikker på vi trenger å lage en ny instans hær.... Mulig å bruke A, gjør jo ikke noe med den nye, bare henter ut info fra den.....
				
				// Choose a random task from the plan of the chosen_vehicle to remove
				ArrayList<Task> choosen_task_list = new_solution.get_task_per_vehicle().get(chosen_vehicle);
				int random_task_index = r.nextInt(choosen_task_list.size());
				Task random_task = choosen_task_list.get(random_task_index);
				
				// Check if this task fits the other_v vehicle
				int capacity_of_other_vehicle = other_v.capacity();
				for (Task t : new_solution.get_task_per_vehicle().get(other_v)) {
					capacity_of_other_vehicle -= t.weight;
				}
				
				// Check if the randomly chosen task fits
				if (capacity_of_other_vehicle >= random_task.weight) {
					ArrayList<COPSolution> neighbors_list = change_vehicle(new_solution, random_task, chosen_vehicle, other_v);
					for (COPSolution s : neighbors_list) {
						if (s.get_cost_of_solution() >= new_solution.get_cost_of_solution()) {
							neighbour_set.add(s);
						}
					}
				}
			}
		}
		return null;
	}
	
	
	public static ArrayList<COPSolution> change_vehicle(COPSolution old_A, Task t, Vehicle chosen_vehicle, Vehicle other_vehicle) {
		COPSolution new_A = new COPSolution(old_A);
		
		// Remove the task from choosen_vehicle and add to other_vehicle
		new_A.get_task_per_vehicle().get(chosen_vehicle).remove(t);
		new_A.get_task_per_vehicle().get(other_vehicle).add(t);
		
		Action a = new Action.Pickup(t);
		System.out.println(a.hashCode());
		
		// Hær har æ prøvd my rart no, men ingenting funke.... Koss ska æ få ut actions dra en liste med actions...
		// Remove the actions to get the chosen task
		ArrayList<Action> chosen_vehicle_actions = new_A.get_action_task_list().get(chosen_vehicle);
		for (int i = 0; i < chosen_vehicle_actions.size(); i++) {
			System.out.println(chosen_vehicle_actions.get(i).hashCode());
			if (chosen_vehicle_actions.get(i).hashCode() == a.hashCode()) {
				
			}
		}
		System.out.println(t);
		System.out.println(new_A.get_action_task_list().get(chosen_vehicle).equals(a));
		boolean b = new_A.get_action_task_list().get(chosen_vehicle).remove(new Action.Pickup(t));
		System.out.println(b);
		new_A.get_action_task_list().get(chosen_vehicle).remove(new Action.Delivery(t));
		
		
		return null;
	}


}
