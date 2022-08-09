package movement;

import core.Cell;
import sim.util.Double3D;
import utils.Quaternion;

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
public interface Translation 
{
	/** Returns the desired move as as a vector. 
	 * @param speedS_StD 
	 * @param speedS_Mean 
	 * @param speedM_StD 
	 * @param speedM_Mean */
	public Double3D move(Quaternion orientation, double speedM_Mean, double speedM_StD, double speedS_Mean, double speedS_StD);

	Quaternion newOrientation(Quaternion orientation, Cell cell);

	public Double3D move(Quaternion orientation, Cell cell);
}
