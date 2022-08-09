package movement;

import java.util.ArrayList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import core.Cell;
import core.MigratoryCell;
import core.Simulation;
import sim.util.Double3D;
import utils.LevyDistribution;
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
 * 
 * Implements a Levy Flight random walk. Cells pick a random orientation, and then move in that direction by some
 * distance, determined from a long-tailed distribution where the long tail points to positive infinity.   
 */
public class LevyFlight implements Orientation, Translation
{	
	
	private static double restMu = Double.NaN;
	private static double restScale = Double.NaN;
	
	private static double motileMu = Double.NaN; 
	private static double motileScale = Double.NaN;
	
	private static double speedMu = Double.NaN;
	private static double speedScale = Double.NaN;
	
	private enum State
	{
		REST,
		MOTILE
	}
	private State state = State.REST;

	
	private Quaternion newOrientation;
	private double currentSpeed;
	
	private double endMotileTime = -1.0;	// absolute simulation time at which cell will start moving again. 
	private double endRestTime = -1.0;    // absolute simulation time at which cell will pause.
	
	public LevyFlight()
	{
		plan();
	}

	private void plan()
	{	
		// execute this twice so that cases where no rest (or motility, though that would be daft) are handled.
		// Otherwise there will be a time step where nothing happens, even if the rest/motile duration is zero.
		for (int i = 0; i < 2; i++)
		{
			if (this.state == State.REST)
			{
				if (Simulation.instance.schedule.getTime() >= endRestTime)
				{
					// timer has elapsed, start moving again.
					newOrientation = Quaternion.randomUniform();
					currentSpeed = LevyDistribution.sample_positive(speedMu, speedScale);				
					double duration = LevyDistribution.sample_positive(motileMu, motileScale);
					endMotileTime = Simulation.instance.schedule.getTime() + duration;
					this.state = State.MOTILE;
				}
			} 
			if (this.state == State.MOTILE) 
			{
				// cell is moving.
				if (Simulation.instance.schedule.getTime() >= endMotileTime)
				{	
					// time to stop moving. 				
					currentSpeed = 0.0;
					double duration = 0.0;
					if (restMu > 0.0)	// if rest time is being used. 
						duration = LevyDistribution.sample_positive(restMu, restScale);					
					endRestTime = Simulation.instance.schedule.getTime() + duration;
					this.state=State.REST;
				}
			}
		}
	}
	
	public Quaternion newOrientation(Quaternion orientation) 
	{
		plan();
		return newOrientation;		
	}

	public Double3D move(Quaternion orientation) 
	{
		// Levy flight draws step size from a long tailed distribution. In this case log-normal.		
		double length = currentSpeed * Simulation.timeSlice;
		// cell moves along it's x axis. Find its orientation in absolute space, by transforming x-axis. This gives a 
		// unit vector poining in the direction of the cell's orientation.
		Double3D facing = orientation.transform(MigratoryCell.x_axis);
		// convert unit vector describing cell's orientation in absolute space to a move forward.
		Double3D move = new Double3D(facing.x*length, facing.y*length, facing.z*length);
		return move;
	}

	
	public static void loadParameters(Document params) throws XPathExpressionException
	{
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;		
		
		n = (Node) xPath.compile("/params/Movement/LevyFlight/restMu").evaluate(params, XPathConstants.NODE);
		restMu = Double.parseDouble(n.getTextContent());
		n = (Node) xPath.compile("/params/Movement/LevyFlight/restScale").evaluate(params, XPathConstants.NODE);
		restScale = Double.parseDouble(n.getTextContent());
		
		n = (Node) xPath.compile("/params/Movement/LevyFlight/motileMu").evaluate(params, XPathConstants.NODE);
		motileMu = Double.parseDouble(n.getTextContent());
		n = (Node) xPath.compile("/params/Movement/LevyFlight/motileScale").evaluate(params, XPathConstants.NODE);
		motileScale = Double.parseDouble(n.getTextContent());
		
		n = (Node) xPath.compile("/params/Movement/LevyFlight/speedMu").evaluate(params, XPathConstants.NODE);
		speedMu = Double.parseDouble(n.getTextContent());	
		n = (Node) xPath.compile("/params/Movement/LevyFlight/speedScale").evaluate(params, XPathConstants.NODE);
		speedScale = Double.parseDouble(n.getTextContent());	
	}

	@Override
	public Double3D move(Quaternion orientation, double speedM_Mean, double speedM_StD, double speedS_Mean,
			double speedS_StD) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Quaternion newOrientation(Quaternion orientation, ArrayList<Double> pitchData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double3D move(Quaternion orientation, ArrayList<Double> pitchData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Quaternion newOrientation(Quaternion orientation, Cell cell) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double3D move(Quaternion orientation, Cell cell) {
		// TODO Auto-generated method stub
		return null;
	}
}	

