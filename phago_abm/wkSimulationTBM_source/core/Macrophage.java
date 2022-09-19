package core;

import java.util.ArrayList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import core.Compartment3D.MoveResults;
import loggers.CellLogger;
import movement.Brownian;
import movement.BrownianMeander;
import movement.HeterogeneousBetaMeander;
import movement.HeterogeneousCRW_Orientation;
import movement.HeterogeneousCRW_Translation;
import movement.Orientation;
import movement.Translation;
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
    
 * @author Mark N. Read and Wunna Kyaw
 *
 */

@SuppressWarnings("serial")
public class Macrophage extends MigratoryCell 
{
	
	private static double speedM_Mean;
	private static double speedM_StD;
	private static double speedS_Mean;
	private static double speedS_StD;
	private static ArrayList<Double> turnParams = new ArrayList<Double>(8);
	Document parameters = Simulation.parameters;	
	
	public static TranslationParadigm translationParadigm;
	public static OrientationParadigm orientationParadigm;
	protected static Translation translationActuator;
	protected static Orientation orientationActuator;

	public static double diameter;		// in microns. 
	public static String cellType = "Macrophage";
	
	private CellLogger.Track logger;

	private static int cellCount = 0;

	
	public Macrophage() {
		try {
			Macrophage.loadParameters(parameters);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Macrophage(Schedule sched)
	{
		super(sched);
		if (Simulation.trackCells)
			logger = new CellLogger.Track(this);
		Macrophage.cellCount++;
		try {
			Macrophage.loadParameters(parameters);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("null")
	@Override
	// TBM's are motile. If they collide with another TBM, they keep going (no contact repulsion)
	public void step(SimState state) 
	{
		location = Simulation.space.getCellLocation(this);
			// no change in state perform random walk.

		orientation = orientationActuator.newOrientation(orientation, this);
		bounce();
		Double3D move = null;
		move = translationActuator.move(orientation, speedM_Mean, speedM_StD, speedS_Mean, speedS_StD);

		MoveResults mr = Simulation.space.moveCellCollisionDetection(this, move);
		location = mr.newLocation;
		bounce = mr.bounce;			// bounce off other cells that may have been contacted.
		collidedCells = mr.colliders;

		Simulation.space.sphericalBoundary(this, false);
	}
	

	
	public static void loadParameters(Document params) throws XPathExpressionException
	{
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;
		n = (Node) xPath.compile("/params/Simulation/Macrophage/speedM_Mean")
				.evaluate(params, XPathConstants.NODE);
		speedM_Mean = Double.parseDouble(n.getTextContent());
		n = (Node) xPath.compile("/params/Simulation/Macrophage/speedM_StD")
				.evaluate(params, XPathConstants.NODE);
		speedM_StD = Double.parseDouble(n.getTextContent());
		n = (Node) xPath.compile("/params/Simulation/Macrophage/speedS_Mean")
				.evaluate(params, XPathConstants.NODE);
		speedS_Mean = Double.parseDouble(n.getTextContent());
		n = (Node) xPath.compile("/params/Simulation/Macrophage/speedS_StD")
				.evaluate(params, XPathConstants.NODE);
		speedS_StD = Double.parseDouble(n.getTextContent());
		n = (Node) xPath.compile("/params/Simulation/Macrophage/speedM_Mean")
				.evaluate(params, XPathConstants.NODE);
		
		n = (Node) xPath.compile("/params/Simulation/Macrophage/pitchM_Mean")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(0, Double.parseDouble(n.getTextContent()));
		n = (Node) xPath.compile("/params/Simulation/Macrophage/pitchM_StD")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(1, Double.parseDouble(n.getTextContent()));
		n = (Node) xPath.compile("/params/Simulation/Macrophage/pitchS_Mean")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(2, Double.parseDouble(n.getTextContent()));
		n = (Node) xPath.compile("/params/Simulation/Macrophage/pitchS_StD")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(3, Double.parseDouble(n.getTextContent()));
		
		n = (Node) xPath.compile("/params/Simulation/Macrophage/rollM_Mean")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(4, Double.parseDouble(n.getTextContent()));
		n = (Node) xPath.compile("/params/Simulation/Macrophage/rollM_StD")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(5, Double.parseDouble(n.getTextContent()));
		n = (Node) xPath.compile("/params/Simulation/Macrophage/rollS_Mean")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(6, Double.parseDouble(n.getTextContent()));
		n = (Node) xPath.compile("/params/Simulation/Macrophage/rollS_StD")
				.evaluate(params, XPathConstants.NODE);
		turnParams.add(7, Double.parseDouble(n.getTextContent()));
		
		n = (Node) xPath.compile("/params/Simulation/Macrophage/diameter")
				.evaluate(params, XPathConstants.NODE);
		diameter = Double.parseDouble(n.getTextContent());
		
		/* set up orientation paradigm */
		n = (Node) xPath.compile("/params/Simulation/Macrophage/translationParadigm")
				.evaluate(params, XPathConstants.NODE);
		String selection = n.getTextContent();
		n = (Node) xPath.compile("/params/Simulation/Macrophage/orientationParadigm")
				.evaluate(params, XPathConstants.NODE);
		selection = n.getTextContent();
		if (selection.equals("Brownian"))
		{
			translationParadigm = TranslationParadigm.BROWNIAN;
			orientationParadigm = OrientationParadigm.BROWNIAN;
			orientationActuator = Brownian.instance;
			translationActuator = Brownian.instance;	
			Brownian.loadParameters(params);
		}
		else if (selection.equals("BrownianMeander"))
		{
			translationParadigm = TranslationParadigm.BROWNIAN_MEANDER;
			orientationParadigm = OrientationParadigm.BROWNIAN_MEANDER;
			orientationActuator = BrownianMeander.instance;
			translationActuator = BrownianMeander.instance;	
		}
		else if (selection.equals("HeterogeneousBetaMeander"))
		{
			translationParadigm = TranslationParadigm.HETEROGENOUS_BETA_MEANDER;
			orientationParadigm = OrientationParadigm.HETEROGENOUS_BETA_MEANDER;
			orientationActuator = HeterogeneousBetaMeander.instance;
			translationActuator = HeterogeneousBetaMeander.instance;
		}
		else if (selection.equals("HeterogeneousCRW"))
		{
			translationParadigm = TranslationParadigm.HETERO_CRW;
			orientationParadigm = OrientationParadigm.HETERO_CRW;
			translationActuator = new HeterogeneousCRW_Translation();
			orientationActuator = new HeterogeneousCRW_Orientation();
		}
	}

	@Override
	public int getRemovedCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getSpeed() {
		return Macrophage.speedM_Mean;
	}

	@Override
	public Double3D getStartLoc() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getMeanderChance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Double> getTurnParams() {
		return turnParams;
	}

	@Override
	public void setStartLoc(Double3D loc) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addMeanderCount() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getMeanderCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ArrayList<Double> getBetaDistrParams() {
		// TODO Auto-generated method stub
		return null;
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

	public int getCount() {
		return Macrophage.cellCount;
	}

	@Override
	public ArrayList<Double> getTurnData() {
		// TODO Auto-generated method stub
		return null;
	}
}
