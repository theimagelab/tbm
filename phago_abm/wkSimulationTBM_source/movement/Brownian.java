package movement;

import java.util.ArrayList;
import java.util.List;

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
    
 * 
 * Brownian motion, wherein a partice's speed (and displacement if delta time between time steps is constant) in each
 * dimension is selected from a normal distribution. 
 * 
 * This is implemented as selecting a new orientation in 3D space from a uniform distribution, and a speed from a 
 * zero-mean normal distribution. Hence there is only one parameter. 
 * 
 * @author mark
 *
 */
public class Brownian implements Orientation, Translation
{
	protected static double speedStD;


	// singleton pattern. 
	public static Brownian instance = new Brownian();
	private Brownian()
	{}
	
	public Quaternion newOrientation(Quaternion orientation)
	{
		return Quaternion.randomUniform();
	}
	
	public Double3D move(Quaternion orientation)
	{	
		// cell moves along it's x axis. Find its orientation in absolute space, by transforming x-axis. This gives a 
		// unit vector poining in the direction of the cell's orientation. 
				
		Double3D facing = orientation.transform(MigratoryCell.x_axis);
		double dist = 0;
		
		dist = Simulation.rng.nextGaussian() * speedStD;
		
		/* apply movement to the cell in the direction that it faces.  */		
		dist = Math.min(25.0, dist);	// units in um/min. Ensure not faster than maximum possible neutrophil spd. 
		dist *= Simulation.timeSlice;
		// translate would-be backwards movement into forwards. 
		dist = Math.abs(dist);
		// convert unit vector describing cell's orientation in absolute space to a move forward. 
		Double3D move = new Double3D(facing.x * dist, facing.y * dist, facing.z * dist);			
		return move;		
	}
	
	public static void loadParameters(Document params) throws XPathExpressionException
	{
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;

		n = (Node) xPath.compile("/params/Movement/Brownian/speedStD")
				.evaluate(params, XPathConstants.NODE);
		speedStD = Double.parseDouble(n.getTextContent());
	}

	public Double3D move(Quaternion orientation, double speedM_Mean, double speedM_StD, double speedS_Mean,
			double speedS_StD) {
		// TODO Auto-generated method stub
		return null;
	}

	public Quaternion newOrientation(Quaternion orientation, ArrayList<Double> pitchData) {
		// TODO Auto-generated method stub
		return null;
	}

	
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
