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
 * A Slha Field is an object that represents and individual block of 
 * a Slha File. Individual blocks contain variables with one or two PGD codes,
 * a floating point value, and an optional description of the data.
 * @author Patrick
 *
 */
public class SlhaBlock implements IBlock {

	//Member Variables
	private ArrayList<Variable> vars;
	private int numberOfVariables;
	private String name;
	
	/**
	 * Generates an empty SlhaField with no variables, and sets its name to 
	 * The passed string.
	 */
	public SlhaBlock(String name){

		this.name = name;
		vars = new ArrayList<Variable>();
		this.numberOfVariables = 0;
	}
	
	
	public boolean addVariable(String PDG, String PDG_2,String PGD_3, String Value, String Description, String blockName)
	{
		this.vars.add(new Variable(PDG, PDG_2, PGD_3, Value,  Description.trim(),blockName.trim()));
		return false;
	}
	
	
	public ArrayList<Variable> getVariables()
	{
		return this.vars;
	}
	
	
	public String BlockName()
	{
		return this.name;
	}
		
	
	public int hashCode()
	{
		int val = 0;
		for(int i = 0; i < this.name.length(); i++)
		{
			val += this.name.charAt(i);
		}
		return val;
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
	
	/**
	 * Gets the number of variables within this block.
	 */
	public int getNumberOfVariables()
	{
		return this.numberOfVariables;
	}
	
}


