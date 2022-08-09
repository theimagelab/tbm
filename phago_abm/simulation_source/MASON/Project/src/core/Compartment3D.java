package core;

import java.util.ArrayList;

import sim.engine.SimState;
import sim.engine.Steppable;
import sim.field.continuous.Continuous3D;
import sim.field.grid.IntGrid3D;
import sim.util.Bag;
import sim.util.Double3D;
import sim.util.IntBag;
import utils.Utils;

/**
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Represents the 3D tissue volume.
 * 
 * @author Mark N. Read and Wunna Kyaw
 * 
 * Changes made by Wunna: 09 Aug 2022.
 * I have added functionality to initialize and restrict cell motion inside spherical volume.
 * This is encapsulated in the addition of 3 functions: outsideSphericalVolume(), sphericalBoundary(), placeCellRandomlyInSphere().
 *
 */
@SuppressWarnings("serial")
public class Compartment3D implements Steppable {
	
	/*
	 * Note that these fields are deliberately public - they have to be for the GUI
	 * to latch onto them for portrayal.
	 */
	public Continuous3D cellField; // stores cells in a continuous space
	public IntGrid3D restrictedField; // stores restrictions on cell movement. 0 = no restriction, 1 = blocked.
	final Double3D center = new Double3D((double) Simulation.tissueRadius, (double) Simulation.tissueRadius, (double) Simulation.tissueRadius);

	public Compartment3D() {
		cellField = new Continuous3D(1.0, // discretization, dividing space into regions for maintaining a map of
											// objects' locations.
				Simulation.tissueWidth, Simulation.tissueHeight, Simulation.tissueDepth);
		cellField.clear();
		restrictedField = new IntGrid3D(Simulation.tissueWidth, Simulation.tissueHeight, Simulation.tissueDepth, 0);

		//createHairFollicles();
	}

	@Override
	public void step(SimState arg0) {
	}

	/**
	 * Populate the space with cells, placing each cell randomly in the volume such
	 * that cells do not overlap.
	 */
	public void placeCellRandomly(Cell cell) {
		Double3D loc; // will try to find a location for the neutrophil.
		// local copies of parameter values, to make equations below more readable.
		final double w = Simulation.tissueWidth;
		final double h = Simulation.tissueHeight;
		final double d = Simulation.tissueDepth;
		final double bf = Simulation.bufferSize;
		do {
			// select a random location.
			// neutrophils can be placed in the tissue volume, and similar sized volumes all
			// around it (except
			// above it, because that breaches the skin).
			final double lw = (Simulation.rng.nextDouble() * (w + (2.0 * w * bf))) - (w * bf);
			final double lh = (Simulation.rng.nextDouble() * (h + (2.0 * h * bf))) - (h * bf);
			final double ld = Simulation.rng.nextDouble() * (d + (d * bf));

			loc = new Double3D(lw, lh, ld);
		} while (!isOccupiableSpace(loc, cell));
		this.cellField.setObjectLocation(cell, loc);
	}
	

	
	public void placeCellRandomlyInSphere(Cell cell, boolean spawn) {
		Double3D loc; // will try to find a location
		// local copies of parameter values, to make equations below more readable.
		final double R = Simulation.tissueRadius;
		do {
			// select a random location in spherical coords
			
			double theta = Simulation.rng.nextDouble() * 2 * Math.PI;
			double phi = Simulation.rng.nextDouble() * 1 * Math.PI;
			double r = Simulation.rng.nextDouble() * R;
			
			double x = r * Math.cos(theta) * Math.sin(phi) + R; // translate because origin is not the center of simulation, but the topleft corner..
			double y = r * Math.sin(theta) * Math.sin(phi) + R;
			double z = r * Math.cos(phi) + R;
			


			loc = new Double3D(x, y, z);
		} while (!isOccupiableSpace(loc, cell));
		this.cellField.setObjectLocation(cell, loc);
		
		if (spawn) {
			cell.setStartLoc(loc);
		}

	}
	

