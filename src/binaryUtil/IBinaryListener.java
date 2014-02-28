package binaryUtil;
/**
 * SLHA parsing program
 * Copyright (C) 2014 Patrick Cowan
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of this License, or
 * (at your option) any later version.
 *
 *You should have received a copy of the GNU General Public License 
 *along with this program if not, see <http://www.gnu.org/licenses/>.
 */
/**
 * Interface used by the binary parser to allow outside classes
 * to know of writing progress. Example would be the controller listening to
 * the events then passing the information off to the view.
 * @author Patrick
 *
 */
public interface IBinaryListener {
	
	/**
	 * Called by the binary reader, indicates current
	 * writing progress on the binary file
	 * @param progress -- The total number of completed writes
	 * @param total -- The total number of queued writes.
	 */
	public void completedWrites(int progress, int total);
}
