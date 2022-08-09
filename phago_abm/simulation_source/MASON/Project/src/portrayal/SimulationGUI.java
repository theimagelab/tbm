package portrayal;

import javax.swing.JFrame;

import core.Simulation;
import core.SimulationBCell;
import sim.display.Controller;
import sim.display.GUIState;
import sim.display3d.Display3D;
import sim.engine.SimState;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.portrayal3d.grid.ValueGridPortrayal3D;
import sim.portrayal3d.simple.AxesPortrayal3D;
import sim.portrayal3d.simple.WireFrameBoxPortrayal3D;

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
public abstract class SimulationGUI extends GUIState
{
	// Set to true, this will take pictures of the tissue volume every timestep and save them. 
	public static boolean imageVolume = false;
	
	// set to true to prevent items leaving the imaging volume being displayed. 
	public static boolean imageOutsideVolume = false;

		
	public Display3D display;
	public JFrame displayFrame;
	
	public ContinuousPortrayal3D cellPortrayal = new ContinuousPortrayal3D();
	public ValueGridPortrayal3D visitedPortrayal = new ValueGridPortrayal3D("isVisited");

	
	public WireFrameBoxPortrayal3D frame;
	public AxesPortrayal3D axes;
	
	public SimulationGUI()
	{ 		
		super(new SimulationBCell());
	}
	
	public SimulationGUI(SimState state)
	{	super(state); 	

	}
			
	public void start()
	{		
		super.start();
		setupPortrayals();
	}
	
	protected abstract void setupPortrayals();
	protected abstract void specificInit();
	
	public void init(Controller c)
	{
		System.out.println("GCPhagoCoverage3D - initialising");
		super.init(c);
		
		display = new Display3D(1000, 1000, this);
		frame = new WireFrameBoxPortrayal3D(
				0.0, 0.0, 0.0, 				// one corner, at the origin. Second corner at the opposite extreme.
				Simulation.tissueWidth, Simulation.tissueHeight, Simulation.tissueDepth);
		axes = new AxesPortrayal3D(1.0, true);
		display.attach( frame, "frame" );
		display.attach( axes, "axes" );
		
		// changes the camera location and focal length. 
        display.translate(-Simulation.tissueWidth / 2.0, 
        				  -Simulation.tissueHeight / 2.0, 
        				  -Simulation.tissueDepth / 2.0);
        display.scale(1.3 / Simulation.tissueWidth);
        display.rotateX(180); 	// flip scene around so camera points at skin. 
                
		displayFrame = display.createFrame();
		displayFrame.setTitle("Tissue Display");
		c.registerFrame(displayFrame);
		displayFrame.setVisible(true);
		
		display.attach( cellPortrayal, "cells");
		display.attach( visitedPortrayal, "visited");
		specificInit();		// concrete classes perform their initialisations. 		
	}
		
	public void quit()
	{
		super.quit();
		if (displayFrame != null) displayFrame.dispose();
		displayFrame = null;
		display = null;
	}	
}