	/**
	 * Checks whether the particular cell would collide with any others, were it to
	 * be placed at the supplied location.
	 * 
	 * @param cellLoc
	 * @param cell
	 * @return True if there is a collision.
	 */
	private boolean cellularCollisions(Double3D cellLoc, Cell cell) {
		// this is a conservative estimate, true as long as the biggest cell's diameter
		// isn't more than
		// twice the diameter as the smallest.
		double distance = 2 * cell.getDiameter();
		boolean collision = false; // the default case, we will look for the exception below.
		Bag neighbours = cellField.getNeighborsExactlyWithinDistance(cellLoc, distance, false, true, true, null);
		// remove `cell` if it is already placed in the tissue space (clearly it will
		// collide with itself).
		neighbours.remove(cell);
		for (Object other : neighbours) {
			double minDistance = ((Cell) other).getRadius() + cell.getRadius();
			// minimum distance at which there is no collision.
			Double3D otherLoc = cellField.getObjectLocation(other);
			// actual distance between the two cells.
			distance = Math.sqrt(((cellLoc.x - otherLoc.x) * (cellLoc.x - otherLoc.x))
					+ ((cellLoc.y - otherLoc.y) * (cellLoc.y - otherLoc.y))
					+ ((cellLoc.z - otherLoc.z) * (cellLoc.z - otherLoc.z)));
			if (distance <= minDistance) {
				collision = true;
				break;
			}
		}
		return collision;
	}

