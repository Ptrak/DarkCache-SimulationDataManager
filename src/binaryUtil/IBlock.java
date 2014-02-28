package binaryUtil;

import java.security.InvalidParameterException;
import java.util.ArrayList;
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
 * This interface describes a block object. There are two types of blocks. Regular Slha Blocks, and Decay Blocks.
 * Blocks are required to support the addition of variables, and the ability to return them in list format
 * @author Patrick
 *
 */
public interface IBlock {
	/**
	 * Adds a variable to be stored in this field.
	 * @param PDG
	 * @param PDG_2
	 * @param Value
	 * @param Description
	 * @return
	 */
	public boolean addVariable(String PGD, String PGD_2, String PGD_3, String value, String description, String blockName);
	
	/**
	 * Returns all of the variables associated with this block
	 */
	public ArrayList<Variable> getVariables();
	
	/**
	 * Returns the name (or description) of this block.
	 * @return -- A string that describes the block data.
	 */
	public String BlockName();
	
	/**
	 * Checks to see if this block contains the variable passed (with the exception of the data)
	 * Only fields that are compared are PDG, PDG2, PDG3(for decay).
	 */
	public boolean containsVariable(Variable v);

	/**
	 * Returns the variable in this block based on the name of the given variable
	 */
	public Variable getVariable(String VariableDescription) throws InvalidParameterException;

}
