package template;

import java.awt.Color;

import logist.simulation.Vehicle;
import logist.task.TaskSet;
import logist.topology.Topology.City;

public class AdverseriVehicle implements Vehicle {
	
	private int capacity;
	private City homecity;
	private int costPerKm;
	
	public AdverseriVehicle(int capacity, int costPerKm, City homecity) {
		this.capacity = capacity;
		this.costPerKm = costPerKm;
		this.homecity = homecity;
	}
	
	public int capacity() {
		// TODO Auto-generated method stub
		return capacity;
	}
	
	public int costPerKm() {
		// TODO Auto-generated method stub
		return costPerKm;
	}
	
	public City getCurrentCity() {
		// TODO Auto-generated method stub
		return homecity;
	}

	@Override
	public int id() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String name() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public City homeCity() {
		// TODO Auto-generated method stub
		return homecity;
	}

	@Override
	public double speed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public TaskSet getCurrentTasks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getReward() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDistanceUnits() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getDistance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Color color() {
		// TODO Auto-generated method stub
		return null;
	}

}