	/**
	 * Moves a cell in space, providing collision avoidance with other cells. A cell
	 * must be specified, and its preferred movement (movement, not preferred
	 * location!). The preferred movement is used to calculate the eventual
	 * movement, but is adjusted to ensure that no spatial co-occupancies occur.
	 * 
	 * Returns an array containing two Double3Ds. The first is the new location is
	 * returned, as it may be useful to store this locally. The second is a 'bounce'
	 * vector. This is a sum of the normals of contacts that 'cell' makes with
	 * neighbours. The bounce vector can be used to allow cells to slide over one
	 * another, if it is summed with the next movement vector. Bounce vector is of
	 * zero length if no contact occurred.
	 * 
	 * Note that his algorithm does not work so well for what seems like
	 * simultaneous collisions with two things. The reason is that with cells that
	 * don't squash, at this level of precision, one object is always hit before
	 * another, and hence the cell will make very small bounces between the two,
	 * rather than hitting both simultaneously.
	 */
	public MoveResults moveCellCollisionDetection(Cell cell, Double3D move) {
		/*
		 * This algorithm is based on that found in
		 * http://www.gamasutra.com/view/feature/131424/pool_hall_lessons_fast_accurate_
		 * .php?page=2 Accessed on the 13th June 2014.
		 * 
		 * Some notation for what is happening here. The 'cell's current location is
		 * labelled C. Cell is to make a move, initially proposed to be 'move', but
		 * shortened to avoid collisions as needed. The location that the move will take
		 * C to is S.
		 * 
		 * 'Other's current location is labelled O. Hence, vector CO is the distance
		 * from the centre of 'cell', and the centre of 'other'. Vector CS is the
		 * distance from the centre of 'cell' to its resting place at the end of the
		 * move, S.
		 * 
		 * In calculating if there is a collision between 'cell' and 'other', the point
		 * along CS closest to O needs to be calculated. This point is R.
		 * 
		 * Vector RO is the distance between the centre of 'other', and where 'cell'
		 * will be closest to O. Point T lies along CS. It is the nearest point along CR
		 * where 'cell' touches 'other'. This is where 'cell' should stop to avoid a
		 * collision. T is only calculated if there will be a collision. Vectors ending
		 * in 2 indicate that they are the squared distance. This is done to avoid
		 * taking square roots, which are computationally expensive.
		 * 
		 * Wunna Modifications: If a cell is about to move outside the spherical volume, the cell 
		 * will bounce off the walls, as if it were bouncing off a cell travelling towards the centre of the sphere
		 */
		double bouncex = 0.0;
		double bouncey = 0.0;
		double bouncez = 0.0;
		double CSx = move.x;
		double CSy = move.y;
		double CSz = move.z;

		Double3D C = cellField.getObjectLocation(cell);
		// find cells other cells that the length of 'move' could bring 'cell' to
		// collide with (omnidirectional).
		Bag potentialColliders = cellField.allObjects;
		// record the cells with which there was an actual collision.
		ArrayList<Cell> colliders = new ArrayList<Cell>();
		for (Object o : potentialColliders) {
			Cell other = (Cell) o;
			if (!(o == cell)) // cell can't collide with itself.
			{
				Double3D O = cellField.getObjectLocation(other);
				// dealing with hair follicles. This effectively places a sphere representing
				// the follicle at the exact
				// same z-coordinate as the 'cell'.
				/*
				 * check if current movement will bring 'cell' within touching range of 'other',
				 * ignoring direction.
				 */

				double COx = O.x - C.x;
				double COy = O.y - C.y;
				double COz = O.z - C.z;
				double touchRadii = cell.getRadius() + other.getRadius();
				double touchRadii2 = touchRadii * touchRadii;
				double CO2 = COx * COx + COy * COy + COz * COz; // CO squared.
				double CO = Math.sqrt(COx * COx + COy * COy + COz * COz);
				double CS = Math.sqrt(CSx * CSx + CSy * CSy + CSz * CSz);

				// if true, 'other' is within touching distance of 'cell'. This is simply
				// whether they are within range,
				// not accounting for directon.
				if (CS > (CO - touchRadii)) {
					/*
					 * check if the movement vector is in the direction of the 'other' cell. Ignore
					 * if it is not. (it might be moving away from 'other', rather than towards it).
					 */
					double dotP = COx * CSx + COy * CSy + COz * CSz; // dot product.
					if (dotP > 0.0) // if true, 'cell' is moving towards 'other'.
					{
						/*
						 * check whether the CS vector will actually bring 'cell' within touching
						 * distance of 'other'. Is it here that point R must be calculated.
						 */
						double CSunitx = CSx / CS;
						double CSunity = CSy / CS;
						double CSunitz = CSz / CS;
						// The geometry here is a right angle triangle, between C, O and R. The
						// rightangle is CRO.
						// we have three equations - note that dp means dot product:
						// 1. cos(a) = adjacent / hypotenuse. Angle a is RCO.
						// 2. dp(v,w) = |v|*|w|*cos(a).
						// 3. dp(v,w) = vx*wx + vy*wy + vz*wz
						//
						// combining 1 and 2 gives:
						// 4. dp(v,w) = |v|*|w|* (|adj| / |hyp|)
						//
						// plug some variables into 4. Construct a right angle triangle. Hyp = CO, Adj =
						// CR (what we are
						// trying to find). Opposite = RO, though we don't use it in this calculation.
						// dp(CS,CO) = |CS| * |CO| * |CR| / |CO| the COs cancel each other out
						// dp(CS,CO) = |CS| * |CR|
						//
						// We can use the unit vector of CS, which has the same direction as CS, but
						// length 1. Hence,
						// dp(CSunit,CO) = |CR|
						//
						// Hence, dot product of CS unit and CO gives the distance along 'CS' closest to
						// O (i.e., CR).
						double CR = COx * CSunitx + COy * CSunity + COz * CSunitz; // this is the dot product, gives
																					// opposite.
						double CR2 = CR * CR; // gives opposite squared.
						double RO2 = CO2 - CR2;
						// if true, then cells get within contact range of one another.
						if (RO2 <= touchRadii2) // square minSeparation to avoid square roots.
						{
							/* The cells will collide, here we find point T, and vector CT. */
							colliders.add(other);
							// This is also done using a right angle triangle, between O, R and T. Right
							// angle is ORT.
							// hyp = TO. The other two sides are TR and OR. We wish to find TR.
							double TO2 = touchRadii2;
							double TR2 = TO2 - RO2;
							double TR = Math.sqrt(TR2);
							double CT = CR - TR;
							double shrinkFactor = CT / CS;
							double CTx = CSx * shrinkFactor;
							double CTy = CSy * shrinkFactor;
							double CTz = CSz * shrinkFactor;
							// set point S to point T, ready for consideration of other potentially
							// colliding cells.
							CSx = CTx;
							CSy = CTy;
							CSz = CTz;
							// calculate vector OT, since this is the normal to the contact between 'cell'
							// and 'other'.
							// This can be used to create a 'bounce' vector, which can be used to have cells
							// slide over
							// one another.
							double Tx = C.x + CTx; // these are coordinates, not magnitudes.
							double Ty = C.y + CTy;
							double Tz = C.z + CTz;
							// these are directional vector components. They constitute the normal of the
							// contact between
							// 'cell' and 'other'. Unit vectors so that multiple bounces count equally.
							double bx = Tx - O.x;
							double by = Ty - O.y;
							double bz = Tz - O.z;
							double blen = Math.sqrt(bx * bx + by * by + bz * bz);
							bouncex += bx / blen;
							bouncey += by / blen;
							bouncez += bz / blen;
						}
					}
				}
			}
		}
	

		Double3D bounce = new Double3D(bouncex, bouncey, bouncez);
		if (bounce.lengthSq() > 0.0)
			bounce = Utils.unitVector(bounce);
		Double3D newLoc;
		
		newLoc = new Double3D(C.x + CSx, C.y + CSy, C.z + CSz);
		cellField.setObjectLocation(cell, newLoc);

		return new MoveResults(newLoc, bounce, colliders); // return information.
	}
	
