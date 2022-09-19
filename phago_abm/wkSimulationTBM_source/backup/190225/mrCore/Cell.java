package mrCore;

import mrCore.Simulation;
import sim.engine.Schedule;
import sim.engine.Steppable;
import sim.util.Double3D;

/**
 *  This program is free software: you can redistribute it and/or modify
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
public abstract class Cell implements Steppable
{
	// the location of the center of the hair follicle. The Z coordinate should be ignored, as the follicle occupies
	// a vertical cylinder along the z-axis. 
	protected Double3D location;
	
	private int id;
	
	private static int id_counter = 0;
	
	public Cell(){
		this.id = id_counter;
		id_counter++;

	}
	public Cell(Schedule sched)
	{
		this.id = id_counter;
		id_counter++;
		// start time at which the new neutrophil will be scheduled.
		double startTime = sched.getTime();
		if (sched.getTime() < Schedule.EPOCH)	// time is -1.0 when simulation has not been started yet. 
			startTime = Schedule.EPOCH;		
		sched.scheduleRepeating(startTime, Simulation.cellOrdering, this, Simulation.timeSlice);
	}

	public abstract double getDiameter();
	public abstract double getRadius();
	
	public Double3D getCurrentLocation()
	{	
		if(location != null)
			return location;
		else
			return Simulation.space.getCellLocation(this);
	}
	
	public int getID()
	{
		return this.id;
	}
	
	public abstract void removeCell();
	
	public abstract String getType();
}
