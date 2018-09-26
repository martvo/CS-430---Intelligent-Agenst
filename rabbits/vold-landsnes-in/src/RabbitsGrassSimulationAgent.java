import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {
	
	private int x;
	private int y;
	private int vX;
	private int vY;
	private int energy;
	private int stepsToLive;
	private static int IDNumber = 0;
	private int ID;
	private RabbitsGrassSimulationSpace rgSpace;
	private int energyToReproduce;
	private int costOfReproduction;
	private int costOfMovment;
	
	public RabbitsGrassSimulationAgent(int minLifespan, int maxLifespan, int startEnergy, int energyToReproduce, int costOfReproduction, 
			int costOfMovment){
	    x = -1;
	    y = -1;
	    energy = startEnergy;
	    setVxVy();
	    stepsToLive = (int)((Math.random() * (maxLifespan - minLifespan)) + minLifespan);
	    IDNumber++;
	    ID = IDNumber;
	    this.energyToReproduce = energyToReproduce;
	    this.costOfReproduction = costOfReproduction;
	    this.costOfMovment = costOfMovment;
	}
	
	
	private void setVxVy() {
	    vX = 0;
	    vY = 0;
	    
	    int randomNum = (int)((Math.random() * 4) + 1);
	    
	    if (randomNum == 1) {
	    	vX = 1;
		    vY = 0;
	    } else if (randomNum == 2) {
	    	vX = -1;
		    vY = 0;
	    } else if (randomNum == 3) {
	    	vX = 0;
		    vY = 1;
	    } else if (randomNum == 4) {
	    	vX = 0;
		    vY = -1;
	    } else System.out.println("FEIIIIIIIIIIIIIIIIIL RANDOM");
	    
	    /*
	    while ((vX == 0) && (vY == 0)) {
	    	vX = (int)Math.floor(Math.random() * 3) - 1;
	    	vY = (int)Math.floor(Math.random() * 3) - 1;
	    }
	    */
	}
	
	
	public void setRabbitsGrassSimulationSpace(RabbitsGrassSimulationSpace rgs) {
	    rgSpace = rgs;
	}
	
	
	public String getID() {
	    return "A-" + ID;
	}
	
	
	public int getEnergy() {
	    return energy;
	}
	
	
	public int getStepsToLive(){
	    return stepsToLive;
	}

	
	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		if (stepsToLive > 10)
			arg0.drawFastRoundRect(Color.white);
		else
			arg0.drawFastRoundRect(Color.gray);
	}
	
	
	public void setXY(int newX, int newY) {
	    x = newX;
	    y = newY;
	}

	
	public int getX() {
		// TODO Auto-generated method stub
		return x;
	}

	
	public int getY() {
		// TODO Auto-generated method stub
		return y;
	}
	
	
	public void report(){
	    System.out.println(getID() + " at " + getX() + ", " + getY() + " has " +  getEnergy() + " energy and " + getStepsToLive() 
	    	+ " steps to live.");
	}
	
	
	public boolean step() {
		int newX = x + vX;
	    int newY = y + vY;

	    Object2DGrid grid = rgSpace.getCurrentAgentSpace();
	    newX = (newX + grid.getSizeX()) % grid.getSizeX();
	    newY = (newY + grid.getSizeY()) % grid.getSizeY();
		
	    if (tryMove(newX, newY)) {
	    	energy += rgSpace.eatGrassAt(x, y);
	    } else setVxVy();
	    
	    boolean reproducec = false;
	    if (this.energy >= this.energyToReproduce) {
	    	reproducec = true;
	    	energy -= costOfReproduction;
	    }
	    stepsToLive--;
	    setVxVy();
	    return reproducec;
	}
	
	
	public void costOfMovment() {
		energy -= costOfMovment;
	}
	
	
	private boolean tryMove(int newX, int newY){
	    return rgSpace.moveAgentAt(x, y, newX, newY);
	}

}
