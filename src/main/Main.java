package main;

import guiControl.ParserProgramController;

public class Main {
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
	 * The entry point for this program. This program will prompt the user for a directory to parse information from.
	 * The program will then parse the information in. While reading in the file, a progress bar will appear showing 
	 * current progress towards completing the parsing. After that, A new window will pop up displaying all found 
	 * variables. After the variables are all selected, the program will generate an output file to the specified 
	 * directory.
	 * @param args --Unused.
	 */
	public static void main(String args[])
	{
		if(args.length == 0)
			new ParserProgramController();	
		//else
			//TODO: implement command line params. 
	}
}
