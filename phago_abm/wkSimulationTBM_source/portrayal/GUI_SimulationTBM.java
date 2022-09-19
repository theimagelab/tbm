package portrayal;

import java.awt.Color;

import core.Fragment;
import core.Macrophage;
import core.Simulation;
import core.SimulationTBM;
import loggers.Snapper;
import sim.display.Console;
import sim.engine.Schedule;

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
public class GUI_SimulationTBM extends SimulationGUI
{

	public GUI_SimulationTBM()
	{
		super(new SimulationTBM());
	}
	
	public static String getString() {	return "Fragments";	}
	
	public void start()
	{
		super.start();
	}
	
	@Override
	protected void setupPortrayals() 
	{
		Simulation simulation = (Simulation) state;
		
		cellPortrayal.setField(simulation.space.cellField);

		cellPortrayal.setPortrayalForClass(Fragment.class, new FragmentPortrayal() );
		cellPortrayal.setPortrayalForClass(Macrophage.class, new MacrophagePortrayal() );
		
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
	
	protected void specificInit()
	{}
	
	public static void main(String[] args)
	{
        SimulationGUI sim = new GUI_SimulationTBM();
        Console c = new Console(sim);
        c.setVisible(true);	
	}	
}
