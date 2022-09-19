package mrCore;


import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import mrMovement.Ballistic;
import mrMovement.Brownian;
import mrMovement.HeterogeneousCRW_Orientation;
import mrMovement.HeterogeneousCRW_Translation;
import mrMovement.HomogeneousCRW_Orientation;
import mrMovement.HomogeneousCRW_Translation;
import mrMovement.InverseHeterogeneousCRW;
import mrMovement.InverseHomogeneousCRW;
import mrMovement.LevyFlight;
import mrMovement.Orientation;
import mrMovement.Translation;
import mrUtils.Quaternion;
import mrUtils.Utils;
import sim.engine.Schedule;
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
 * 
 * 
 * Cells that can be moved
 * @author mark
 *
 */
public abstract class MigratoryCell extends Cell
{
	public enum TranslationParadigm 
	{
		BALLISTIC,
		BROWNIAN,
		LEVY_VPL,	// Levy distributions with variable power law decay 
		HOMO_CRW,
		INVERSE_HOMO_CRW,
		HETERO_CRW,
		INDIVIDUALCORRELATED,
		INVERSE_HETERO_CRW	
	}
	
	public enum OrientationParadigm
	{
		BALLISTIC,
		BROWNIAN,
		LEVY_VPL,	// Levy distributions with variable power law decay		
		HOMO_CRW,
		INVERSE_HOMO_CRW,
		HETERO_CRW,
		INDIVIDUALCORRELATED,
		INVERSE_HETERO_CRW		
	}
	public static TranslationParadigm translationParadigm;
	public static OrientationParadigm orientationParadigm;
	
	// These are used by concrete subclasses to provide random walk movements. 
	protected Translation translationActuator;
	protected Orientation orientationActuator;
	
		
	// vector of movement resulting from knocking into other cells. Used to allow cells to slide over one another. 
	protected Double3D bounce;
	// Record of other cells that this cell is currently in contact with. 
	protected ArrayList<Cell> collidedCells = new ArrayList<Cell>();	 
		
	/* `orientation` represents the cell's current orientation in space, compared to where it started from. This 
	 * variable is updated every time the cell changes its orientation. Hence, it provides a conversion between space
	 * relative to the cell, and absolute space. 
	 * 
	 * orientation.transform(x_axis) will give a unit vector in absolute space that points along the x-axis relative to 
	 * the cell. Cells always move along their x_axis, so this is used to update a cells movement. 
	 * 
	 * The inverse can be used to convert coordinates relative to the cell back into absolute space. */
	protected Quaternion orientation = Quaternion.identity();	 	
	public static final Double3D x_axis = new Double3D(1, 0, 0);			
	public static final Double3D y_axis = new Double3D(0, 1, 0);
	public static final Double3D z_axis = new Double3D(0, 0, 1);	
	
	/* standard rotations to locate points around the cell that are frequently used, represented as quaternions. No 
	 * need to destroy and recreate objects, as the values don't change. Hence, static final objects. Remember right 
	 * hand scew. Hence negative and positive values for z-axis of rotation. */
	final static Quaternion rotFront 	= Quaternion.representRotation(0, 1, 0, 0);
	final static Quaternion rotBack 	= Quaternion.representRotation(Math.PI, 0, 0, -1);
	final static Quaternion rotRight 	= Quaternion.representRotation((90 * Math.PI/180), 0, 0, -1);
	final static Quaternion rotLeft 	= Quaternion.representRotation((90 * Math.PI/180), 0, 0, 1);
	final static Quaternion rotUp 		= Quaternion.representRotation((90 * Math.PI/180), 0, -1, 0);
	final static Quaternion rotDown 	= Quaternion.representRotation((90 * Math.PI/180), 0, 1, 0);
	
	public MigratoryCell()
	{}

	public MigratoryCell(Schedule sched)
	{
		super(sched);
		
		bounce = new Double3D(0.0,0.0,0.0);		
		// assign a random orientation.		
		orientation = Quaternion.randomUniform();		
		
		if (translationParadigm == TranslationParadigm.HOMO_CRW)
			translationActuator = HomogeneousCRW_Translation.instance;
		else if (translationParadigm == TranslationParadigm.HETERO_CRW)
			translationActuator = new HeterogeneousCRW_Translation();
		
		if (orientationParadigm == OrientationParadigm.HOMO_CRW)
			orientationActuator = HomogeneousCRW_Orientation.instance;
		else if (orientationParadigm == OrientationParadigm.HETERO_CRW)
			orientationActuator = new HeterogeneousCRW_Orientation();
		else if (orientationParadigm == OrientationParadigm.LEVY_VPL)
		{
			LevyFlight lf = new LevyFlight();
			orientationActuator = lf;
			translationActuator = lf;
		}
		else if (orientationParadigm == OrientationParadigm.INVERSE_HETERO_CRW)
		{
			InverseHeterogeneousCRW icto = new InverseHeterogeneousCRW();
			orientationActuator = icto;
			translationActuator = icto;
		}
		else if (orientationParadigm == OrientationParadigm.INVERSE_HOMO_CRW)
		{			
			orientationActuator = InverseHomogeneousCRW.instance;
			translationActuator = InverseHomogeneousCRW.instance;
		}
		else if (orientationParadigm == OrientationParadigm.BROWNIAN)
		{			
			orientationActuator = Brownian.instance;
			translationActuator = Brownian.instance;
		}
		else if (orientationParadigm == OrientationParadigm.BALLISTIC)
		{
			orientationActuator = Ballistic.instance;
			translationActuator = Ballistic.instance;
		}
	}
	
