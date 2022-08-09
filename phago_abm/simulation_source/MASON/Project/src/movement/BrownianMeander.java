package movement;

import java.util.ArrayList;

import core.Cell;
import core.MigratoryCell;
import core.Simulation;
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
    
 *  * Edited by Wunna: 04 Aug 2022.
 * This is a Brownian motion where each cell has it's own bespoke mean speed.
 * The orientation of the cell is drawn from a uniform distribution and speeds are drawn from a distribution (lognormal) that matches
 * the imaging data.
 * To reflect the meandering index of the fragments, each cell is given a small chance of meandering directly away or towards the track start position.
 * This activity is called a meander, if it moves away from the track start, or a confine, if it moves towards.
 * We tune the meander probability to reflect the meandering index observed in vivo.
 * Hence this is a brownian motion with a tunable meandering behaviour.
 * 
 * Original description by Mark Read can be found in Brownian.java
 * 
 *
 */
public class BrownianMeander implements Orientation, Translation
{
	protected static double speedStD;


	// singleton pattern. 
	public static BrownianMeander instance = new BrownianMeander();
	private BrownianMeander()
	{}
	
	public Quaternion newOrientation(Quaternion orientation, Cell cell)
	{
		return Quaternion.randomUniform();
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
		}
		
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

