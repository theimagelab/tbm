package loggers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import core.Cell;
import core.Simulation;
import sim.engine.Schedule;
import sim.engine.SimState;
import sim.engine.Steppable;
import sim.util.Double3D;


/**
 * 
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
 * This class encapsulates the functionality of logging cell movements, and writing them to the filesystem. 
 * It contains a subclass, Track, one of which is associated with each tracked cell in the simulation. 
 * 
 * It is indented that a single simulation only ever has one instance of the CellLogger class. 
 * 
 * @author Mark Read
 */
public class CellLogger 
{
	// a way of keeping hold of all the cell loggers tracking this type of cell together. 
	public static ArrayList<Track> tracks = new ArrayList<Track>();
	public static ArrayList<CellType> CellTypes = new ArrayList<CellType>();

	/**
	 * A single Track object is associated with a single Neutrophil object. The Track is responsible for tracking 
	 * the Neutrophil's movements and compiling statistics thereof. 
	 * 
	 * @author Mark Read
	 */
	public static class Track implements Steppable
	{
		private Cell target;		
				
		private ArrayList<Double3D> positionLog = new ArrayList<Double3D>(); 
		private ArrayList<String> positionLogCellType = new ArrayList<String>(); 
		private ArrayList<Long> meanderLog = new ArrayList<Long>(); 


		private ArrayList<Double> timeLog = new ArrayList<Double>();	// the actual time
		private ArrayList<Long> timeIterLog = new ArrayList<Long>();	// iterative count of timesteps so far. 
		
		public Track(Cell targetCell)
		{
			this.target = targetCell;	
			tracks.add(this);			
			
			Schedule sched = Simulation.instance.schedule;
			double startTime = sched.getTime();
			if (startTime < 0.0)
				startTime = 0.0;
			sched.scheduleRepeating(startTime, Simulation.loggerOrdering, this, Simulation.sampleTimeSlice);
		}
		
		
		@Override
		public void step(SimState state) 
		{				
			if (Simulation.space.cellField.exists(target) == true) {
				positionLog.add(target.getCurrentLocation());
				positionLogCellType.add(target.getType());
				meanderLog.add((long) target.getMeanderCount());			
				timeLog.add(Simulation.instance.schedule.getTime());
				timeIterLog.add(Simulation.instance.timeIter);
			}
		}
	}
	

	public static class CellType implements Steppable
	{
		private Cell target;		
				
		private ArrayList<Long> countLog = new ArrayList<Long>(); 
		private ArrayList<Long> removedCountLog = new ArrayList<Long>(); 
		private ArrayList<Double> timeLog = new ArrayList<Double>();	// the actual time
		private ArrayList<Long> timeIterLog = new ArrayList<Long>();	// iterative count of timesteps so far. 

		
		public CellType(Cell targetCell)
		{
			this.target = targetCell;	
			CellTypes.add(this);			
			
			Schedule sched = Simulation.instance.schedule;
			double startTime = sched.getTime();
			if (startTime < 0.0)
				startTime = 0.0;
			sched.scheduleRepeating(startTime, Simulation.loggerOrdering, this, Simulation.sampleTimeSlice);
		}

		@Override
		public void step(SimState state) {
			countLog.add((long) target.getCount());	
			removedCountLog.add((long) target.getRemovedCount());	
			timeLog.add(Simulation.instance.schedule.getTime());
			timeIterLog.add(Simulation.instance.timeIter);		}
		
	}
	
	public CellLogger()
	{}
	
