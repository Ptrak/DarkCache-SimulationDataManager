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
 * 
 * A decay block is used for the special case in a Slha file where decay is present. Decay Blocks contain
 * additional information in declaration of the block, which will be saved here.
 *
 */
public class DecayBlock implements IBlock {

	//Member variables
	ArrayList<Variable> vars;
	
	private String headerCode;
	private String headerData;
	private String headerDescription;
	
	
	/**
	 * Creates a DecayBlock object with no variables.
	 * @param code --The code from the declaration of the decay block
	 * @param data --The data from the declaration of the decay block
	 * @param description (optional, but preferred) -- the description of the decay block 
	 */
	public DecayBlock(String code, String data, String description)
	{
		this.headerCode = code;
		this.headerData = data;
		this.headerDescription = description;
		vars = new ArrayList<Variable>();
	}

	/**
	 * Gets the code from the header of this block
	 */
	public String getHeaderCode()
	{
		return this.headerCode;
	}
	
	/**
	 * Gets the data from the header of this block
	 */
	public String getHeaderData()
	{
		return this.headerData;
	}
	
	/**
	 * Gets the description of this block from the header of this block.
	 */
	public String getHeaderDescription()
	{
		return this.headerDescription;
	}
	
	public boolean addVariable(String PGD, String PGD_2, String PGD_3,
			String value, String description,String blockName) {
		try
		{
		Variable v = new Variable(PGD, PGD_2, PGD_3, value, description, blockName);
		vars.add(v);
		}
		catch(Exception e)
		{
			return false;
		}
		return true;
	}

	
	public ArrayList<Variable> getVariables() {
		
		return this.vars;
	}

	
	public String BlockName() {
		
		return this.headerDescription;
	}
	
	
	public boolean containsVariable(Variable v) {
		for(Variable var: this.vars)
		{
			if(var.equals(v))
			{
				return true;
			}
		}		
		return false;
	}
	
	
	public Variable getVariable(String VariableDescription) {
		for(Variable var: this.vars)
		{
			if(var.getDescription().equalsIgnoreCase(VariableDescription))
			{
				return var;
			}
		}
		throw new InvalidParameterException();
	}
	

}