	public void sphericalBoundary(Cell cell, boolean randomly_reposition) {
		if (outsideSphericalVolume(cell)) {
			if (randomly_reposition && cell.getType() == "BCellLogNorm") {
				
				Simulation.space.cellField.remove(cell);
				Fragment frag = new Fragment(Simulation.instance.schedule);
				Simulation.space.placeCellRandomlyInSphere(frag, true);
				SimulationBCell.fragsln.add(frag);
			} else {
				// move towards the center of the sphere. 
				Double3D C = cellField.getObjectLocation(cell);

				double CCenx = center.x - C.x ;
				double CCeny = center.y - C.y;
				double CCenz = center.z - C.z;
				double CCen = Math.sqrt(CCenx*CCenx+ CCeny*CCeny + CCenz*CCenz);
				
				double CSx = cell.getSpeed() * CCenx / CCen;
				double CSy = cell.getSpeed() * CCeny / CCen;
				double CSz = cell.getSpeed() * CCenz / CCen;
				Double3D newLoc = new Double3D(C.x + CSx, C.y + CSy, C.z + CSz);
				cellField.setObjectLocation(cell, newLoc);
			}
		}
	}
	

	/**
	 * Used solely for grouping together information passed back to cell following
	 * an attempted move in space
	 */
	public static class MoveResults {
		public Double3D newLocation;
		public Double3D bounce;
		public ArrayList<Cell> colliders;

		public MoveResults(Double3D newLoc, Double3D bounce, ArrayList<Cell> colliders) {
			this.newLocation = newLoc;
			this.bounce = bounce;
			this.colliders = colliders;
		}
	}

	/**
	 * Concrete classes queried on whether the sphere placed at location with given
	 * radius breaches any other objects. Overridden by subclasses.
	 */
	protected boolean restrictedSpace(Double3D location, double radius) {
		return false;
	}

