package loggers;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import core.Simulation;
import sim.display3d.Display3D;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.media.PNGEncoder;


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
 * This will create a series of png snapshots of the tissue volume in the experiment directory, under "movie". 
 * 
 * images are named "img_xxx.png"
 * 
 * to create a movie of this, use ffmpeg (a bash utility). Run this command from the "movie" directory.  
 * 
 * $> ffmpeg -framerate 5 -i img_%03d.png -c:v libx264 -r 30 -pix_fmt yuv420p out.mp4
 * 
 * 
 * @author markread
 *
 */

public class Snapper implements Steppable 
{
	private Display3D display;
	private String movieDir;
	private int sequenceNum = 0;		// used to label the snapshots taken of imaging volume sequentially. 
	public Snapper(Display3D disp)
	{
		this.display = disp;
		movieDir = Simulation.outputPath + "/stills";
		File dir = new File(movieDir);
		if (!dir.exists())
		{
			System.out.println("creating directory : " + movieDir);
			boolean result = dir.mkdirs();			
			if (!result)	System.out.println("ERROR: could not create directory " + movieDir);
		}		
	}
	
	
	@Override
	public void step(SimState state) 
	{
		/* a complete hack, and an annoying one. There seems to be come synchronisation issues between the Display3D's
		 * rendering of the tissue space, and the simulation itself. This sleep is purely to allow some catch up time.
		 * Without it, some of the cells in the simulation don't move between subsequent png files generated, it looks
		 * like some cells are being moved whilst the image is still being captured. I can't find a clean way to 
		 * fix this, and have spent enough time trying. Hence, sleep for a while.
		 * 
		 * Annoyingly, MASON's video capture system suffers the same problem. Hence this class. 
		 */
		try {
		    Thread.sleep(500);                 //1000 milliseconds is one second.
		} catch(InterruptedException ex) {
		    Thread.currentThread().interrupt();
		}
		
		// Hacked together from mason's Display3D.takeSnapshot() method. 
		Simulation sim = (Simulation) state;
		long step = sim.schedule.getSteps();		// used in creating a filename.
		double time = sim.schedule.getTime();
		String seqNum = String.format("%03d", sequenceNum);
		String fileName = movieDir + "/img_" + seqNum + ".png";
		
        // start the image
        display.canvas.beginCapturing(false);
    	// doesnt seem to actally work
    	display.canvas.waitForOffScreenRendering();
		try
        {		
			File snapShotFile = new File(fileName);
	        BufferedImage image = display.canvas.getLastImage();
	        PNGEncoder tmpEncoder = new PNGEncoder(image, false,PNGEncoder.FILTER_NONE,9);
	        OutputStream stream = new BufferedOutputStream(new FileOutputStream(snapShotFile));
	        stream.write(tmpEncoder.pngEncode());
	        stream.close();
	        image.flush();  // just in case -- OS X bug?
        }
		catch (FileNotFoundException e) { } // fail
		catch (IOException e) { /* could happen on close? */} // fail
		sequenceNum ++;
	}
}
