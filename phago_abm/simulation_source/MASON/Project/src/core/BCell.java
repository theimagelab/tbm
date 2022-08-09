package core;

import java.util.ArrayList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import core.Compartment3D.MoveResults;
import loggers.CellLogger;
import sim.engine.Schedule;
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
    
 * @author Mark N. Read
 *
 */

public class BCell extends MigratoryCell 
{
	public static double diameter;	// in microns. 
	private static double speed;
	Document parameters = Simulation.parameters;	


	private CellLogger.Track logger;
	private static int removedCount=0;	
	private static int cellCount = 0;
	private static double speedM_Mean;
	private static double speedM_StD;
	private static double speedS_Mean;
	private static double speedS_StD;
	private static ArrayList<Double> pitchData = new ArrayList<Double>(8);
	
	public BCell()
	{		try {
		BCell.loadParameters(parameters);
	} catch (XPathExpressionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	}
	
	public BCell(Schedule sched)
	{
		super(sched);
		if (Simulation.trackCells)
			logger = new CellLogger.Track(this);
		BCell.cellCount++;
		

	}
	
	@Override
	public void step(SimState state)
	{
		if (Simulation.space.cellField.exists(this) == true) // check if the cell exists. This is required because if a BCell gets phagocytosed, moveCellCollisionsDetection will try to access it's location and get nullpointerException.
		{
			location = Simulation.space.getCellLocation(this);
			if (bounce.lengthSq() == 0.0)
			{

				orientation = orientationActuator.newOrientation(orientation, pitchData);	
			}
			
			// Detect collision
			bounce();
			Double3D move = null;
			move = translationActuator.move(orientation, speedM_Mean, speedM_StD, speedS_Mean, speedS_StD);
			
			MoveResults mr = Simulation.space.moveCellCollisionDetection(this, move);
			location = mr.newLocation;
			bounce = mr.bounce;			// bounce off other cells that may have been contacted.
			collidedCells = mr.colliders;
			
			int numCollisions = collidedCells.size();
			if (numCollisions > 0) 
			{
				for (int i = 0; i<numCollisions; i++) 
				{
					String colliderCell = ((Cell) collidedCells.get(i)).getType(); // needs the (Cell) cast because collidedCells is an arrayList, not a <Cell> object.
					if (colliderCell == "Macrophage") // is the encountered cell a macrophage?
					{
						removeCell();
						//System.out.println("Removed. B Cell count = " + this.getCount());
						
						// rebirth on phagocytosis to keep cell numbers constant.
						BCell bcell = new BCell(Simulation.instance.schedule);
						Simulation.space.placeCellRandomlyInSphere(bcell, true);
						SimulationBCell.bcells.add(bcell);
					} 
				}
			}
		}
	}
	
	public double getDiameter()
	{ 	return diameter;	}
	
	public double getRadius()
	{	return diameter / 2.0;	}
	
	public Double3D getCurrentLocation()
	{	
		if(location != null)
			return location;
		else
			return Simulation.space.getCellLocation(this);
	}
	
	
	public void removeCell()
	{
		Simulation.space.cellField.remove(this);
		BCell.removedCount++;
	}
	
	public int getCount()
	{	return BCell.cellCount; }
	
	public CellLogger.Track getLogger()
	{ 	return logger;	}
	
	public String getType()
	{	return "BCell";	}
	
	public static void loadParameters(Document params) throws XPathExpressionException
		{
			XPath xPath =  XPathFactory.newInstance().newXPath(); 
			Node n;
			n = (Node) xPath.compile("/params/Simulation/BCell/speedM_Mean")
					.evaluate(params, XPathConstants.NODE);
			speedM_Mean = Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/speedM_StD")
					.evaluate(params, XPathConstants.NODE);
			speedM_StD = Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/speedS_Mean")
					.evaluate(params, XPathConstants.NODE);
			speedS_Mean = Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/speedS_StD")
					.evaluate(params, XPathConstants.NODE);
			speedS_StD = Double.parseDouble(n.getTextContent());
			
			n = (Node) xPath.compile("/params/Simulation/BCell/pitchM_Mean")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(0, Double.parseDouble(n.getTextContent()));
			n = (Node) xPath.compile("/params/Simulation/BCell/pitchM_StD")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(1, Double.parseDouble(n.getTextContent()));
			n = (Node) xPath.compile("/params/Simulation/BCell/pitchS_Mean")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(2, Double.parseDouble(n.getTextContent()));
			n = (Node) xPath.compile("/params/Simulation/BCell/pitchS_StD")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(3, Double.parseDouble(n.getTextContent()));
			
			n = (Node) xPath.compile("/params/Simulation/BCell/rollM_Mean")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(4, Double.parseDouble(n.getTextContent()));
			n = (Node) xPath.compile("/params/Simulation/BCell/rollM_StD")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(5, Double.parseDouble(n.getTextContent()));
			n = (Node) xPath.compile("/params/Simulation/BCell/rollS_Mean")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(6, Double.parseDouble(n.getTextContent()));
			n = (Node) xPath.compile("/params/Simulation/BCell/rollS_StD")
					.evaluate(params, XPathConstants.NODE);
			pitchData.add(7, Double.parseDouble(n.getTextContent()));
			
			n = (Node) xPath.compile("/params/Simulation/BCell/diameter")
					.evaluate(params, XPathConstants.NODE);
			diameter = Double.parseDouble(n.getTextContent());
		}

	@Override
	public int getRemovedCount() {
		return BCell.removedCount;
	}

	@Override
	public double getSpeed() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Double3D getStartLoc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getMeanderChance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Double> getTurnData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setStartLoc(Double3D loc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addMeanderCount() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMeanderCount() {
		return 0;
		// TODO Auto-generated method stub
	}

	@Override
	public ArrayList<Double> getBetaDistrParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Double> getTurnParams() {
		// TODO Auto-generated method stub
		return null;
	}
}