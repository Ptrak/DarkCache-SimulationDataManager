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
 * The variable is a helper class, a variable is a container for the 
 * pdg code(s), value, and optional description
 * @author Patrick
 *
 */
public class Variable
{
	//member variables
	private String PDG;
	private String PDG_2;
	private String PDG_3;
	private String block;
	private String value;
	private String description;

	/**
	 * Creates a variable to store within the slhaField. If an item isn't included
	 * within the data line (e.g. no pdg_2 or description) then pass null or empty string
	 * to the constructor
	 * @param PDG	--(required) The first PDG code of the variable
	 * @param PDG_2	--(optional) The second PDG Code of the variable 
	 * @param value --(required) The value of the current variable
	 * @param description --(optional) The description of this variable.
	 * 
	 */
	public Variable(String PDG, String PDG_2, String PDG_3, String value, String description, String blockName)
	{
		if(PDG == null || PDG.equalsIgnoreCase(""))
			this.PDG = "0";
		else
			this.PDG = PDG.trim();
		if(PDG_2 == null || PDG_2.equalsIgnoreCase(""))
			this.PDG_2 ="0";
		else
			this.PDG_2 = PDG_2.trim();
		this.value = value;
		if(PDG_3 == null || PDG_3.equalsIgnoreCase(""))
			this.PDG_3 ="0";
		else
			this.PDG_3 = PDG_3.trim();
		this.value = value;
		if(description == null)
			this.description = "";
		else
			this.description = description.trim();
		if(blockName == null)
			this.block = "";
		else
			this.block = blockName.trim();
		
		
	}
	

	/**
	 * gets the description of this variable (returns empty string if there is no description.)
	 */
	public String getDescription()
	{
		return this.description;
	}
	public String getPDG()
	{
		return this.PDG;
	}
	public String getPDG2()
	{
		return this.PDG_2;
	}
	public String getPDG3()
	{
		return this.PDG_3;
	}
	public String getValue()
	{
		return this.value;
	}
	public String getBlock()
	{
		return this.block;
	}
	@Override
	public String toString()
	{
		String returnString = "";
		if(!this.block.equalsIgnoreCase(""))
			returnString+= "BLOCK: " + this.block + " ";
//		if(!this.PDG.equalsIgnoreCase("0"))
//			returnString += "Code: " + this.PDG + " ";
//		if(!this.PDG_2.equalsIgnoreCase("0"))
//			returnString +=   this.PDG_2 + " ";
//		if(!this.PDG_3.equalsIgnoreCase("0"))
//			returnString +=   this.PDG_3 + " ";
//		if(!this.block.equalsIgnoreCase(""))
//			returnString += "\t BlockName: " + this.block + " ";
		if(!this.description.equalsIgnoreCase(""))
			returnString += " Description: " + this.description;
//		if(!this.value.equalsIgnoreCase(""))
//			returnString += "\t Value: " + this.value + " ";
		return returnString;
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Variable))
			return false;
		Variable comparingTo = (Variable)o;


		if(comparingTo.getPDG().equalsIgnoreCase(this.PDG)
				&& comparingTo.getPDG2().equalsIgnoreCase(this.PDG_2)
				&& comparingTo.getPDG3().equalsIgnoreCase(this.PDG_3)
				&& comparingTo.getBlock().equalsIgnoreCase(this.block))
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		int ScalingFactor = 0;
		char[] c = this.block.toCharArray();
		for(int i = 0; i < c.length; i++)
		{
			ScalingFactor += (int)c[i];
		}
		return (int) Math.sqrt(Double.parseDouble(this.PDG + PDG_2 + PDG_3)*ScalingFactor); 
	}
	
}

