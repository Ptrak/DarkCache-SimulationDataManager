package binaryUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
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
 * A slha reader written in java, when passed a slha file, will parse into data separated into blocks containing data points and store them.
 * @author Patrick
 *
 */
public class SlhaFile {
	HashSet<IBlock> data;
	Scanner parser;										//A scanner to run through the entire file.
	String line;										//A scanner to run through individual lines in the file.
	int numberOfVariables;
	private String fileName;									//A string to save the name of the file this is from.
	private long lastModified;
	

	/**
	 * reads in specified slha file, and parses it and stores it automatically. The information is stored in a hash map, 
	 * where the keys are the block names read in from the file, and the values are arrayLists of field objects, which 
	 * are containers for all of the information.
	 * @throws Exception --Throws an exception if the passed file is invalid
	 */
	public SlhaFile(File f) throws Exception{
		this(f.getAbsolutePath());
	}
	
	
	/**
	 * reads in specified slha file, and parses it and stores it automatically. The information is stored in a hash map, 
	 * where the keys are the block names read in from the file, and the values are arrayLists of field objects, which 
	 * are containers for all of the information.
	 * @throws Exception --Throws an exception if the file being parsed has invalid SLHA syntax
	 */
	public SlhaFile(String filepath) throws Exception{
		//get the filename and store it
		int lastSlash = filepath.lastIndexOf('\\');
		this.fileName = filepath.substring(lastSlash + 1);
		File f = new File(filepath);
		this.lastModified = f.lastModified();
		data = new HashSet<IBlock>();
		
		try {
			parser = new Scanner(f);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try
		{
		line = parser.nextLine();
		}
		catch(NoSuchElementException e)
		{
			throw new Exception();
		}
		
		while(parser.hasNextLine()){				//scan through the entire file
			
			Scanner s = new Scanner(line);
			String first = s.next();
			
			if(first.charAt(0) == '#'){ 			//ignore the line, it is a comment
				line = parser.nextLine();
				continue;						//get a new line, and start from the top
			}
			else if(first.equalsIgnoreCase("BLOCK")){		//Here we assume that if the first character of a line is a B, then it is the start of a new block.
				
				String blockName = s.next();	//grab the second word in the block definition header, this is all we want.

				
				this.data.add(parseInBlock(blockName));		//parsing code moved to private method to reduce constructor code complexity.
				
				continue;
			}
			else if(first.equalsIgnoreCase("DECAY")){
				//Here we are assuming that all DECAY blocks are the same and that they follow the same order
				//and number of values in the block declaration
				this.data.add(parseDecayBlock());
				
				continue;
				
			}
			else
			{
				s.close();
				throw new Exception();
			}
			
		} //end while
		if(this.data.isEmpty())
			throw new Exception();
		
	}
	
	/**
	 * Private method used to parse in Decay blocks from certain Slha files.
	 */
	private IBlock parseDecayBlock()
	{
		
		//first is the headerCode
		Scanner s = new Scanner(this.line);
		s.next();
		
		String headerCode = s.next();
		
		String headerValue = s.next();
		
		String description = "";
		s.next();
		while(s.hasNext()){													//now we gather the descritption(if possible.)
			String next = s.next();
			if(next.charAt(0) == '#')
				break;
			description += " " + next;
		}
		s.close();

		DecayBlock d = new DecayBlock(headerCode, headerValue, description);
		//now we add the individual variables
		line = parser.nextLine();
		while(line.charAt(0) != 'D' && line.charAt(0) != 'B')
		{
			if(line.charAt(0) == '#')
			{
				try{
					line = parser.nextLine();
					//in case we are at the end of the file
				}
				catch(Exception e){
					return d;
				}
				continue;				// We can still have comments in the blocks, so this code will automatically skip them.	
			}
			
			//don't forget to advance the scanner
			s = new Scanner(line);
			String value = s.next();
			String PGD = s.next();
			String PGD_2 = s.next();
			String PGD_3 = s.next();
			//get the description
			s.next();
			description = "";
			while(s.hasNext()){													
				String next = s.next();
				if(next.charAt(0) == '#')
					break;
				description += " " + next;
			}
			s.close();
			
			d.addVariable(PGD, PGD_2, PGD_3, value, description, "DECAY");
			try
			{
			line = parser.nextLine();
			}
			catch(NoSuchElementException e)
			{
				//we hit the bottom of the file, exit
				return d;
			}
		}

		return d;
	}
	
	/**
	 * Used for when we reach a block of data that needs to be parsed into the internal map.
	 * Will automatically determine how to fill out the field object based on number of tokens in each data line(excluding comments.)
	 */
	private IBlock parseInBlock(String blockName) {
	
		SlhaBlock field = new SlhaBlock(blockName);
											
		line = parser.nextLine();					//advance to the next line. (we assume that we start on the BLOCK line.
			
		while(line.charAt(0) != 'B' && line.charAt(0) != 'D'){				//we run until we reach another block.
			if(line.charAt(0) == '#'){				//skip comments
				try{
					line = parser.nextLine();
					//in case we are at the end of the file
				}
				catch(NoSuchElementException e){
					return field;
				}
				continue;				// We can still have comments in the blocks, so this code will automatically skip them.	
			}

			//here we parse the individual line.
			Scanner s = new Scanner(line);
			this.numberOfVariables++;
			ArrayList<String> tokenList = new ArrayList<String>();
			while(s.hasNext()){
				String currToken = s.next();
				if(currToken.charAt(0) == '#')
					break;
				tokenList.add(currToken);
			}
			
			//from what we have gotten from the individual line, we can determine what is assigned to it. 
			String data = "";
			String PDG = "";
			String PDG_2 = "";
			String PDG_3 = ""; 	//Unused for this type of block
			String description = "";
			
			
				if(tokenList.size() >= 1){
				int grabLocation = 0;	
				if(tokenList.size() == 1){											// we have only data
					data  = (tokenList.get(grabLocation));
					
					//grab the description of the variable
					while(s.hasNext()){													//now we gather the descritption(if possible.)
						String next = s.next();
						if(next.charAt(0) == '#')
							break;
						description += " " + next;
					}
					
					field.addVariable(PDG, PDG_2, PDG_3, data, description, blockName);
					try{
						this.line = parser.nextLine();
					}
					catch(NoSuchElementException e){
						//we have no more elements 
						s.close();
						return field;
					}
					continue;
				}
														//if we have only two values, there can be a problem, either data and descript, or 
				
				PDG = tokenList.get(grabLocation);		//We assume that the first value is ALWAYS A PGD CODE (if there is more than one token
				grabLocation++;
				if(tokenList.size() == 3){											//we assume we have 2 PGD codes if the number of tokens is 3.
					PDG_2 = (tokenList.get(grabLocation));
					grabLocation++;
				}
				for(int i = grabLocation; i < tokenList.size(); i++){				//after we collect the PGD codes, the rest(up to the comment marker) is data.
					data += tokenList.get(i) +" ";
				}
				while(s.hasNext()){													//now we gather the descritption(if possible.)
					String next = s.next();
					if(next.charAt(0) == '#')
						break;
					description += " " + next;
				}
				try{															//After filling out all possible we add the collected data to the field.
																				//start the next line.
					field.addVariable(PDG, PDG_2, PDG_3, data, description, blockName);
					this.line = parser.nextLine();
					while(line.length() == 0)
						this.line = parser.nextLine();
				}
				catch(NoSuchElementException e){
					//we have no more elements 
					s.close();
					return field;
				}
				continue;
			}//end if statment list.size() >=1
			s.close();
		}//end while determining the end of the block
		return field;
		
	}//end method
	
	/**
	 * Returns the counted number of variables in this file. Useful for comparing differences on the surface.
	 * @return The number of variables in this Slha File.
	 */
	public int getNumberOfVariables(){
		return this.numberOfVariables;
	}
	
	/**
	 * Returns the file name (not path) in which this file originated.
	 * @return	--A string with the fileName (with extension.)
	 */
	public String getFileName()
	{
		return this.fileName;
	}
	
	/**
	 * This method will return an IBlock implementing object of variables related to the block name passed
	 * @param block -- The block to be looked up
	 * @return --returns an IBlock of all the data associated with the block in this file. If no such block
	 * Exists, will return null.
	 * @throws --This method will throw and InvalidParameterException if the passed blockName does not exist
	 * within this file.
	 */
	public IBlock getField(String blockName)
	{
		for(IBlock f: this.data)
		{
			if(f.BlockName().equalsIgnoreCase(blockName))
				return f;
		}
		//we failed
		throw new InvalidParameterException();
	}
	
	/**
	 * Returns all of the known blocks read in from this file in a set.
	 * @return	--A set of strings containing all of the block names for the data in this file.
	 */
	public Set<String> getBlocks()
	{
		Set<String> set = new HashSet<String>();
		for(IBlock f: this.data)
		{
			set.add(f.BlockName());
		}
		return set;
	}
	
	/**
	 * reports whether or not this file contains a block with the same name (ignoring case)
	 * as the passed string.
	 * @param blockname
	 * 			--The blockname we are checking for
	 * @return
	 * 			--True if the block exists in this file, false otherwise.
	 */
	public boolean ContainsBlock(String blockname)
	{
		for(String s: this.getBlocks())
		{
			if(s.equalsIgnoreCase(blockname))
				return true;
		}
		return false;
	
	}
	
	/**
	 * returns the last modified date of this slha file, as reported by the original file.
	 */
	public long lastModified()
	{
		return this.lastModified;
	}

}
