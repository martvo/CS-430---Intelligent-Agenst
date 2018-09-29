import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */

public class RabbitsGrassSimulationSpace {
	
	private Object2DGrid grassSpace;
	private Object2DGrid agentSpace;
	private int energyInGrass;
	
	
	public RabbitsGrassSimulationSpace(int xSize, int ySize, int energyInGrass) {
		grassSpace = new Object2DGrid(xSize, ySize);
		agentSpace = new Object2DGrid(xSize, ySize);
		this.energyInGrass = energyInGrass;
		
		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				grassSpace.putObjectAt(i,j,new Integer(0));
			}
		}
	}
	

	public void spreadGrass(int grass) {
	    // Randomly place money in moneySpace
	    for (int i = 0; i < grass; i++) {
	    	
	    	int currentValue = 1;
	    	
	    	while (currentValue != 0) {
	    		// Choose coordinates
	  	      	int x = (int)(Math.random()*(grassSpace.getSizeX()));
	  	      	int y = (int)(Math.random()*(grassSpace.getSizeY()));
	  	      	
	  	      	// Get the value of the object at those coordinates
		    	currentValue = getGrassAt(x, y);
		    	
		    	if (currentValue == 0) {
		    		// Replace the Integer object with another one with the new value
		    		int grassMultiplyer = (int)((Math.random() * (3 - 1)) + 1);
				    grassSpace.putObjectAt(x, y, new Integer(energyInGrass * grassMultiplyer));
				    break;
		    	}
	    	}
	    }
	}
	
	
	public int getGrassAt(int x, int y){
	    int i;
	    if (grassSpace.getObjectAt(x,y) != null) {
	    	i = ((Integer)grassSpace.getObjectAt(x, y)).intValue();
	    } else {
	    	i = 0;
	    }
	    return i;
	}
	
	
	public Object2DGrid getCurrentGrassSpace() {
		return grassSpace;
	}
	
	
	public Object2DGrid getCurrentAgentSpace(){
	    return agentSpace;
	  }
	
	
	public boolean isCellOccupied(int x, int y) {
	    boolean retVal = false;
	    if (agentSpace.getObjectAt(x, y) != null) retVal = true;
	    return retVal;
	}
	
	
	public boolean addAgent(RabbitsGrassSimulationAgent agent) {
	    boolean retVal = false;
	    int count = 0;
	    int countLimit = 10 * agentSpace.getSizeX() * agentSpace.getSizeY();

	    while ((retVal == false) && (count < countLimit)) {
	    	int x = (int)(Math.random()*(agentSpace.getSizeX()));
	    	int y = (int)(Math.random()*(agentSpace.getSizeY()));
	    	if (isCellOccupied(x,y) == false) {
	    		agentSpace.putObjectAt(x, y, agent);
	    		agent.setXY(x, y);
	    		agent.setRabbitsGrassSimulationSpace(this);
	    		retVal = true;
	    	}
	    	count++;
	    }
	    return retVal;
	}
	
	
	public void removeAgentAt(int x, int y) {
	    agentSpace.putObjectAt(x, y, null);
	}
	
	
	public int eatGrassAt(int x, int y) {
		int energy = getGrassAt(x, y);
	    grassSpace.putObjectAt(x, y, new Integer(0));
	    return energy;
	}
	
	
	public boolean moveAgentAt(int x, int y, int newX, int newY) {
	    boolean retVal = false;
	    if (!isCellOccupied(newX, newY)) {
	    	RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentSpace.getObjectAt(x, y);
	    	removeAgentAt(x,y);
	    	rga.setXY(newX, newY);
	    	agentSpace.putObjectAt(newX, newY, rga);
	    	rga.costOfMovment();
	    	retVal = true;
	    }
	    return retVal;
	  }
	
	public int getTotalGrass() {
		int totalGrass = 0;
		for (int i = 0; i < grassSpace.getSizeX(); i++) {
			for (int j = 0; j < grassSpace.getSizeY(); j++) {
				if (getGrassAt(i, j) != 0) {
					totalGrass += 1;
				}
			}
		}
	return totalGrass;
	}
	
}
