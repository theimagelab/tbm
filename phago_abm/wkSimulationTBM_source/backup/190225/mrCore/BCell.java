package mrCore;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import mrCore.Compartment3D.MoveResults;
import mrLoggers.CellLogger;
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
	public static double diameter = 7.0;	// in microns. 
	public static double speed = 0;
	
	private CellLogger.Track logger;	
	
	public BCell()
	{}
	
	public BCell(Schedule sched)
	{
		super(sched);
		if (Simulation.trackCells)
			logger = new CellLogger.Track(this);
	}
	
	@Override
	public void step(SimState state)
	{
		if (Simulation.space.cellField.exists(this) == true) // check if the cell exists. This is required because if a BCell gets phagocytosed, moveCellCollisionsDetection will try to access it's location and get nullpointerException.
		{
			location = Simulation.space.getCellLocation(this);
			if (bounce.lengthSq() == 0.0)
			{
				// no change in neutrophil state; perform random walk.
				orientation = orientationActuator.newOrientation(orientation);	
			}
			
			bounce();
			Double3D move = null;
			move = translationActuator.move(orientation);
			
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
						Simulation.space.cellField.remove(this); // phagocytosed
						System.out.println("removed");
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
	}
	
	public CellLogger.Track getLogger()
	{ 	return logger;	}
	
	public String getType()
	{	return "BCell";	}
}