	/**
	 * Returns true if the specified location (in continuous space) can be occupied
	 * be supplied cell.
	 */
	public boolean isOccupiableSpace(Double3D location, Cell cell) {
		boolean collision = cellularCollisions(location, cell);
		boolean restricted = restrictedSpace(location, cell.getRadius()); // subclasses indicate restricted space.
		
		//boolean outsideEpidermis = outsideEpidermis(location, cell);
		// location cannot be occupied if it is restricted, or if it will result in a
		// collision.
		return !(restricted || collision );
	}
	/**
	 * Returns true if the supplied location resides within a cell.
	 */
	public boolean insideCell(Double3D location) {
		for (Object obj : cellField.getAllObjects()) {
			Cell c = (Cell) obj;
			Double3D nLoc = c.getCurrentLocation();
			if (Utils.displacement(nLoc, location) < c.getRadius())
				return true;
		}
		return false;
	}

	
	/**
	 * Will return all the locations within a 2D discrete grid that lie within
	 * `distance` of the coordinate (originX, originY). The grid has dimensions
	 * (0,0) to (fieldWidth, fieldHeight). A location is considered within the
	 * circle if its center is within distance of the specified point. `xPos` and
	 * `yPos` must be supplied, and the locations are returned in these positions.
	 * The origin is always included.
	 * 
	 * This method was necessary because the MASON methods in 3D don't work
	 * properly. The MASON code for these methods looks pretty hacked, so I have
	 * created my own.
	 */
	public static void radial2DLocations(int originX, int originY, int fieldWidth, int fieldHeight, double distance,
			IntBag xPos, IntBag yPos) {
		if (xPos == null) {
			xPos = new IntBag();
		}
		if (yPos == null) {
			yPos = new IntBag();
		}
		xPos.clear();
		yPos.clear();

		// all the locations to be returned must lie within a square, centered on
		// (originX, originY).
		// This defines the boundaries of that square.
		int squareWMin = (int) Math.round(originX - distance);
		int squareWMax = (int) Math.round(originX + distance);
		int squareHMin = (int) Math.round(originY - distance);
		int squareHMax = (int) Math.round(originY + distance);
		// ensure the boundaries do not lie outside of the grid.
		if (squareWMin < 0) {
			squareWMin = 0;
		}
		if (squareWMax >= fieldWidth) {
			squareWMax = fieldWidth - 1;
		}
		if (squareHMin < 0) {
			squareHMin = 0;
		}
		if (squareHMax >= fieldHeight) {
			squareHMax = fieldHeight - 1;
		}

		// go through each location in the grid, calculate its distance from the
		// specified origin,
		// and stores those within range.
		for (int x = squareWMin; x <= squareWMax; x++)
			for (int y = squareHMin; y <= squareHMax; y++) {
				double hyp = Math.sqrt(((x - originX) * (x - originX)) + ((y - originY) * (y - originY)));
				if (hyp <= distance) {
					xPos.add(x);
					yPos.add(y);
				}
			}
	}

	public void moveCell(Cell cell, Double3D newLocation) {
		cellField.setObjectLocation(cell, newLocation);
	}

	public Double3D getCellLocation(Cell cell) {
		return cellField.getObjectLocation(cell);
	}

	public boolean outsideSphericalVolume(Cell cell) {
		Double3D loc = cellField.getObjectLocation(cell);
		double r = Simulation.tissueRadius;
		Double3D co = new Double3D(r,r,r); //sphere Center
		double dist2 = Math.pow(loc.x - co.x,2) + Math.pow(loc.y - co.y, 2) + Math.pow(loc.z - co.z, 2);
		double boundedRadius2 = r * r;
		if (dist2 > boundedRadius2)
			return true;
		return false;
	}

	
	public boolean insideImagingVolume(Object o) {
		Double3D loc = cellField.getObjectLocation(o);
		if (loc.x < 0)
			return false;
		if (loc.x > Simulation.tissueWidth)
			return false;
		if (loc.y < 0)
			return false;
		if (loc.y > Simulation.tissueHeight)
			return false;
		if (loc.z < 0)
			return false;
		if (loc.z > Simulation.tissueDepth)
			return false;

		return true;
	}

	public boolean insideImagingVolume(double x, double y, double z) {
		if (x < 0)
			return false;
		if (x > Simulation.tissueWidth)
			return false;
		if (y < 0)
			return false;
		if (y > Simulation.tissueHeight)
			return false;
		if (z < 0)
			return false;
		if (z > Simulation.tissueDepth)
			return false;

		return true;
	}

	public boolean insideImagingVolume(Double3D location) {
		return insideImagingVolume(location.x, location.y, location.z);
	}

	/**
	 * Returns a Bag of those cells that lie within a radius of the specified cell.
	 * The specified cell is not included in the Bag.
	 */
	public Bag cellsInLocale(Cell cell, double radius) {
		Double3D loc = cellField.getObjectLocation(cell);
		Bag neighbours = cellField.getNeighborsExactlyWithinDistance(loc, radius);
		neighbours.remove(cell);
		return neighbours;
	}


}
