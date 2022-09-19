package mrPortrayal;

import java.awt.Color;

import sim.display.Console;
import sim.engine.Schedule;
import mrCore.Simulation;
import mrCore.SimulationBCell;
import mrCore.BCell;
import mrCore.Macrophage;
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
public class GUI_BCell extends SimulationGUI
{

	public GUI_BCell()
	{
		super(new SimulationBCell());
	}
	
	public static String getString() {	return "BCells";	}
	
	public void start()
	{
		System.out.println("BCell Simulation - starting");
		super.start();
	}
	
	@Override
	protected void setupPortrayals() 
	{
		Simulation simulation = (Simulation) state;
		
		cellPortrayal.setField(simulation.space.cellField);

		cellPortrayal.setPortrayalForClass(BCell.class, new BCellPortrayal() );
		cellPortrayal.setPortrayalForClass(Macrophage.class, new MacPortrayal() );
		
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
        SimulationGUI sim = new GUI_BCell();
        Console c = new Console(sim);
        c.setVisible(true);	
	}	
}
