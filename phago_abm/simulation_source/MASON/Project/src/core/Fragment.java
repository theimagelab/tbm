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
 *  * Edited by Wunna: 09 Aug 2022.
 * Introduced a cell called a fragment. This cell type represents an apoptotic b cell fragment.
 * Fragments are able to be phagocytosed, meaning that they disappear on contact with a macrophage.
 * The rate of clearance can be tracked with the parameter removedCount.
 *
 */

public class Fragment extends MigratoryCell 
{
	public static double diameter;	// in microns. 
	private double speed;
	Document parameters = Simulation.parameters;	


	private CellLogger.Track logger;
	private Double3D startLoc;
	private int meanderCount = 0;

	private static int removedCount=0;	
	private static int cellCount = 0;
	
	private static double meanderMeanParam;
	private static double meanderStDParam;
	private static boolean perform_meander;
	private double meanderChance;
	
	private static double speedM_Mean;
	private static double speedM_StD;
	private static double speedS_Mean;
	private static double speedS_StD;
	
	public static TranslationParadigm translationParadigm;
	public static OrientationParadigm orientationParadigm;
	protected static Translation translationActuator;
	protected static Orientation orientationActuator;

	private static ArrayList<Double> turnParam = new ArrayList<Double>(8);
	private ArrayList<Double> turnData = new ArrayList<Double>(8);
	private static ArrayList<Double> betaDistrParams = new ArrayList<Double>(3);

	
	public Fragment()
	{		try {
		Fragment.loadParameters(parameters);
	} catch (XPathExpressionException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}	}
	
