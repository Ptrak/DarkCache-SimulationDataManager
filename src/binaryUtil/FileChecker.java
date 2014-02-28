package binaryUtil;

import java.util.ArrayList;
import java.util.LinkedList;
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
public class FileChecker {

	/**
	 * Used to report the differences between the two files. An inconsistency is defined as:
	 * a missing variable, a extra or non-matching variable in an individual block, a missing block of data.
	 * For this method, inconsistencies are defined with error codes.
	 * 
	 * Error Codes:
	 * 1 - missing block (block name is given)
	 * 2 - unmatched Variable.Tthe variable exists within the file to compare, but not in the template
	 * 		Information will be given in the following format:
	 * 		PDG PDG2 PDG3 blockName description
	 * 3 - Missing variable. the variable exists with the template, but not in the file to compare.
	 * 		
	 * @param template
	 * 		--The template file, this is the file that this method will compare to.
	 * @param fileToCompare
	 * 		--The file that is being compared to the template. inconsistencies between these files are against this file. Meaning
	 *			that reported result will cite this file as being inconsistent.
	 *@return
	 *		--Returns a linked list containing all of the reported errors in strings. Reported error follow the following format:
	 *			inconsistency found in file "comparing file" variable/block "missing/extra block or variable" found.
	 */
	public static LinkedList<String> reportDifferences(SlhaFile fileToCompare, SlhaFile template) 
	{
		//first we check to ensure that all blocks match
		LinkedList<String> foundErrors = new LinkedList<String>();
		ArrayList<String> validBlocks = new ArrayList<String>(); 	//This is used to track what blocks are valid, so we can avoid lookup errors.
		for(String templateBlocks: template.getBlocks())
		{
			if(!(fileToCompare.ContainsBlock(templateBlocks)))
			{
				//we are missing a block
				foundErrors.add("1" + templateBlocks);
				foundErrors.add("Missing Block: " + templateBlocks + " in file: " + fileToCompare.getFileName());
				continue;
			}
			validBlocks.add(templateBlocks);
		}
		
		//we have now done a block check, we must now check individual variables within each block
		for(String blockname: validBlocks)
		{
			//in this block, we will compare variables to each other, we must account for not only
			//missing variables, but also unmatched variables.
			IBlock templateBlock =  template.getField(blockname);
			IBlock compareBlock = fileToCompare.getField(blockname);
			//checking for extra/unmatched variables here.
			for(Variable var: compareBlock.getVariables())
			{
				if(!(templateBlock.containsVariable(var)))
				{
					//if the template does not contain this variable found in compare to file, then it is unmatched
					foundErrors.add("2 " + var.getPDG() + " " + var.getPDG2() + " " + var.getPDG3() + " " + templateBlock.BlockName() + " " + var.getDescription());
					foundErrors.add("Unmatched Variable: "+"PDG Code " +var.getPDG()+", "+var.getPDG2()+", "+var.getPDG3()+ " Description: " + var.getDescription() + "in block: " + templateBlock.BlockName() + " in File " + fileToCompare.getFileName());
				}
			}
			
			//here we search for missing variables
			for(Variable var: templateBlock.getVariables())
			{
				if(!(compareBlock.containsVariable(var)))
				{
					foundErrors.add("3 " + var.getPDG() + " " + var.getPDG2() + " " + var.getPDG3() + " " + templateBlock.BlockName() + " " + var.getDescription());
					foundErrors.add("Missing Variable: "+"PDG Code " +var.getPDG()+", "+var.getPDG2()+", "+var.getPDG3()+ " Description: " + var.getDescription() + "in block: " + templateBlock.BlockName() + " in File " + fileToCompare.getFileName() + " not found.");
				}
			}
		}
		
		return foundErrors;
	}
}
