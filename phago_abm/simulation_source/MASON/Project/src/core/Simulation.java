package core;

import java.io.File;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import sim.engine.SimState;
import sim.engine.Steppable;
import ec.util.MersenneTwisterFast;
import filesystem.FileSystemIO;

/**
 * This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
    
 * 
 * This is the simulation driver, it is the top level organising entity of a simulation execution. 
 * 
 * @author Mark N. Read
 *
 */
@SuppressWarnings("serial")
public abstract class Simulation extends SimState
{
	// java representation of the XML document holding parameters.
	public static String parametersPath = null;
	public static String outputPath = SimulationBCell.getDefaultOutputDir();

	public static Document parameters = null;	
	
	public static long seed = -1;	
	
	public static MersenneTwisterFast rng = new MersenneTwisterFast();
	public static Compartment3D space;
		
	public static double endTime;		// when to terminate the simulation. 
	public static double timeSlice;  		// duration covered by a stimulation time-step. In minutes.
	// times at which simulation state is sampled and recorded. Should match in vivo work.
	public static double sampleTimeSlice = 0.5; 
	// how many iterations the simulation has been through. Starts at 1 to be consistent with IMARIS. 
	public long timeIter = 1;
	public static boolean position_macs_randomly;
	public static boolean bcell_lognorm;
 
	public static boolean trackCells = true;
	public static boolean trackPositions;

	
	// cells reside outside the imaging volume, and enter it. Hence, simulation can be set up with cells
	// occupying this space (buffer), at similar density to those in the imaging volume (at launch time). This parameter
	// specifies the size of the buffer as a proportion of the imaging volume's size in each dimension. Hence, a value
	// of 0.0 has no buffer; a value of 1.0 has an entire imaging volume's worth of buffer in each dimension.  	
	public static double bufferSize = 0.0;	// must be >= 0.0. 
 				
					// in micrometers.
	public static int tissueRadius = 81;
	public static int tissueWidth = 2*Simulation.tissueRadius;						// in micrometers.	
	public static int tissueHeight = 2*Simulation.tissueRadius;  	// in micrometers.
	public static int tissueDepth = 2*Simulation.tissueRadius;	

	public static double volumeSimulated;		// total volume, including imaging volume and buffer zone. 
	public static double volumeImaged;			// volume of the imaging volume. 
	
	// will only have one simulation running at a time, so this instance can be used by other classes. 
	public static Simulation instance;
	public static int phagoCount;
	public static int numBCells;


	
	// ordering for simulation components added to the schedule. 
	public static final int blastOrdering = 0;			// laser blast happens before everything else. 
	public static final int compartmentOrdering = 1;	// compartments are always stepped before cells.
	public static final int cellOrdering = 2;			// the order for cells to be stepped by the schedule.
	public static final int loggerOrdering = 3;			// logger stepping order, comes after cells.  
	public static final int timeIterOrdering = 10;		// increments the iteration count of simulation time steps. 
	
	public Simulation()
	{
		super(seed);
		instance = this;			
		if(parametersPath == null)
			// means this has not been set by user. 
			parametersPath = getDefaulParametersPath();
		parameters = FileSystemIO.openXMLFile(parametersPath);
	}
	
	public abstract String getDefaulParametersPath();
	public abstract void populateCells();
	public abstract void populateCellsInSphere(boolean position_macs_randomly);
	
	/**
	 * Subclasses can override this if a different compartment is needed. 
	 */
	public Compartment3D initializeCompartment()
	{	return new Compartment3D();	}
	
	/** Sets up the simulation. */
	public void start()
	{
		super.start();		
		Simulation.setupSimulationParameters();
		
		// ensure the experimental directory has been set up. 
		File dir = new File(outputPath);
		if (!dir.exists())
		{
			System.out.println("creating directory : " + outputPath);
			boolean result = dir.mkdirs();			
			if (!result)	System.out.println("ERROR: could not create directory " + outputPath);
		}
		FileSystemIO.copyFile(parametersPath, outputPath + "/parameters.xml");

		// Anonymouse inner class to increment the time iteration count. 
		Steppable tih = new Steppable() {
			@Override
			public void step(SimState state) 
			{	
				((Simulation)state).timeIter ++;
			}
		};
		schedule.scheduleRepeating(schedule.EPOCH, timeIterOrdering, tih, sampleTimeSlice);
		
		// calculate the total number of neutrophils required, based on the size of the buffer zone, and density of 
		// neutrophils in the imaging volume. 
		double Vw = tissueWidth + (2.0 * bufferSize * tissueWidth);
		double Vh = tissueHeight + (2.0 * bufferSize * tissueHeight);
		double Vd = tissueDepth + (2.0 * bufferSize * tissueDepth);
		volumeSimulated = Vw * Vh * Vd;		// total volume, including imaging volume and buffer zone. 
		volumeImaged = tissueWidth * tissueHeight * tissueDepth;	// volume of the imaging volume. 
		
		space = initializeCompartment();		
		System.out.println("3D Compartment initialized with dimensions = " + tissueWidth + " by " + tissueHeight + " by " +  tissueDepth);
		schedule.scheduleRepeating(schedule.EPOCH, Simulation.compartmentOrdering, space, timeSlice);
		
		populateCellsInSphere(position_macs_randomly);
		timeIter += 1;		// must increment this too, else two records for iteration 1 are generated. 
	}
	
