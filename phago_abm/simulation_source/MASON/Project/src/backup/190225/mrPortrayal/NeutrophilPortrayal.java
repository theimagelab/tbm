package mrPortrayal;

import java.awt.Color;

import javax.media.j3d.TransformGroup;

import mrCore.Neutrophil;
import mrCore.Simulation;
import sim.portrayal3d.simple.SpherePortrayal3D;
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
    
 * @author Mark N. Read
 *
 */
public class NeutrophilPortrayal extends SpherePortrayal3D
{
	// naive = green. 
	private static Color neutrophilColor = new Color(0.0f, 0.8f, 0.0f, 1.0f);
	
	public NeutrophilPortrayal()
	{
		super(neutrophilColor, Neutrophil.diameter);
	}
	

	/**
	 * Overridden to provide a custom portrayal for neutrophils. 
	 */
	public TransformGroup getModel(Object obj, TransformGroup j3dModel)
	{
		Neutrophil neutrophil = (Neutrophil)obj;

		return colorByState(neutrophil, j3dModel);
	}
		
	private TransformGroup colorByState(Neutrophil neutrophil, TransformGroup j3dModel)
	{

		setAppearance(j3dModel, appearanceForColors(
				neutrophilColor, 		// ambient color
				null, 					// emissive color (black)
				neutrophilColor, 		// diffuse color
				null, 					// specular color (white)
				1.0f, 					// shininess, none. 
				1.0f));					// opacity		
		if (SimulationGUI.imageOutsideVolume == false && outsideImagingVolume(neutrophil))
		{
			setAppearance(j3dModel, appearanceForColors(
					neutrophilColor, 			// ambient color
					null, 						// emissive color (black)
					neutrophilColor, 			// diffuse color
					null, 						// specular color (white)
					1.0f, 						// shininess, none. 
					0.0f));						// opacity					
		}
		
		return super.getModel(neutrophil, j3dModel);		
	}
	
	private boolean outsideImagingVolume(Neutrophil n)
	{
		Double3D loc = n.getCurrentLocation();
			
		if (loc.x < 0.0 || loc.x > Simulation.tissueWidth)
			return true;
		if (loc.y < 0.0 || loc.y > Simulation.tissueHeight)
			return true;
		if (loc.z < 0.0 || loc.z > Simulation.tissueDepth)
			return true;
		return false;
	}
	
}
