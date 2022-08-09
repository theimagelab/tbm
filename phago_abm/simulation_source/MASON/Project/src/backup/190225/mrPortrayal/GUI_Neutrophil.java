package mrPortrayal;

import java.awt.Color;

import sim.display.Console;
import sim.engine.Schedule;
import sim.portrayal3d.continuous.ContinuousPortrayal3D;
import sim.portrayal3d.grid.ValueGridPortrayal3D;
import mrCore.Neutrophil;
import mrCore.Simulation;
import mrCore.SimulationNeutrophil;
import mrLoggers.Snapper;

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
public class GUI_Neutrophil extends SimulationGUI
{	
	public ContinuousPortrayal3D samplePortrayal;
	// These are inefficient cube-based portrayals that can be turned off. 
	public ValueGridPortrayal3D restrictedPortrayal;
	
	
	public GUI_Neutrophil()
	{
		super(new SimulationNeutrophil());		
	}
	
	public static String getString() {	return "NeutroSwarm";	}
	

	public void start()
	{
		System.out.println("NeutroSwarm3D - starting");
		super.start();
	}
	

	public void setupPortrayals()
	{
		Simulation simulation = (Simulation) state;
		
		cellPortrayal.setField(simulation.space.cellField);
		cellPortrayal.setPortrayalForClass(Neutrophil.class, new NeutrophilPortrayal() );		
				
		// redraw the scene. 
		display.setBackdrop(Color.BLACK);		
		display.createSceneGraph();
		display.reset();
		if (imageVolume)
		{
			Snapper snapper = new Snapper(display);
			simulation.schedule.scheduleRepeating(Schedule.EPOCH, 100, snapper, Simulation.timeSlice);
		}		
	}
		
	public static void main(String[] args)
	{
        SimulationGUI sim = new GUI_Neutrophil();
        Console c = new Console(sim);
        c.setVisible(true);	
	}

	@Override
	protected void specificInit() 
	{}
}
