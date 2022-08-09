package movement;

import java.util.ArrayList;
import org.apache.commons.math3.distribution.BetaDistribution;

import core.Cell;
import core.Fragment;
import core.MigratoryCell;
import core.Simulation;
import core.SimulationBCell;
import sim.util.Double3D;
import utils.Quaternion;

/**
 * 
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
    
    @author Mark N. Read and Wunna Kyaw
    
 *  * Edited by Wunna: 04 Aug 2022.
 * This is a random walk where each cell has it's bespoke motility characteristics.
 * The turn speeds of each cell is drawn from a beta distribution, and speeds are drawn from a log normal distribution. These distributions
 * were the most appropriate for the observed imaging data.
 * To reflect the meandering index of the fragments, each cell is given a small chance of meandering directly away or towards the track start position.
 * This activity is called a meander, if it moves away from the track start, or a confine, if it moves towards.
 * 
 * There is functionality to tune the meander probability to reflect the meandering index observed in vivo.
 * Hence this is a brownian motion with a tunable meandering behaviour.
 * 
 * Original description by Mark Read can be found in Brownian.java
 * 
 *
 */
public class HeterogeneousBetaMeander implements Orientation, Translation
{
	protected static double speedStD;


	// singleton pattern. 
	public static HeterogeneousBetaMeander instance = new HeterogeneousBetaMeander();
	private HeterogeneousBetaMeander()
	{}
	
	@Override
	public Quaternion newOrientation(Quaternion orientation, Cell cell) 
	{
		/* Note the order that quaternion multiplication is done here - the rotations are expressed relative to the
		 * cell, hence orientation is multiplied by the rotation quaternion.  */
		// roll the cell along it's x-axis (axis in which it faces). 
		// This rolls the cell, but doesn't change it's heading or pitch. 
		
		double shapeA = cell.getBetaDistrParams().get(0);
		double shapeB = cell.getBetaDistrParams().get(1);
		double scaleFactor = cell.getBetaDistrParams().get(2);

		BetaDistribution betaDist = new BetaDistribution(shapeA, shapeB);

		double rollRate = betaDist.sample() * scaleFactor;
	
		double roll;
		if (rollRate < 0.0)
			// if mean roll rate has been set to a negative value, assume this indicates a uniform distribution
			// should be used. 
			roll = Simulation.rng.nextDouble() * 2.0 * Math.PI;
		else{
			roll = rollRate;
			if (Simulation.rng.nextBoolean())
				roll *= -1.0;		// cells can roll in either direction.
			roll *= Simulation.timeSlice;
		}
		// roll as a quaternion.
		Quaternion rotateQ = Quaternion.representRotation
				(roll, MigratoryCell.x_axis.x, MigratoryCell.x_axis.y, MigratoryCell.x_axis.z); 
		// multiply orientation by rotateQ, because rotateQ is calculated relative to cell, not in absolute space. 
		orientation = orientation.multiply(rotateQ).normalise();	// alter the cell's orientation. 
		
		double pitchRate = betaDist.sample() * scaleFactor;
		// change cell pitch (roll along the y axis). Pitch can be changed in both positive and negative directions.
		double pitch = pitchRate;
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
	
	private static Double3D getStartDirection(Cell cell)
	{
		Double3D startLoc = cell.getStartLoc();
		Double3D startDirection = startLoc.subtract( cell.getCurrentLocation() );

		return startDirection; 
	}
	
	public Double3D move(Quaternion orientation, Cell cell)
	{	
		// cell moves along it's x axis. Find its orientation in absolute space, by transforming x-axis. This gives a 
		// unit vector poining in the direction of the cell's orientation. 
				
		Double3D facing = orientation.transform(MigratoryCell.x_axis);
		
		double dist = cell.getSpeed() * Simulation.timeSlice;
		// translate would-be backwards movement into forwards. 
		dist = Math.abs(dist);

		// convert unit vector describing cell's orientation in absolute space to a move forward. 
		Double3D move = new Double3D(facing.x * dist, facing.y * dist, facing.z * dist);		

		
		double meanderChance = cell.getMeanderChance();
		int mPolarity = -1; // moving away from start.
		if (meanderChance < 0) {
			//mPolarity = 1; // indicates confinement (moving towards start)
		} else if (meanderChance > 0) {
			if (Simulation.rng.nextFloat() < Math.abs(meanderChance)) {
				Double3D startDirection = getStartDirection(cell);
				
				if (!Double.isNaN(startDirection.getX()) && (startDirection.getX() + startDirection.getY() + startDirection.getZ())!=0) {
					Double3D mDir = new Double3D(startDirection.x * mPolarity, startDirection.y * mPolarity, startDirection.z * mPolarity);

						double meanderDirNorm = Math.sqrt(mDir.x*mDir.x+ mDir.y*mDir.y + mDir.z*mDir.z);
						
						// Vector of displacement for meandering, for this timestep.
						double Mx = dist * mDir.x / meanderDirNorm;
						double My = dist * mDir.y / meanderDirNorm;
						double Mz = dist * mDir.z / meanderDirNorm;
						
						if (!Double.isNaN(Mx) && Mx != 0.0) {
							move = new Double3D(Mx, My, Mz);
							cell.addMeanderCount();
						}
					} else if (Double.isNaN(startDirection.getX()) && cell.getType() == "BCellLogNorm") {
						
						Simulation.space.cellField.remove(cell);
						Fragment frag = new Fragment(Simulation.instance.schedule);
						Simulation.space.placeCellRandomlyInSphere(frag, true);
						SimulationBCell.fragsln.add(frag);
					}
				}
			}
		
		return move;	
	}

	public Double3D move(Quaternion orientation, double speedM_Mean, double speedM_StD, double speedS_Mean,
			double speedS_StD) {
		double speedMean = (Simulation.rng.nextGaussian() * speedM_StD) + speedM_Mean;
		// invert negative values. 
		if (speedMean < 0.0) 	speedMean *= -1.0;
		speedStD = (Simulation.rng.nextGaussian() * speedS_StD) + speedS_Mean;
		if (speedStD < 0.0) speedStD *= -1.0;		// invert negative values
		
		// cell moves along it's x axis. Find its orientation in absolute space, by transforming x-axis. This gives a 
		// unit vector poining in the direction of the cell's orientation. 
		Double3D facing = orientation.transform(MigratoryCell.x_axis);		
		/* apply movement to the cell in the direction that it faces */		
		double currentSpeed = (Simulation.rng.nextGaussian() * speedStD) + speedMean;
		// units in um/min. Ensure not faster than maximum possible neutrophil spd.
		currentSpeed = Math.min(25.0, currentSpeed);	 
		double dist = currentSpeed * Simulation.timeSlice;
		// translate would-be backwards movement into forwards. 
		dist = Math.abs(dist);
		// convert unit vector describing cell's orientation in absolute space to a move forward. 
		Double3D move = new Double3D(facing.x * dist, facing.y * dist, facing.z * dist);			
		return move;
	}

	public Quaternion newOrientation(Quaternion orientation, ArrayList<Double> pitchData) {
		// TODO Auto-generated method stub
		return null;
	}

	
	public Double3D move(Quaternion orientation, ArrayList<Double> pitchData) {
		// TODO Auto-generated method stub
		return null;
	}
}



