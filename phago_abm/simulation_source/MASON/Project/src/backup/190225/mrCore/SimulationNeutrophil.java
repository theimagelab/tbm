package mrCore;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import mrCore.Compartment3D;
import mrCore.Neutrophil;
import mrCore.Simulation;
import mrLoggers.CellLogger;
import mrLoggers.TimeLogger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
    
 * Entry point to a simulation of neutrophil motility. 
 * 
 * 
 * @author Mark N. Read
 *
 */
public class SimulationNeutrophil extends Simulation 
{
	public static String defaultParametersPath = "parameters-neutrophil.xml";
	
	public static ArrayList<Neutrophil> neutrophils = new ArrayList<Neutrophil>();	
	
	public static CellLogger cellLogger = null;
	
	public static int numNeutrophils = 50;		// number of neutrophils in the imaging volume.
	public static int totalNeutrophils; 		// calculated at launch. Includes neutrophils in the buffer.	
	
	public static double follicleRatio = 0.0;		// looking top down, how much of the skin area comprises hair?
	
	public SimulationNeutrophil()
	{	
		super();
		try{
			SimulationNeutrophil.loadParameters(parameters);
			Neutrophil.loadParameters(parameters);
		}
		catch(XPathExpressionException e) { 
			System.out.println("ERROR reading in parameters: " + e.toString());
		}
    }
	
	/**
	 * Subclasses can override this if a different compartment is needed. 
	 */
	public Compartment3D initializeCompartment()
	{	return new Compartment3D();	}
	
	public String getDefaulParametersPath()
	{	return defaultParametersPath;		}
	
	/**
	 * Populate the simulation's spatial environment with neutrophils.
	 */
	public void populateCells()
	{	
	
		if (trackCells)
		{
			cellLogger = new CellLogger();
		}
		
		totalNeutrophils = (int) Math.round(volumeSimulated/volumeImaged * numNeutrophils);
		System.out.println("total number of N = " + totalNeutrophils );
		
		for(int n = 0; n < totalNeutrophils; n++)
		{
			Neutrophil neutro = new Neutrophil(Simulation.instance.schedule);
			Simulation.space.placeCellRandomly(neutro);
			neutrophils.add(neutro);
		}
		// record neutrophil initial positions; logging only happens after neutrophils have been stepped.
		for (Neutrophil n : neutrophils)
			n.getLogger().step(this);		
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
		n = (Node) xPath.compile("/params/Simulation/Neutrophils/numNeutrophils")
				.evaluate(params, XPathConstants.NODE);
		numNeutrophils = Integer.parseInt(n.getTextContent());
				
		n = (Node) xPath.compile("/params/Simulation/Neutrophils/follicleRatio")
				.evaluate(params, XPathConstants.NODE);		
		follicleRatio = Double.parseDouble(n.getTextContent());
	}
	
	
	public static void main(String[] args)
	{
		readArgs(args);
		Simulation state = new SimulationNeutrophil();
		execute(state);
	}
}
