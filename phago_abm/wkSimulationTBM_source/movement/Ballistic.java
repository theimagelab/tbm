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
    
 * Cells follow a ballistic trajectory. This is used primarily for testing purposes. 
 * 
 * @author Mark N. Read
 *
 */
public class Ballistic implements Orientation, Translation
{
	protected static double speed;
	
	// singleton pattern. 
	public static Ballistic instance = new Ballistic();
	private Ballistic()
	{}
	
	public Quaternion newOrientation(Quaternion orientation)
	{
		return orientation;	
	}
	
	public Double3D move(Quaternion orientation)
	{	
		// cell moves along it's x axis. Find its orientation in absolute space, by transforming x-axis. This gives a 
		// unit vector poining in the direction of the cell's orientation. 
		Double3D facing = orientation.transform(MigratoryCell.x_axis);		
		/* apply movement to the cell in the direction that it faces.  */				
		double dist = Math.min(25.0, speed);	// units in um/min. Ensure not faster than maximum possible neutrophil spd. 
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
		
		n = (Node) xPath.compile("/params/Movement/Ballistic/speed").evaluate(params, XPathConstants.NODE);
		speed = Double.parseDouble(n.getTextContent());
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