	/** Apply some rotation to the cell's current orientation in response to it having collided.
	 */
	protected void bounce()
	{	// TODO this is not currently rotation-rate limited. Should it be?
		/* apply some rotation to the cell's current orientation in response to it having collided. Cells only
		 * move in the direction they are facing, so for cells to slide around obstacles, their orientation
		 * must be altered. The bounce represents the normal of the collision(s) that the cell had. A new
		 * orientation is selected that is part way between the desired headin, and the bounce heading. 
		 * 
		 * Note on application of quaternions. The calculation of the rotation is performed in absolute space (not
		 * relative to the cell). This is because the bounce vector is expressed in absolute space. Hence, the cell's 
		 * heading must be converted to absolute space, the angle between the two is calculated, and the rotation 
		 * is applied around the normal of the two vectors. */
		if (bounce.lengthSq() != 0.0)		
		{		 				
			Double3D facing = orientation.transform(x_axis);
			// the normal is perpendicular to the plane on which bounce and facing vectors lie. 
			Double3D normal = Utils.crossProduct(facing, bounce);		
			// this calculation takes vector direction into account. Ie, because facing points into the object that			
			// bounce points out of, 90 <= angle <= 180 (eqivalen in radians)
			double angle = Utils.angleBetweenVectors(facing, bounce);	// in radians.
			// want to slide along the object, so subtract a little less than 90 degrees. 90 exactly still results in
			// some collision, and cells grind along their obstacles more slowly. This is a balance between the two. 
			angle -= Math.PI * 0.4;			 			
			// rotation applied to orientation to represent the cell bouncing off a collision.
			Quaternion bounceRot = Quaternion.representRotation(angle, normal.x, normal.y, normal.z);			
			/* apply the rotation. Note the reverse order of mutliplication. Mutliply orientation by the bounce
			 * rotation. This is because the bounce rotation is calculated in absolute space, rather than relative to 
			 * the cell. The bounce resulting from collision is given in absolute space, and the cell's heading
			 * in absolute space must be calculated before the rotation can be derived.  */ 
			orientation = bounceRot.multiply(orientation);	 
		} 		
	}	
	
	public double[] eulersAngles()
	{	return orientation.toEulerAngles();		}
	
	public static void loadParameters(Document params) throws XPathExpressionException
	{	
		XPath xPath =  XPathFactory.newInstance().newXPath(); 
		Node n;
		/* set up translation paradigm */
		n = (Node) xPath.compile("/params/Movement/translationParadigm")
				.evaluate(params, XPathConstants.NODE);
		String selection = n.getTextContent();
		if (selection.equals("HomogeneousCRW"))
		{
			translationParadigm = TranslationParadigm.HOMO_CRW;
			HomogeneousCRW_Translation.loadParameters(params);
		}
		else if (selection.equals("HeterogeneousCRW"))
		{
			translationParadigm = TranslationParadigm.HETERO_CRW;
			HeterogeneousCRW_Translation.loadParameters(params);
		}
		else if (selection.equals("LevyFlight"))
		{
			translationParadigm = TranslationParadigm.LEVY_VPL;
			LevyFlight.loadParameters(params);			
		}
		else if (selection.equals("InverseHeterogeneousCRW"))
		{ /* handled below */ }
		else if (selection.equals("InverseHomogeneousCRW"))
		{ /* handled below */ }
		else if (selection.equals("Brownian"))
		{ /* handled below */ }
		else if (selection.equals("Ballistic"))
		{ /* handled below */ }
		else throw new RuntimeException("Must select an appropriate translation paradigm.");
		
		/* set up orientation paradigm */
		n = (Node) xPath.compile("/params/Movement/orientationParadigm")
				.evaluate(params, XPathConstants.NODE);
		selection = n.getTextContent();
		if (selection.equals("HomogeneousCRW"))
		{
			orientationParadigm = OrientationParadigm.HOMO_CRW;
			HomogeneousCRW_Orientation.loadParameters(params);
		}
		else if (selection.equals("HeterogeneousCRW"))
		{
			orientationParadigm = OrientationParadigm.HETERO_CRW;
			HeterogeneousCRW_Orientation.loadParameters(params);
		}
		else if (selection.equals("LevyFlight"))
		{
			orientationParadigm = OrientationParadigm.LEVY_VPL;
			LevyFlight.loadParameters(params);
		}
		else if (selection.equals("InverseHeterogeneousCRW"))
		{
			translationParadigm = TranslationParadigm.INVERSE_HETERO_CRW;
			orientationParadigm = OrientationParadigm.INVERSE_HETERO_CRW;			
			InverseHeterogeneousCRW.loadParameters(params);
		}
		else if (selection.equals("InverseHomogeneousCRW"))
		{
			translationParadigm = TranslationParadigm.INVERSE_HOMO_CRW;
			orientationParadigm = OrientationParadigm.INVERSE_HOMO_CRW;
			InverseHomogeneousCRW.loadParameters(params);
		}
		else if (selection.equals("Brownian"))
		{
			translationParadigm = TranslationParadigm.BROWNIAN;
			orientationParadigm = OrientationParadigm.BROWNIAN;
			Brownian.loadParameters(params);
		}
		else if (selection.equals("Ballistic"))
		{
			translationParadigm = TranslationParadigm.BALLISTIC;
			orientationParadigm = OrientationParadigm.BALLISTIC;
			Ballistic.loadParameters(params);			
		}
		else throw new RuntimeException("Must select an appropriate orientation paradigm.");
	}
}