	public Fragment(Schedule sched)
	{
		super(sched);
		if (Simulation.trackCells)
			logger = new CellLogger.Track(this);
		Fragment.cellCount++;
		try {
			Fragment.loadParameters(parameters);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Set up motility distribution unique for this cell.
		this.setSpeed(Math.exp((Simulation.rng.nextGaussian() * speedS_Mean) + speedM_Mean)); // convert gaussian to lognormal distribution
		
		// Set up confinement measure unique for this cell.;
		if (perform_meander == true) {
			double meanderChance = 1;
			do {
				meanderChance = Math.exp(Simulation.rng.nextGaussian() * meanderStDParam + meanderMeanParam); //lognorm distr
			} while (meanderChance > 0.99)
	;
			this.setMeanderChance(meanderChance);
		} else {
			this.setMeanderChance(0);
		}
		
		}

	@Override
	public void step(SimState state)
	{
		if (Simulation.space.cellField.exists(this) == true) // check if the cell exists. This is required because if a BCell gets phagocytosed, moveCellCollisionsDetection will try to access it's location and get nullpointerException.
		{
			location = Simulation.space.getCellLocation(this);
			if (bounce.lengthSq() == 0.0)
			{

				//orientation = orientationActuator.newOrientation(orientation, pitchData);
				orientation = orientationActuator.newOrientation(orientation, this);	
			}

			// Detect collision
			bounce();
			Double3D move = null;

			move = translationActuator.move(orientation, this);


			//System.out.println("currspeed" + this.getSpeed() );
			Double3D oldloc = location;
			MoveResults mr = Simulation.space.moveCellCollisionDetection(this, move);
			location = mr.newLocation;
			bounce = mr.bounce;			// bounce off other cells that may have been contacted.
			collidedCells = mr.colliders;
		

			int numCollisions = collidedCells.size();
			if (numCollisions > 0) 
			{
				for (int i = 0; i<numCollisions; i++) 
				{
					String colliderCell = ((Cell) collidedCells.get(i)).getType(); // needs the (Cell) cast because collidedCells is an arrayList, not a <Cell> object.
					if (colliderCell == "Macrophage") // is the encountered cell a macrophage?
					{
						removeCell();
						// rebirth on phagocytosis to keep cell numbers constant.
						Fragment frag = new Fragment(Simulation.instance.schedule);
						Simulation.space.placeCellRandomlyInSphere(frag, true);
						SimulationBCell.fragsln.add(frag);
					} 
				}
			}
			
			if (Simulation.space.cellField.exists(this) == true) { // may have been phagocytosed
				Simulation.space.sphericalBoundary(this, true);
			} 
			
			
			// rare bug: (<1% ) get 'stuck' during a bounce. Reason unsure. In this case, delete and create new.
			if (Simulation.space.cellField.exists(this) == true) { 
				Double3D diff = oldloc.subtract(location);
				if ((diff.x + diff.y + diff.z) == 0.0) {
					Simulation.space.cellField.remove(this);
					Fragment frag = new Fragment(Simulation.instance.schedule);
					Simulation.space.placeCellRandomlyInSphere(frag, true);
					SimulationBCell.fragsln.add(frag);
				}
			}
		}
	}

	
	public static void loadParameters(Document params) throws XPathExpressionException
		{
			XPath xPath =  XPathFactory.newInstance().newXPath(); 
			Node n;
			n = (Node) xPath.compile("/params/Simulation/BCell/speedM_Mean")
					.evaluate(params, XPathConstants.NODE);
			speedM_Mean = Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/speedM_StD")
					.evaluate(params, XPathConstants.NODE);
			speedM_StD = Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/speedS_Mean")
					.evaluate(params, XPathConstants.NODE);
			speedS_Mean = Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/speedS_StD")
					.evaluate(params, XPathConstants.NODE);
			speedS_StD = Double.parseDouble(n.getTextContent());
			
			
			Node pitchM_Mean = (Node) xPath.compile("/params/Simulation/BCell/pitchM_Mean")
					.evaluate(params, XPathConstants.NODE);
			Node pitchM_StD = (Node) xPath.compile("/params/Simulation/BCell/pitchM_StD")
					.evaluate(params, XPathConstants.NODE);
			Node pitchS_Mean = (Node) xPath.compile("/params/Simulation/BCell/pitchS_Mean")
					.evaluate(params, XPathConstants.NODE);
			Node pitchS_StD = (Node) xPath.compile("/params/Simulation/BCell/pitchS_StD")
					.evaluate(params, XPathConstants.NODE);
			Node rollM_Mean = (Node) xPath.compile("/params/Simulation/BCell/rollM_Mean")
					.evaluate(params, XPathConstants.NODE);
			Node rollM_StD = (Node) xPath.compile("/params/Simulation/BCell/rollM_StD")
					.evaluate(params, XPathConstants.NODE);
			Node rollS_Mean = (Node) xPath.compile("/params/Simulation/BCell/rollS_Mean")
					.evaluate(params, XPathConstants.NODE);
			Node rollS_StD = (Node) xPath.compile("/params/Simulation/BCell/rollS_StD")
					.evaluate(params, XPathConstants.NODE);
			
			setTurnParams(Double.parseDouble(pitchM_Mean.getTextContent()),
					Double.parseDouble(pitchM_StD.getTextContent()),
					Double.parseDouble(pitchS_Mean.getTextContent()),
					Double.parseDouble(pitchS_StD.getTextContent()),
					Double.parseDouble(rollM_Mean.getTextContent()),
					Double.parseDouble(rollM_StD.getTextContent()),
					Double.parseDouble(rollS_Mean.getTextContent()),
					Double.parseDouble(rollS_StD.getTextContent())
					);
			
			n = (Node) xPath.compile("/params/Simulation/BCell/perform_meander")
					.evaluate(params, XPathConstants.NODE);
			perform_meander = Boolean.parseBoolean(n.getTextContent()); 
			n = (Node) xPath.compile("/params/Simulation/BCell/meanderStD")
					.evaluate(params, XPathConstants.NODE);
			meanderStDParam =  Double.parseDouble(n.getTextContent());
			n = (Node) xPath.compile("/params/Simulation/BCell/meanderMean")
					.evaluate(params, XPathConstants.NODE);
			meanderMeanParam =  Double.parseDouble(n.getTextContent());
			
			Node alpha = (Node) xPath.compile("/params/Simulation/BCell/betaDistr_alpha")
					.evaluate(params, XPathConstants.NODE);
			Node beta = (Node) xPath.compile("/params/Simulation/BCell/betaDistr_beta")
					.evaluate(params, XPathConstants.NODE);
			Node scale = (Node) xPath.compile("/params/Simulation/BCell/betaDistr_scaleFactor")
					.evaluate(params, XPathConstants.NODE);
			
			setBetaDistrParams(Double.parseDouble(alpha.getTextContent()),
								Double.parseDouble(beta.getTextContent()),
								Double.parseDouble(scale.getTextContent()));
			
			n = (Node) xPath.compile("/params/Simulation/BCell/diameter")
					.evaluate(params, XPathConstants.NODE);
			diameter = Double.parseDouble(n.getTextContent());
			
			n = (Node) xPath.compile("/params/Simulation/BCell/translationParadigm")
					.evaluate(params, XPathConstants.NODE);
			String selection = n.getTextContent();
			n = (Node) xPath.compile("/params/Simulation/BCell/orientationParadigm")
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
		}


	
	private void setMeanderChance(double meanderChance) {
		this.meanderChance = meanderChance;
	}
	
	public double getMeanderChance() {
		return(this.meanderChance);
	}
	
	public void addMeanderCount() {
		this.meanderCount = this.meanderCount + 1;
	}
	
	public int getMeanderCount() {
		return(this.meanderCount);
	}
	
	public void setStartLoc(Double3D location) {
		this.startLoc = location;
	}
	public Double3D getStartLoc() {
		return(this.startLoc);
	}
	
	private void setSpeed(double speed) {
		this.speed = speed;
	}
	public double getSpeed() {
		return(this.speed);
	}
	
	public ArrayList<Double> getBetaDistrParams() {
		return(Fragment.betaDistrParams);
	}
	
	public static void setBetaDistrParams(Double alpha, Double beta, Double scaleFactor) {
		Fragment.betaDistrParams.add(0, alpha);
		Fragment.betaDistrParams.add(1, beta);
		Fragment.betaDistrParams.add(2, scaleFactor);
	}
	
	public ArrayList<Double> getTurnData() {
		return(this.turnData);
	}
	private void setTurnData(double pitchM_Mean, double pitchM_StD, double pitchS_Mean, double pitchS_StD, 
			double rollM_Mean, double rollM_StD, double rollS_Mean, double rollS_StD) {
		//pitch
		this.turnData.add(0, pitchM_Mean);
		this.turnData.add(1, pitchM_Mean);
		this.turnData.add(2, pitchS_Mean);
		this.turnData.add(3, pitchS_StD);
		this.turnData.add(4, rollM_Mean);
		this.turnData.add(5, rollM_StD);
		this.turnData.add(6, rollS_Mean);
		this.turnData.add(7, rollS_StD);
	}
	
	private static void setTurnParams(double pitchM_Mean, double pitchM_StD, double pitchS_Mean, double pitchS_StD, 
			double rollM_Mean, double rollM_StD, double rollS_Mean, double rollS_StD) {
		//pitch
		Fragment.turnParam.add(0, pitchM_Mean);
		Fragment.turnParam.add(1, pitchM_Mean);
		Fragment.turnParam.add(2, pitchS_Mean);
		Fragment.turnParam.add(3, pitchS_StD);
		Fragment.turnParam.add(4, rollM_Mean);
		Fragment.turnParam.add(5, rollM_StD);
		Fragment.turnParam.add(6, rollS_Mean);
		Fragment.turnParam.add(7, rollS_StD);
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
	
	public int getCount()
	{	return Fragment.cellCount; }
	
	public void removeCell()
	{
		Simulation.space.cellField.remove(this);
		Fragment.removedCount++;
		Fragment.cellCount--;
	}
	
	@Override
	public int getRemovedCount() {
		return Fragment.removedCount;
	}
	public CellLogger.Track getLogger()
	{ 	return logger;	}
	
	public String getType()
	{	return "Fragment-LogNorm";	}

	@Override
	public ArrayList<Double> getTurnParams() {
		// TODO Auto-generated method stub
		return null;
	}
}