package mrCore;

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

public class Macrophage extends MigratoryCell 
{
	public static double diameter = 14.0;		// in microns. 
	
	private CellLogger.Track logger;	
	
	public Macrophage()
	{}
	
	public Macrophage(Schedule sched)
	{
		super(sched);
		if (Simulation.trackCells)
			logger = new CellLogger.Track(this);
	}
	
	@Override
	public void step(SimState state)
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
	
	public CellLogger.Track getLogger()
	{ 	return logger;	}
	
	public void removeCell()
	{
		Simulation.space.cellField.remove(this);
	}
	
	public String getType()
	{	return "Macrophage";	}
}