	public void writeTrackData(String dir)
	{	
		System.out.println("Writing cell track data to the filesystem.");
		try 
		{
			BufferedWriter positionOut = setupPositionOutputFile(dir + "/_Position.csv", "Position");

			int trackID = 0;	// for writing to FS. Incremented for every encounter of a cell in imaging volume.			
			for (Track track : tracks)
			{
				/* Track ID is incremented for each new track that appears inside the imaging volume, and whenever
				 * an existing track re-enters the volume. Start it on false (as newly enountered tracks are assumed
				 * to not be in the volume).
				 */				
				boolean previouslyInsideVolume = false;
				// need a new track ID for every new track. 
				for (int t = 0; t < track.positionLog.size(); t++)
				{
					double x = track.positionLog.get(t).x;
					double y = track.positionLog.get(t).y;
					double z = track.positionLog.get(t).z;	
					String type = track.positionLogCellType.get(t);
					double meanderCount = track.meanderLog.get(t);
					if (Simulation.space.insideImagingVolume(x, y, z))							
					{
						// check if the track ID needs to be incremented. Done for every new track, and every re-entry
						// of existing tracks into the imaging volume. 
						if (previouslyInsideVolume == false)
						{	
							trackID ++;
							previouslyInsideVolume = true;
						}	
						positionOut.write(
							x + "," + y + "," + z + "," + 
							"um,Spot,Position," + 
							track.timeIterLog.get(t) + 
							"," + trackID + ","+ type + "," + meanderCount +"\n");
					} else {
						previouslyInsideVolume = false;
					}
				}				
			}
			positionOut.close();
				
		} catch (IOException ex) {
			System.out.println("ERROR: exception when writing to filesystem, " + ex.toString());
		}
	}
	
	public void writeCountData(String dir)
	{
		System.out.println("Writing cell count data to the filesystem.");
		try 
		{
			BufferedWriter countOut = setupCountOutputFile(dir + "/CellTypeCount.csv", "CellTypeCount");

			int trackID = 0;	// for writing to FS. Incremented for every encounter of a cell in imaging volume.			
			for (CellType type : CellTypes)
			{
				/* Celltype is incremented only when 
				 */				
				// need a new track ID for every new track. 
				for (int t = 0; t < type.countLog.size(); t++)
				{
					double count = type.countLog.get(t);
					countOut.write(
							count + "," + type.timeIterLog.get(t) + "," + type.target.getType() + "\n"
							);	
				}				
			}
			countOut.close();
				
		} catch (IOException ex) {
			System.out.println("ERROR: exception when writing to filesystem, " + ex.toString());
		}
	}
	
	public void writeRemovedCountData(String dir)
	{
		System.out.println("Writing remove count data to the filesystem.");
		try 
		{
			BufferedWriter countOut = setupCountOutputFile(dir + "/removeCount.csv", "removedCount");

			int trackID = 0;	// for writing to FS. Incremented for every encounter of a cell in imaging volume.			
			for (CellType type : CellTypes)
			{
				/* Celltype is incremented only when 
				 */				
				// need a new track ID for every new track. 
				for (int t = 0; t < type.removedCountLog.size(); t++)
				{
					double count = type.removedCountLog.get(t);
					countOut.write(
							count + "," + type.timeIterLog.get(t) + "," + type.target.getType() + "\n"
							);	
				}				
			}
			countOut.close();
				
		} catch (IOException ex) {
			System.out.println("ERROR: exception when writing to filesystem, " + ex.toString());
		}
	}
	
	
	private static BufferedWriter setupPositionOutputFile(String fileName, String title) throws IOException
	{
		FileWriter fw = new FileWriter(fileName);
		BufferedWriter out = new BufferedWriter(fw);
		out.write("\n");
		out.write(title + "\n");
		out.write("==================== \n");
		out.write("Position X,Position Y,Position Z,Unit,Category,Collection,Time,Parent,ID,meanderCount\n");	
		return out;
	}
	
	private static BufferedWriter setupCountOutputFile(String fileName, String title) throws IOException
	{
		FileWriter fw = new FileWriter(fileName);
		BufferedWriter out = new BufferedWriter(fw);
		out.write("\n");
		out.write(title + "\n");
		out.write("==================== \n");
		out.write("Count,Time, CellType\n");	
		return out;
	}
}

