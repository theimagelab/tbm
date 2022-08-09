package mrLoggers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import mrCore.Simulation;

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
    
 * Writes information pertaining to the time-step of the simulation to the file system. This is necessary for downstream
 * analysis that operates in terms of speeds. 
 * 
 * @author Mark N. Read
 *
 */
public class TimeLogger 
{

	public static void writeTimeData(String dir)
	{
		System.out.println("Writing time information to the filesystem.");
		try
		{
			String fileName = dir + "/_TimeIndex.csv";
			FileWriter fw = new FileWriter(fileName);
			BufferedWriter timeOut = new BufferedWriter(fw);
			timeOut.write("\n");
			timeOut.write("Time Index\n");
			timeOut.write(" ==================== \n");
			timeOut.write("Time [ms],Track\n");
			double currentTime = 0.0;
			double timeStepMiliSeconds = (60.0 * Simulation.sampleTimeSlice) * 1000;	// calculate seconds, convert to ms. 
				
			for (int i = 0 ; i < 10; i++)
			{				
				timeOut.write(currentTime + "," + i + "\n");
				currentTime += timeStepMiliSeconds;
			}
			timeOut.close();
				
		} catch (IOException ex) {
			System.out.println("ERROR: exception when writing to filesystem," + ex.toString());
		}
	}
}
