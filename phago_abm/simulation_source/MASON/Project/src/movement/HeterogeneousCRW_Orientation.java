 package movement;

import java.util.ArrayList;

import core.Cell;
import core.MigratoryCell;
import core.Simulation;
import sim.util.Double3D;
import utils.Quaternion;

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
public class HeterogeneousCRW_Orientation implements Orientation
{
	protected double pitchRateMean;
	protected double pitchRateStD;
	protected double rollRateMean;
	protected double rollRateStD;
	
	protected static double pitchM_Mean;
	protected static double pitchM_StD;
	protected static double pitchS_Mean;
	protected static double pitchS_StD;

	protected static double rollM_Mean;
	protected static double rollM_StD;
	protected static double rollS_Mean;
	protected static double rollS_StD;
	
	public HeterogeneousCRW_Orientation()
	{

	}

	@Override
	public Quaternion newOrientation(Quaternion orientation, ArrayList<Double> pitchData) 
	{
		/* Note the order that quaternion multiplication is done here - the rotations are expressed relative to the
		 * cell, hence orientation is multiplied by the rotation quaternion.  */
		// roll the cell along it's x-axis (axis in which it faces). 
		// This rolls the cell, but doesn't change it's heading or pitch. 
		
		double pitchM_Mean = pitchData.get(0);
		double pitchM_StD = pitchData.get(1);
		double pitchS_Mean = pitchData.get(2);
		double pitchS_StD = pitchData.get(3);

		double rollM_Mean= pitchData.get(4);
		double rollM_StD= pitchData.get(5);
		double rollS_Mean= pitchData.get(6);
		double rollS_StD= pitchData.get(7);
		
		pitchRateMean = (Simulation.rng.nextGaussian() * pitchM_StD) + pitchM_Mean;
		pitchRateMean = Math.abs(pitchRateMean);	// invert negative values.
		pitchRateStD = (Simulation.rng.nextGaussian() * pitchS_StD) + pitchS_Mean;
		pitchRateStD = Math.abs(pitchRateStD);
		
		rollRateMean = (Simulation.rng.nextGaussian() * rollM_StD) + rollM_Mean;
		rollRateMean = Math.abs(rollRateMean);	// invert negative values.
		rollRateStD = (Simulation.rng.nextGaussian() * rollS_StD) + rollS_Mean;
		rollRateStD = Math.abs(rollRateStD);	
		double roll;
		if (rollM_Mean < 0.0)
			// if mean roll rate has been set to a negative value, assume this indicates a uniform distribution
			// should be used. 
			roll = Simulation.rng.nextDouble() * 2.0 * Math.PI;
		else{
			roll = (Simulation.rng.nextGaussian() * rollRateStD) + rollRateMean;
			if (Simulation.rng.nextBoolean())
				roll *= -1.0;		// cells can roll in either direction.
			roll *= Simulation.timeSlice;
		}
		// roll as a quaternion.
		Quaternion rotateQ = Quaternion.representRotation
				(roll, MigratoryCell.x_axis.x, MigratoryCell.x_axis.y, MigratoryCell.x_axis.z); 
		// multiply orientation by rotateQ, because rotateQ is calculated relative to cell, not in absolute space. 
		orientation = orientation.multiply(rotateQ).normalise();	// alter the cell's orientation. 
		
		// change cell pitch (roll along the y axis). Pitch can be changed in both positive and negative directions.
		double pitch = (Simulation.rng.nextGaussian() * pitchRateStD) + pitchRateMean;
		// randomly invert values. This makes no difference to zero-mean distributions, and avoids a bias in 
		// non-zero-mean distributions. Hence, applicable to both. 
		if (Simulation.rng.nextBoolean())
			pitch *= -1.0;
		pitch *= Simulation.timeSlice;		// account for timestep.
		Quaternion pitchQ = Quaternion.representRotation
				(pitch, MigratoryCell.y_axis.x, MigratoryCell.y_axis.y, MigratoryCell.y_axis.z);
		// multiply orientation by rotateQ, because pitchQ is calculated relative to cell, not in absolute space.
		orientation = orientation.multiply(pitchQ).normalise();
		return orientation;
	}
	
	public Double3D move(Quaternion orientation, ArrayList<Double> pitchData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Quaternion newOrientation(Quaternion orientation, Cell cell) {
		/* Note the order that quaternion multiplication is done here - the rotations are expressed relative to the
		 * cell, hence orientation is multiplied by the rotation quaternion.  */
		// roll the cell along it's x-axis (axis in which it faces). 
		// This rolls the cell, but doesn't change it's heading or pitch. 
		 ArrayList<Double> turnParams = cell.getTurnParams();
		double pitchM_Mean = turnParams.get(0);
		double pitchM_StD = turnParams.get(1);
		double pitchS_Mean = turnParams.get(2);
		double pitchS_StD = turnParams.get(3);

		double rollM_Mean= turnParams.get(4);
		double rollM_StD= turnParams.get(5);
		double rollS_Mean= turnParams.get(6);
		double rollS_StD= turnParams.get(7);
		
		pitchRateMean = (Simulation.rng.nextGaussian() * pitchM_StD) + pitchM_Mean;
		pitchRateMean = Math.abs(pitchRateMean);	// invert negative values.
		pitchRateStD = (Simulation.rng.nextGaussian() * pitchS_StD) + pitchS_Mean;
		pitchRateStD = Math.abs(pitchRateStD);
		
		rollRateMean = (Simulation.rng.nextGaussian() * rollM_StD) + rollM_Mean;
		rollRateMean = Math.abs(rollRateMean);	// invert negative values.
		rollRateStD = (Simulation.rng.nextGaussian() * rollS_StD) + rollS_Mean;
		rollRateStD = Math.abs(rollRateStD);	
		double roll;
		if (rollM_Mean < 0.0)
			// if mean roll rate has been set to a negative value, assume this indicates a uniform distribution
			// should be used. 
			roll = Simulation.rng.nextDouble() * 2.0 * Math.PI;
		else{
			roll = (Simulation.rng.nextGaussian() * rollRateStD) + rollRateMean;
			if (Simulation.rng.nextBoolean())
				roll *= -1.0;		// cells can roll in either direction.
			roll *= Simulation.timeSlice;
		}
		// roll as a quaternion.
		Quaternion rotateQ = Quaternion.representRotation
				(roll, MigratoryCell.x_axis.x, MigratoryCell.x_axis.y, MigratoryCell.x_axis.z); 
		// multiply orientation by rotateQ, because rotateQ is calculated relative to cell, not in absolute space. 
		orientation = orientation.multiply(rotateQ).normalise();	// alter the cell's orientation. 
		
		// change cell pitch (roll along the y axis). Pitch can be changed in both positive and negative directions.
		double pitch = (Simulation.rng.nextGaussian() * pitchRateStD) + pitchRateMean;
		// randomly invert values. This makes no difference to zero-mean distributions, and avoids a bias in 
		// non-zero-mean distributions. Hence, applicable to both. 
		if (Simulation.rng.nextBoolean())
			pitch *= -1.0;
		pitch *= Simulation.timeSlice;		// account for timestep.
		Quaternion pitchQ = Quaternion.representRotation
				(pitch, MigratoryCell.y_axis.x, MigratoryCell.y_axis.y, MigratoryCell.y_axis.z);
		// multiply orientation by rotateQ, because pitchQ is calculated relative to cell, not in absolute space.
		orientation = orientation.multiply(pitchQ).normalise();
		return orientation;
	}

}
