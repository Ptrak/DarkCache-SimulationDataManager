package binaryUtil;

import guiControl.ParserProgramController;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import javax.swing.JOptionPane;
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
 * to be used by the controller to invoke model operations.
 * @author Patrick
 *
 */
public class ProgramModel implements Runnable {

	private ParserProgramController ctlr;
	private File[] files;
	private BinaryParser p;
	private File parent;
	private SlhaFile template;
	
	public ProgramModel(ParserProgramController controller)
	{
		ctlr = controller;
	}
	
	/**
	 * Parses the given directory of SLHA files. 
	 * When this method is used, the binary system is called upon
	 * @param directory
	 */
	public void ParseDirectory(File directory)
	{
		//start a new thread (within this file) that will go through and parse all of the files.
		//Basically it will build the binary and add all of the files (if they don't already exist.
		files = directory.listFiles();
		parent = directory;
		Thread t = new Thread(this);
		//tell the controller to tell the view what file to display in the tree for browsing, and to 
		//open up the list.
		
		//first, sort the files
		Arrays.sort(files, new FileComparator());
		
		for(File f: files)
		{
			//go through all of the files and give the view the 
			//first valid file. 
			try
			{
				SlhaFile s = new SlhaFile(f);
				ctlr.InvokeDisplayFileContents(s);
				template = s;
				break;
			}
			catch(Exception e){continue;}
		}
		
		t.start();	
		
	}
	
	/**
	 * Parses a selection of files
	 * When this method is called, a temporary cache of parsed files
	 * is created. 
	 * @param files
	 */
	public void parseFiles(File[] files)
	{
		this.run();
	}
	
	/**
	 * Closes this model down. Terminates any existing worker threads, then returns.
	 */
	public void Close()
	{
		//wait for this to complete then return.
	}

	public void run()
	{
		//open up a binary file.
		try {
			p = new BinaryParser(new File(parent.getAbsolutePath() + "\\binary.bin"));
			p.addBinaryListener(ctlr);
			int progress = 1;
			for(File f : files)
			{
				if(!p.CotainsFile(f) && !f.getName().contains(".bin") && !f.getName().contains(".log"))
				{
					try
					{
					SlhaFile temp = new SlhaFile(f);
					p.addFile(temp,template);
					template = temp;
					}
					catch(Exception e)
					{
						//the file could not be created, ignore it and continue.
					}					
				}
				ctlr.UpdateProgress(progress++, files.length);	//update the view for files being read in.
			}
			//after adding all of the files, check for inconsistencies and notify the user. (and close the writer stream.)
			p.closeInconsistencyLogger();
			if(p.getNumberOfInconsistencies() > 0)
			{
				JOptionPane.showConfirmDialog(null,
						p.getNumberOfInconsistencies() + " Inconsistencies found, Logged in \"inconsistencies.log\" within the directory"
						, "Inconsistencies Found", JOptionPane.PLAIN_MESSAGE,JOptionPane.INFORMATION_MESSAGE);
			}
			
		} catch (Exception e) 
		{
			e.printStackTrace(); 
		}
		
	}
	
	/**
	 * Adds the binary Listener to a list of classes to be notified in the case of a write event completing.
	 * Events will call upon the completedWrites method.
	 * @param listener -- The binary listener to be notified. Must implement the IBinaryListenerInterface.
	 */
	public void AddWriteListener(IBinaryListener listener)
	{
		p.addBinaryListener(listener);
	}

	/**
	 * Writes a data file to the given file. Uses all of the currently selected variables
	 * to write the file. 
	 * @param f -- The data file to be written to. 
	 */
	public void writeDataFile(File f) {
		//create the file
		if(!f.exists())
		{
			try
			{
				f.createNewFile();
			}
			catch(IOException e)
			{
				System.err.println("Could not create file!");
				return;
			}
		}
		//grab all of the selected variables to use with the binary.
		LinkedList<Variable> vars = ctlr.getSelectedVariables();
		//create a file writer and write the files. 
		
		//use ONLY the files that are legal(e.g. no bins,logs,etc.)
		LinkedList<File> legalFiles = new LinkedList<File>();
		for(File file: files)
		{
			if(file.getName().contains(".log") || file.getName().contains(".bin") || file.getName().contains(".dat"))
				continue;
			else
				legalFiles.addLast(file);
		}
		try
		{
			NumberFormat formatter = new DecimalFormat("0.00000000E00");
			PrintWriter output = new PrintWriter(new FileWriter(f));
			for(int i = 0; i < legalFiles.size(); i++)
			{

				for(Variable v: vars)
				{
					double d = p.getData(v, i);
					if(d < 0)
						output.print(formatter.format(d) + "    " );
					else
						output.print(" " + formatter.format(d) + "    ");
				}
				output.println();
			}
			output.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}
	
	
	
	public boolean hasFile()
	{
		return this.p != null;
	}
	
}

class FileComparator implements Comparator<File> {




	public int compare(File o1, File o2) {
		//get the file names, without extensions.
		int lVal = 0;
		int rVal = 0;
		try
		{
		String lName = o1.getName();
		lName = lName.substring(0, lName.lastIndexOf('.'));
		String rName = o2.getName();
		rName = rName.substring(0, rName.lastIndexOf('.'));
		
			lVal = Integer.parseInt(lName);
			rVal = Integer.parseInt(rName);
		}
		catch (Exception e)
		{
			return o1.getName().compareTo(o2.getName());
		}
		
		return lVal-rVal;
	}
}
