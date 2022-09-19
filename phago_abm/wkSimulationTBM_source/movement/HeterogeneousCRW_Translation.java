package movement;

import sim.util.Double3D;
import utils.Quaternion;
import core.Cell;
import core.MigratoryCell;
import core.Simulation;

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
public class HeterogeneousCRW_Translation implements Translation
{


	/* Create custom random walk mechanism for a cell. Mean and std of this cell's movememnt dynamics are
	 * themselves drawn from a distribution. */		
	protected double speedMean = 0.0;	// instance distribution parameters. Accessible to subclasses. 
	protected double speedStD = 0.0;
	
	// accessible to others in this package. This was the last speed drawn from the cell's distribution.
	double currentSpeed;

	
	public Double3D move(Quaternion orientation, double speedM_Mean, double speedM_StD, double speedS_Mean, double speedS_StD)
	{			
		speedMean = (Simulation.rng.nextGaussian() * speedM_StD) + speedM_Mean;
		// invert negative values. 
		if (speedMean < 0.0) 	speedMean *= -1.0;
		speedStD = (Simulation.rng.nextGaussian() * speedS_StD) + speedS_Mean;
		if (speedStD < 0.0) speedStD *= -1.0;		// invert negative values
		
		// cell moves along it's x axis. Find its orientation in absolute space, by transforming x-axis. This gives a 
		// unit vector poining in the direction of the cell's orientation. 
		Double3D facing = orientation.transform(MigratoryCell.x_axis);		
		/* apply movement to the cell in the direction that it faces */		
		currentSpeed = (Simulation.rng.nextGaussian() * speedStD) + speedMean;
		// units in um/min. Ensure not faster than maximum possible neutrophil spd.
		currentSpeed = Math.min(25.0, currentSpeed);	 
		double dist = currentSpeed * Simulation.timeSlice;
		// translate would-be backwards movement into forwards. 
		dist = Math.abs(dist);
		// convert unit vector describing cell's orientation in absolute space to a move forward. 
		Double3D move = new Double3D(facing.x * dist, facing.y * dist, facing.z * dist);			
		return move;	
	}


	@Override
	public Quaternion newOrientation(Quaternion orientation, Cell cell) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Double3D move(Quaternion orientation, Cell cell) {

	return null;
	}

}
