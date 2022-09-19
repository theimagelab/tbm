package mrCore;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import mrCore.Compartment3D.MoveResults;
import mrLoggers.CellLogger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

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
 * @author mark
 *
 */

@SuppressWarnings("serial")
public class Neutrophil extends MigratoryCell 
{	
	public static double diameter = 7.0;  // in micrometers. 
		
	private CellLogger.Track logger;
	 
	/* Don't use this, only here to make RenderableNeutrophil work. */
	public Neutrophil()
	{}
	
	public Neutrophil(Schedule sched)
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
	

	/* Used for debugging, has cell move at a constant speed. */
	private Double3D calculateMoveConstant()
	{
		Double3D facing = orientation.transform(x_axis);
		double dist = 5.0;
		return new Double3D(facing.x*dist, facing.y*dist, facing.z*dist);
	}


	public double getDiameter()
	{ 	return diameter;	}
	
	public double getRadius()
	{	return diameter / 2.0;	}
	
	public CellLogger.Track getLogger()
	{ 	return logger;	}
		
	
	
	public static void loadParameters(Document params) throws XPathExpressionException
	{
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;
		n = (Node) xPath.compile("/params/Neutrophil/diameter")
				.evaluate(params, XPathConstants.NODE);
		diameter = Double.parseDouble(n.getTextContent());
	}

	public String getType()
	{	return "neutrophil";	}

	@Override
}