    /**
     * Loads the parameters.xml config file and loads the default parameters for all classes in the simulation. Should be the first thing that is done when
     * running the simulation, with GUI or without. Abstract classes must be called before concrete classes. 
     *
     */
    public static void setupSimulationParameters()
    {
    	try
    	{
    		/* read in the default parameters for the various classes in the simulation */
            loadParameters(parameters);
            MigratoryCell.loadParameters(parameters);
            Fragment.loadParameters(parameters);
            Macrophage.loadParameters(parameters);


    	}
    	catch(XPathExpressionException e) 
    	{
			System.out.println("ERROR reading in parameters: " + e.toString());
		}
    }
    
    /**
     * Given the parameters.xml file (represented as a 'Document') this method loads the relevant default values for the top level simulation.
     * @param params
     */
	private static void loadParameters(Document params)
	{
		System.out.println("params = " + params);
		Element pE = (Element) params.getElementsByTagName("Simulation").item(0);			// collect those items under 'Simulation'
									
		tissueWidth = Integer.parseInt(pE.getElementsByTagName("tissueWidth").item(0).getTextContent());
		tissueHeight = tissueWidth;
		tissueDepth = Integer.parseInt(pE.getElementsByTagName("tissueDepth").item(0).getTextContent());
				
		bufferSize = Double.parseDouble(pE.getElementsByTagName("bufferSize").item(0).getTextContent());
		position_macs_randomly = Boolean.parseBoolean(pE.getElementsByTagName("position_macs_randomly").item(0).getTextContent());
		bcell_lognorm = Boolean.parseBoolean(pE.getElementsByTagName("bcell_lognorm").item(0).getTextContent());
		trackPositions = Boolean.parseBoolean(pE.getElementsByTagName("trackPositions").item(0).getTextContent());


		endTime = Double.parseDouble(pE.getElementsByTagName("endTime").item(0).getTextContent());
		timeSlice = Double.parseDouble(pE.getElementsByTagName("timeSlice").item(0).getTextContent());
		
		Element bE = (Element) pE.getElementsByTagName("BCell").item(0);
		numBCells = Integer.parseInt(bE.getElementsByTagName("numBCells").item(0).getTextContent());
		
		sampleTimeSlice = timeSlice;	// default case.
		NodeList nl = pE.getElementsByTagName("sampleTimeSlice");
		if(nl.getLength() > 0)
		{
			sampleTimeSlice = Double.parseDouble(nl.item(0).getTextContent());
		}
	}	
	
	/** Read command line arguments */
	public static void readArgs(String[] args)
	{
		seed = -1;
		int i = 0;		
		while (i < args.length)
		{
			String command = args[i];
			if (command.equals("-e"))		// the end time, as a double. If not supplied here, should be supplied
			{								// with the parameter file. 
				i++;	
				endTime = Double.parseDouble(args[i]);
			}
			else if (command.equals("-p"))	// location of the parameters file. 
			{
				i++;
				parametersPath = args[i];
			}
			else if (command.equals("-o")) 	// where the simulation output files are to be written. 
			{
				i++; 
				outputPath = args[i];
			}
			else if (command.equals("-s"))	// seed
			{
				i++;
				seed = Long.parseLong(args[i]);
			}
			i++;
		}
		if (seed == -1)
			seed = System.currentTimeMillis();	
	}
	
	/** Sets up and then executes the simulation. This is the primary driver loop. */
	public static void execute(Simulation state)
	{
		state.start();
		// this is the main driver loop. 
		do 
		{
			//System.out.println("simulated time = " + state.schedule.getTime());
			if (!state.schedule.step(state))
				break;
			//if  (BCell.getRemovedCount() >= numBCells) 
			//	break;
		} while(state.schedule.getTime() < endTime);
		state.finish();
		
		System.exit(0); 
	}
}
