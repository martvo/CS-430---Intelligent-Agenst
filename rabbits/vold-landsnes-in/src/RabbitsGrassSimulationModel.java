import java.awt.Color;
import java.util.ArrayList;

import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.reflector.RangePropertyDescriptor;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;



/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {
	
	// Default Values
	private static final int NUMAGENTS = 10;
	private static final int WORLDXSIZE = 20;
	private static final int WORLDYSIZE = 20;
	private static final int TOTALGRASS = 100;
	private static final int AGENT_MIN_LIFESPAN = 30;
	private static final int AGENT_MAX_LIFESPAN = 60;
	private static final int AGENT_START_ENERGY = 50;
	private static final int ENERGY_TO_REPRODUCE = 70;
	private static final int COST_OF_REPRODUCTION = 30;
	private static final int ENERGY_IN_GRASS = 10;
	private static final int GRASS_GROWTH_RATE = 10;
	private static final int COST_OF_MOVEMENT = 1;
	
	private int numAgents = NUMAGENTS;
	private int worldXSize = WORLDXSIZE;
	private int worldYSize = WORLDYSIZE;
	private int grass = TOTALGRASS;
	private int agentMinLifespan = AGENT_MIN_LIFESPAN;
	private int agentMaxLifespan = AGENT_MAX_LIFESPAN;
	private int agentStartEnergy = AGENT_START_ENERGY;
	private int energyToReproduce = ENERGY_TO_REPRODUCE;
	private int costOfReproduction = COST_OF_REPRODUCTION;
	private int energyInGrass = ENERGY_IN_GRASS;
	private int grassGrowthRate = GRASS_GROWTH_RATE;
	private int costOfMovment = COST_OF_MOVEMENT;
	
	private Schedule schedule;
	
	private RabbitsGrassSimulationSpace rgSpace;
	
	private ArrayList<RabbitsGrassSimulationAgent> agentList;
	
	private DisplaySurface displaySurf;
	private OpenSequenceGraph amountOfRabbitsAndGrassInSpace;
	
	
	class rabbitsInSpace implements DataSource, Sequence {
		
		public Object execute() {
			return new Double(getSValue());
		}
		
		public double getSValue() {
			return countLivingAgents();
		}
	}
	
	
	class grassInSpace implements DataSource, Sequence {

		public Object execute() {
			return new Double(getSValue());
		}
		
		public double getSValue() {
			return rgSpace.getTotalGrass();
		}
	}

	
	public static void main(String[] args) {	
		System.out.println("Rabbit skeleton");
		
		SimInit init = new SimInit();
		RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
	    init.loadModel(model, "", false);
	}
	
	
	@SuppressWarnings("unchecked")
	public void setup() {
		System.out.println("Running setup");
		rgSpace = null;
		agentList = new ArrayList<RabbitsGrassSimulationAgent>();
		schedule = new Schedule(1);
		
		//Tear down Displays
		if (displaySurf != null){
			displaySurf.dispose();
		}
		displaySurf = null;
		
		if (amountOfRabbitsAndGrassInSpace != null) {
			amountOfRabbitsAndGrassInSpace.dispose();
		}
		amountOfRabbitsAndGrassInSpace = null;
		
		
		// Create Displays
		displaySurf = new DisplaySurface(this, "Rabbits Grass Model Window 1");
		amountOfRabbitsAndGrassInSpace = new OpenSequenceGraph("Amount of Rabbits and Grass in Space", this);

		// Register Displays
		registerDisplaySurface("Rabbits Grass Model Window 1", displaySurf);
		this.registerMediaProducer("Plot", amountOfRabbitsAndGrassInSpace);
		
		// Create sliders
		RangePropertyDescriptor numbeOfRabbits = new RangePropertyDescriptor("NumAgents", 10, 100, 20);
		descriptors.put("NumAgents", numbeOfRabbits);
		
		RangePropertyDescriptor tresholdForBirth = new RangePropertyDescriptor("CostOfReproduction", 10, 80, 20);
		descriptors.put("CostOfReproduction", tresholdForBirth);
		
		RangePropertyDescriptor rateGrassGrows = new RangePropertyDescriptor("GrassGrowthRate", 10, 100, 20);
		descriptors.put("GrassGrowthRate", rateGrassGrows);
		
		RangePropertyDescriptor gridSizeX = new RangePropertyDescriptor("WorldXSize", 10, 100, 20);
		descriptors.put("WorldXSize", gridSizeX);
		
		RangePropertyDescriptor gridSizeY = new RangePropertyDescriptor("WorldYSize", 10, 100, 20);
		descriptors.put("WorldYSize", gridSizeY);
		
	}
	

	public void begin() {
		buildModel();
		buildSchedule();
		buildDisplay();
		
		displaySurf.display();
		amountOfRabbitsAndGrassInSpace.display();
	}
	
		
	public void buildModel() {
		System.out.println("Running BuildModel");
		rgSpace = new RabbitsGrassSimulationSpace(worldXSize, worldYSize, energyInGrass);
		rgSpace.spreadGrass(grass);
		
		for(int i = 0; i < numAgents; i++){
			addNewAgent();
		}
		
		for(int i = 0; i < agentList.size(); i++){
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			rga.report();
		}
	}
	
	
	private void addNewAgent() {
		RabbitsGrassSimulationAgent a = new RabbitsGrassSimulationAgent(agentMinLifespan, agentMaxLifespan, agentStartEnergy, energyToReproduce, 
				costOfReproduction, costOfMovment);
	    agentList.add(a);
	    rgSpace.addAgent(a);
	}
	

	public void buildSchedule() {
		System.out.println("Running BuildSchedule");
		
		class RabbitGrassStep extends BasicAction {
			public void execute() {
				SimUtilities.shuffle(agentList);
		        for (int i =0; i < agentList.size(); i++) {
		        	RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
		        	boolean reproduces = rga.step();
		        	
		        	if (reproduces) {
		        		addNewAgent();
		        	}
		        }
		        
		        regrowGrass();
		        reapDeadAgents();
		        displaySurf.updateDisplay();
			}
		}
		schedule.scheduleActionBeginning(0, new RabbitGrassStep());
		
		class RabbitGrassCountLiving extends BasicAction {
		      public void execute() {
		    	  countLivingAgents();
		      }
		}
		schedule.scheduleActionAtInterval(10, new RabbitGrassCountLiving());
		
		class RabbitGrassUpdateGrassInSpace extends BasicAction {
			public void execute() {
				amountOfRabbitsAndGrassInSpace.step();
			}
		}
		schedule.scheduleActionAtInterval(1, new RabbitGrassUpdateGrassInSpace());
	}
	
	
	public void buildDisplay() {
		System.out.println("Running BuildDisplay");
		
		ColorMap map = new ColorMap();
		
	    map.mapColor(energyInGrass, Color.green);
	    map.mapColor(0, Color.black);

	    Value2DDisplay displayGrass = new Value2DDisplay(rgSpace.getCurrentGrassSpace(), map);
	    
	    Object2DDisplay displayAgents = new Object2DDisplay(rgSpace.getCurrentAgentSpace());
	    displayAgents.setObjectList(agentList);

	    displaySurf.addDisplayableProbeable(displayGrass, "Grass");
	    displaySurf.addDisplayableProbeable(displayAgents, "Agents");
	    
	    amountOfRabbitsAndGrassInSpace.addSequence("Rabbits in Space", new rabbitsInSpace());
	    amountOfRabbitsAndGrassInSpace.addSequence("Grass in Space", new grassInSpace());
	}
	

	public String[] getInitParam() {
		// TODO Auto-generated method stub
		String[] initParams = { "NumAgents", "WorldXSize", "WorldYSize", "CostOfReproduction", "GrassGrowthRate" };
	    return initParams;
	}
	
	
	private void regrowGrass() {
		rgSpace.spreadGrass(grassGrowthRate);
	}
	
	
	private void reapDeadAgents(){
		for (int i = (agentList.size() - 1); i >= 0 ; i--) {
			RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
			if(rga.getStepsToLive() < 1  || rga.getEnergy() <= 0){
				rgSpace.removeAgentAt(rga.getX(), rga.getY());
				agentList.remove(i);
			}
		}
	}

	
	private int countLivingAgents(){
	    int livingAgents = 0;
	    for (int i = 0; i < agentList.size(); i++) {
	    	RabbitsGrassSimulationAgent rga = (RabbitsGrassSimulationAgent)agentList.get(i);
	    	if (rga.getStepsToLive() > 0) livingAgents++;
	    }
	    System.out.println("Number of living agents is: " + livingAgents);

	    return livingAgents;
	}
	

	public String getName() {
		// TODO Auto-generated method stub
		return "Rabbits and Grass Simulation";
	}
	

	public Schedule getSchedule() {
		// TODO Auto-generated method stub
		return this.schedule;
	}
	
	
	public int getWorldXSize() {
		return this.worldXSize;
	}
	

	public void setWorldXSize(int worldXSize) {
	    this.worldXSize = worldXSize;
	}
	

	public int getWorldYSize() {
		return this.worldYSize;
	}
	

	public void setWorldYSize(int worldYSize) {
	    this.worldYSize = worldYSize;
	}
	
	
	public int getNumAgents() {
	    return this.numAgents;
	}
	

	public void setNumAgents(int numAgents) {
		this.numAgents = numAgents;
	}
	
	
	public int getGrass() {
	    return this.grass;
	}
	

	public void setGrass(int grass) {
		this.grass = grass;
	}
	
	
	public int getAgentMaxLifespan() {
	    return this.agentMaxLifespan;
	}

	
	public int getAgentMinLifespan() {
	    return this.agentMinLifespan;
	}

	
	public void setAgentMaxLifespan(int agentMaxLifespan) {
	    this.agentMaxLifespan = agentMaxLifespan;
	}

	
	public void setAgentMinLifespan(int agentMinLifespan) {
	    this.agentMinLifespan = agentMinLifespan;
	}
	
	
	public int getEnergyToReproduce() {
		return this.energyToReproduce;
	}

	
	public void getEnergyToReproduce(int energyToReproduce) {
		this.energyToReproduce = energyToReproduce;
	}
	
	
	public int getCostOfReproduction() {
		return this.costOfReproduction;
	}

	
	public void setCostOfReproduction(int costOfReproduction) {
		this.costOfReproduction = costOfReproduction;
	}
	
	
	public int getEnergyInGrass() {
		return this.energyInGrass;
	}
	
	
	public void setEnergyInGrass(int energyInGrass) {
		this.energyInGrass = energyInGrass;
	}


	public int getGrassGrowthRate() {
		return this.grassGrowthRate;
	}


	public void setGrassGrowthRate(int grassGrowthRate) {
		this.grassGrowthRate = grassGrowthRate;
	}


	public int getCostOfMovment() {
		return costOfMovment;
	}


	public void setCostOfMovment(int costOfMovment) {
		this.costOfMovment = costOfMovment;
	}
	
}
