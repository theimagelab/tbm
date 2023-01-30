package core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import filesystem.FileSystemIO;
import loggers.CellLogger;
import loggers.TimeLogger;
import sim.engine.SimState;
import sim.util.Double3D;

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
 * @author Mark N. Read and Wunna Kyaw
*/
public class SimulationTBM extends Simulation
{
	public static String defaultParametersPath = "calibration-parameters.xml";
	
	
	public static final String defaultOutputDir = "results/"
			+ "/";


	final static File initMacDir = new File("macInitPos_meanNNdist");
	final static File randomMacDir = new File("macInitPos_random");
	
	
	File[] macFiles = initMacDir.listFiles();
	File[] macFiles_random = randomMacDir.listFiles();

	Random rand = new Random();
	File initMacPositionFile = macFiles[rand.nextInt(macFiles.length)]; 
	File randMacPositionFile = macFiles_random[rand.nextInt(macFiles_random.length)]; 

	
	public static ArrayList<Fragment> fragsln = new ArrayList<Fragment>();	

	public static ArrayList<Macrophage> macs = new ArrayList<Macrophage>();	

	public static CellLogger cellLogger = null; 
	public CellLogger.CellType fragCountLogger;
	public CellLogger.CellType macCountLogger; 
	
	public static int numFrags = 100;	//number of Frags in the imaging volume.
	public static int numMacs = 18; // number of macs in the imaging volume.
	public static int totalFrags; 			// calculated at launch. Includes cells outside the imaging volume, which is much larger than the imaging volume and can be approximated to be boundless.
	public static int totalMacs;
	

	public SimulationTBM()
	{	
		super();		
		try{
			SimulationTBM.loadParameters(parameters);			
		}
		
		catch(XPathExpressionException e) { 
			System.out.println("ERROR reading in parameters: " + e.toString());
		}
	}
	
	public String getDefaulParametersPath()
	{	return defaultParametersPath;		}
	
	
	/**
	 * Populate the simulation's spatial environment with cells.
	 */
	public void populateCellsInSphere(boolean position_macs_randomly )
	{		
		
		if (trackCells)
			cellLogger = new CellLogger();
			fragCountLogger = new CellLogger.CellType(new Fragment());
			macCountLogger = new CellLogger.CellType(new Macrophage());
		
		totalFrags = numFrags;
		totalMacs = numMacs;

		double totalCellVol = (4/3) * Math.PI * Math.pow((Fragment.diameter/2), 3);
		if ( totalCellVol/volumeSimulated > 0.4) {
			System.out.println("Total cell volume is " + totalCellVol + "while simulated box size is" + volumeSimulated + ". Program may get stuck as there may not be enough 3D space to populate cells. Consider reducing number of cells");
		}
		
		System.out.println("Populating " + totalFrags + " fragments.");
		for(int n = 0; n < totalFrags; n++)
			{
				Fragment frag = new Fragment(Simulation.instance.schedule);
				Simulation.space.placeCellRandomlyInSphere(frag, true);
				fragsln.add(frag);		

			}

		if (!position_macs_randomly) {
			List<List<String>> positionData = FileSystemIO.readImarisCSV(initMacPositionFile, 1);
				
				// random init are enclosed in a sphere at origin 0,0,0. But our simulation's sphere is centered at (r,r,r) (i..e no negative coords)
				// So we need to translate.
			Double3D newCenter = new Double3D(Simulation.tissueRadius, Simulation.tissueRadius,Simulation.tissueRadius);
			List<Double3D> recenteredPos = FileSystemIO.translatePositionData(positionData, newCenter);
			System.out.println("Populating " + recenteredPos.size() + " macrophages from." + initMacPositionFile);

			for (int n = 0; n < recenteredPos.size(); n++) 
				{
				Macrophage mac = new Macrophage(Simulation.instance.schedule);
				Double3D loc = recenteredPos.get(n);
				Simulation.space.cellField.setObjectLocation(mac, loc);
				macs.add(mac);
			}

		} else if (position_macs_randomly == true) {
			System.out.println("Populating " + numMacs + " macrophages randomly.");
			List<List<String>> positionData = FileSystemIO.readImarisCSV(randMacPositionFile, 1);
				
				// these are generated already pre-centered at location (r,r,r) no need to translate.
			for (int n = 0; n < positionData.size(); n++) 
					{
				Macrophage mac = new Macrophage(Simulation.instance.schedule);
				Double x = Double.valueOf(positionData.get(n).get(0));
				Double y =  Double.valueOf(positionData.get(n).get(1));
				Double z =  Double.valueOf(positionData.get(n).get(2));
				Double3D loc = new Double3D (x, y, z);
				Simulation.space.cellField.setObjectLocation(mac, loc);
				macs.add(mac);
			}
				
		}
		
		for (Fragment c : fragsln)
			c.getLogger().step(this);	

		System.out.println("total number of fragments = " + fragsln.size() );

		System.out.println("total number of Macs = " + macs.size() );
		
	}
	
	/** Tears down the simulation, can be used for writing IO. */ 
	public void finish()
	{
		super.finish();
		if (trackCells)
		{
			System.out.println("Writing simulation output data to filesystem: " + outputPath);
			if (Simulation.trackPositions == true) {
				cellLogger.writeTrackData(outputPath, "_Position.csv");			
			}
			TimeLogger.writeTimeData(outputPath);
			cellLogger.writeCountData(outputPath);		
			cellLogger.writeRemovedCountData(outputPath);			

		}
		System.out.println("Simulation completed, you may close any open windows now.");
	}

	
	public static void loadParameters(Document params) throws XPathExpressionException
	{
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;
		n = (Node) xPath.compile("/params/Simulation/Fragment/numFrags")
				.evaluate(params, XPathConstants.NODE);
		numFrags = Integer.parseInt(n.getTextContent());
		n = (Node) xPath.compile("/params/Simulation/Macrophage/numMacs")
				.evaluate(params, XPathConstants.NODE);
		numMacs = Integer.parseInt(n.getTextContent());
	}
	
	
	public static void main(String[] args)
	{
		readArgs(args);
		Simulation state = new SimulationTBM();
		execute(state);
	}

	public static String getDefaultOutputDir() {
		return defaultOutputDir;
	}

}
