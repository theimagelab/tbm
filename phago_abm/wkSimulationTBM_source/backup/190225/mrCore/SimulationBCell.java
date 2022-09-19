package mrCore;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import mrLoggers.CellLogger;
import mrLoggers.TimeLogger;

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
 * @author Mark N. Read
 *
 */
public class SimulationBCell extends Simulation
{
	public static String defaultParametersPath = "parameters-bcell.xml";
	
	public static ArrayList<BCell> bcells = new ArrayList<BCell>();	
	public static ArrayList<Macrophage> macs = new ArrayList<Macrophage>();	


	public static CellLogger cellLogger = null;
	
	public static int numBCells = 100;	//number of bcells in the imaging volume.
	public static int numMacs = 20; // number of macs in the imaging volume.
	public static int totalBCells; 			// calculated at launch. Includes cells outside the imaging volume, which is much larger than the imaging volume and can be approximated to be boundless.
	public static int totalMacs; 			

	public SimulationBCell()
	{	
		super();		
		try{
			SimulationBCell.loadParameters(parameters);			
		}
		catch(XPathExpressionException e) { 
			System.out.println("ERROR reading in parameters: " + e.toString());
		}
	}
	
	public String getDefaulParametersPath()
	{	return defaultParametersPath;		}
	
	/**
	 * Populate the simulation's spatial environment with T cells.
	 */
	public void populateCells()
	{		
		if (trackCells)
			cellLogger = new CellLogger();
		
		totalBCells = (int) Math.round(volumeSimulated/volumeImaged * numBCells);
		totalMacs = (int) Math.round(volumeSimulated/volumeImaged * numMacs);
		System.out.println("total number of B cells = " + totalBCells );
		
		for(int n = 0; n < totalBCells; n++)
		{
			BCell bcell = new BCell(Simulation.instance.schedule);
			Simulation.space.placeCellRandomly(bcell);
			bcells.add(bcell);
		}
		
		for(int n = 0; n < totalMacs; n++)
		{
			Macrophage mac = new Macrophage(Simulation.instance.schedule);
			Simulation.space.placeCellRandomly(mac);
			macs.add(mac);
		}
		
		// record neutrophil initial positions; logging only happens after neutrophils have been stepped.
		for (BCell c : bcells)
			c.getLogger().step(this);		
	}
	
	/** Tears down the simulation, can be used for writing IO. */ 
	public void finish()
	{
		super.finish();
		if (trackCells)
		{
			System.out.println("Writing simulation output data to filesystem: " + outputPath);
			cellLogger.writeTrackData(outputPath);			
			TimeLogger.writeTimeData(outputPath);
		}
		System.out.println("Simulation completed, you may close any open windows now.");
	}

	
	public static void loadParameters(Document params) throws XPathExpressionException
	{
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;
		n = (Node) xPath.compile("/params/Simulation/BCells/numBCells")
				.evaluate(params, XPathConstants.NODE);
		numBCells = Integer.parseInt(n.getTextContent());
	}
	
	
	public static void main(String[] args)
	{
		readArgs(args);
		Simulation state = new SimulationBCell();
		execute(state);
	}
}
