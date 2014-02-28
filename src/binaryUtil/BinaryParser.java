package binaryUtil;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Scanner;

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
public class BinaryParser
{
	//NOTE: the inconsistency file will be overwritten automatically with each instance
	//of this object. 
	//private members used for tracking the internal binary file
	private int offset;
	private int availableVars;
	private int usedVars;
	private int availableFiles;
	private int usedFiles;
	private File binary;
	private HashMap<Variable, Long> variableInfo;
	private PrintWriter out;	//used for logging inconsistencies within files.
	private int loggedInconsistencies; //used to track the number of inconsistencies that have occurred in this instance.
	private HashMap<String, FileInfo> fileInfo; //a hash map used to track all of the files contained within this binary.
	
	//private members used for read/writes to the binary file
	private BinaryWorker worker;
	private RandomAccessFile bin;
	
	//A list of listeners to be notified of a writeComplete event.
	private LinkedList<IBinaryListener> listeners;
	
	//constants to help make code easier to understand
	private final float LOAD = 0.85f;	//The load factor for this object.
	private final int HEADER_SIZE = 16;
	private final int VAR_BLOCK_MAX_CHARS = 20;
	private final int VAR_DESC_MAX_CHARS = 25;
	private final int FILE_BINARY_SIZE = 62;		//the amount of space for each file entry in the table
	private final int VARIABLE_BINARY_SIZE = 110;	//the number of bytes for each variable entry int the table
	private static final int INITIAL_AVAILABLE_VARIABLES = 150;
	private static final int INITIAL_AVAILABLE_FILES = 2000;
	
	public BinaryParser(File BinaryFile) throws IOException
	{
		binary = BinaryFile;
		//initialize members.
		listeners = new LinkedList<IBinaryListener>();
		variableInfo = new HashMap<Variable, Long>();
		fileInfo = new HashMap<String, FileInfo>();
	
		loggedInconsistencies = 0;
	
		out = null;
		
		if(binary.exists())
		{
			this.bin = new RandomAccessFile(binary,"rwd");
			worker = new BinaryWorker(this.bin);
			new Thread(worker).start();
			//get values
			gatherHeader();
			//calculate the offset. (double - 8 * variable space allocated - 200/loaded in.)
			offset = 8*availableVars;
			//build file and variable table
			gatherVariables();
			//build file table and verify that there have been no changes to them.
			verifyFiles();
			
		}
		else
		{
			binary.createNewFile();
			this.bin = new RandomAccessFile(binary,"rwd");
			worker = new BinaryWorker(this.bin);
			new Thread(worker).start();
			createNewBinaryFile();			
			//calculate the offset. (double - 8 * variable space allocated - 200/loaded in.)
			offset = 8*availableVars;
			
			
		}
		
	}

	/**
	 * This method verifies that all file information in the binary is up to date. Any files that are found to be new
	 * or altered will be updated or added to the binary.
	 */
	private void verifyFiles() {
		try
		{
			for(int i = 0; i < usedFiles; i++)
			{
				//seek the location of the file info in the file table
				this.bin.seek(HEADER_SIZE + (availableVars * VARIABLE_BINARY_SIZE) + (i*FILE_BINARY_SIZE));
				
				StringBuilder fileName = new StringBuilder();
				long modificationDate = bin.readLong();
				for(int j = 0; j < 25; j++)
				{
					char c = bin.readChar();
					if(c == 0)
						break;
					fileName.append(c);
				}
				FileInfo temp = new FileInfo(modificationDate, bin.readInt());
				fileInfo.put(fileName.toString(), temp);
			}
		}
		catch(IOException e)
		{
			System.err.println("Error reading file table in binary");
			e.printStackTrace();
			return;
		}
		
		//now the files are verified. IT IS ASSUMED THAT THE BINAY FILE WILL BE WITHIN THE SAME DIRECTORY AS THE PARSED FILES.
		
		File template = null;
		for(File file: this.binary.getParentFile().listFiles())
		{
			int index = file.getName().lastIndexOf('.');
			FileInfo fInfo = null;
			if(index < 0)
				fInfo = fileInfo.get(file.getName());
			else
				fInfo = fileInfo.get(file.getName().substring(0, file.getName().lastIndexOf('.')));
			if(fInfo == null)
				continue;
			Long lastModified = fInfo.lastModified;
			if(lastModified == null)
			{
				try
				{
				this.addFile(new SlhaFile(file),new SlhaFile(template));
				} 
				catch(Exception e)
				{
					System.err.println("Error on the creation of slhafiles for file verification, please check to ensure that no data was destroyed");
					e.printStackTrace();
				}
				
			}
			else if(lastModified != file.lastModified())
			{
				//NOTE: map is now out of date for this file
				this.updateFile(file, template,fInfo);
			}
			template = file;
		}
		
	}

	/**
	 * Updates the file location in the binary file with new values if the file has been found to be modified.
	 * @param fInfo -- The file data location in the binary, required so modifications can be made to proper part of file.
	 * @param fileName --The name of the file to be updated within the binary.
	 * @param template -- a template to compare this file to, necessary for any potential additions to file.
	 */
	private void updateFile(File f, File template, FileInfo fInfo)
	{
		//a file has been found to be altered. re-add it to the binary, but adjust the values 
		SlhaFile file = null;
		try {
			file = new SlhaFile(f);
		} catch (Exception e) {
			//creation of the file failed. Print stack, abort.
			System.err.print("Error Creating SLHA file"+ f.getName() + "\n");
			e.printStackTrace();
			return;
		}

		//go through file, get variable, place in array in proper order
		//convert array to byte buffer and write it to the file. 
		ByteBuffer buffer = ByteBuffer.allocate(file.numberOfVariables*8);
		for(String b: file.getBlocks())
		{
			for(Variable v: file.getField(b).getVariables())
			{
				//ensure that variable exists
				Long firstInstance = variableInfo.get(v);
				if(firstInstance == null)
				{
					boolean newVariable = addVariable(v);
					if(newVariable)
					{
						//the variable is new, previous files must have an entry. This entry 
						//will be the maximum double value to signify an error in the data
						ByteBuffer buf = ByteBuffer.allocate(8);
						buf.putDouble(Double.MAX_VALUE);
						buf.flip();
						for(int i = this.usedFiles; i > 0; i--)
						{
							worker.addRequest(this.variableInfo.get(v) + (this.offset*i), buf.array());
						}
					}
					firstInstance = variableInfo.get(v);
				}
				//proceed with parsing the file. 
				int index = (int) (firstInstance - HEADER_SIZE - 
						(VARIABLE_BINARY_SIZE*availableVars) - (FILE_BINARY_SIZE*availableFiles));
				try
				{
					buffer.putDouble(index, Double.parseDouble(v.getValue()));
				}
				catch(NumberFormatException e)
				{
					//the value is a string. these will be removed from the binary file to
					//retain constant data access time.
				}
			}
		}	
		
		LinkedList<String> inconsistencies = new LinkedList<String>();
		SlhaFile temp = null;
		try {
			temp = new SlhaFile(template);
		} 
		catch (Exception e1) 
		{
			e1.printStackTrace();
			System.err.println("Error creating slha file" + template.getName()+" Failed to update file: " + file.getFileName());
			return;
		}
		try
		{
			inconsistencies = FileChecker.reportDifferences(file, temp);
		} 
		catch(Exception e)
		{
			System.err.println("Error detecting inconsistencies for file: " + file.getFileName() + " Template: " + temp.getFileName());
			e.printStackTrace();
		}
		for(String s: inconsistencies)
		{
			resolveError(s, temp);
		}

		//update the header values for when the binary is reopened.
		updateBinary();
		
		//write all of the updated variable data
		buffer.flip();
		long fileLocation = HEADER_SIZE + (VARIABLE_BINARY_SIZE * availableVars)
				+ (FILE_BINARY_SIZE * availableFiles) + (usedFiles * offset);
		worker.addRequest(fileLocation, buffer.array());
		//update the binary last modified date.
		ByteBuffer b = ByteBuffer.allocate(8);
		b.putLong(file.lastModified());
		b.flip();
		worker.addRequest(HEADER_SIZE + (availableVars*VARIABLE_BINARY_SIZE) + (fInfo.fileLocation * FILE_BINARY_SIZE), b.array());
	}

	/**
	 * Private helper method. Builds the map to navigate to the starting point for each variable
	 * using the information gathered from the variable table in the binary file.
	 */
	private void gatherVariables() throws IOException {
		//variable table starts at the end of the header
		bin.seek(HEADER_SIZE);
		for(int i = 0; i < usedVars; i++)
		{
			//build the variable table
			int PDG_1 = bin.readInt();
			int PDG_2 = bin.readInt();
			int PDG_3 = bin.readInt();
			//get the blockname. blocks are given 20 characters per variable
			StringBuilder blockName = new StringBuilder();
			for(int j = 0; j < VAR_BLOCK_MAX_CHARS; j++)
			{
				char c = bin.readChar();
				if(c == 0)
					continue;
				blockName.append(c);
			}
			String block = blockName.toString();
			StringBuilder description = new StringBuilder();
			
			for(int j = 0; j < VAR_DESC_MAX_CHARS; j++)
			{
				char c = bin.readChar();
				if(c == 0)
					continue;
				description.append(c);
			}
			String desc = description.toString();
			//first variable instance location
			long firstInstance = bin.readLong();
			//add to variable map
			variableInfo.put(new Variable(PDG_1 +"", PDG_2+"", PDG_3+"", null, desc.trim(), block.trim()) ,firstInstance);
		}
	}

	/**
	 * private helper method. Gathers the header information from the binary file regarding
	 * the current load factor of the binary file.
	 */
	private void gatherHeader() throws IOException {
		bin.seek(0);
		availableVars = bin.readInt();
		usedVars = bin.readInt();
		availableFiles = bin.readInt();
		usedFiles = bin.readInt();	
	}

	/**
	 * Creates a new binary file at the given location with the default size values.
	 */
	private void createNewBinaryFile() throws IOException 
	{
		//new files can hold 200 different variables initially, and have
		//room for 1000 files.
		ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE);
		availableVars = INITIAL_AVAILABLE_VARIABLES;
		usedVars = 0;
		availableFiles = INITIAL_AVAILABLE_FILES;
		usedFiles = 0;
		//build/write buffer.
		b.putInt(availableVars);
		b.putInt(usedVars);
		b.putInt(availableFiles);
		b.putInt(usedFiles);
		b.flip();
		worker.addRequest(0, b.array());
		
		//allocate empty space for the rest of the binary
		b = ByteBuffer.allocate((FILE_BINARY_SIZE*availableFiles) + (VARIABLE_BINARY_SIZE*availableVars));
		worker.addRequest(HEADER_SIZE, b.array());
	}
	
	public boolean addFile(SlhaFile file, SlhaFile template)
	{
		//we assume that the file does not already exist within the binary.
		if(usedFiles == availableFiles)
			grow();
		//use the slhaFileBuffer to convert all of the data into 
		//a single large buffer to be written 
		
		//add the file to the table
		long location = (HEADER_SIZE + (VARIABLE_BINARY_SIZE*availableVars) + usedFiles * FILE_BINARY_SIZE);
		
		ByteBuffer buffer = ByteBuffer.allocate(FILE_BINARY_SIZE);
		buffer.putLong(file.lastModified());
		char[] chars = file.getFileName().substring(0, file.getFileName().indexOf('.')).toCharArray();
		//copy the maximum number of characters over(26) and place in the buffer.
		char[] writeChars = new char[25];
		for(int i = 0; i < chars.length; i++)
			writeChars[i] = chars[i];
		for(int i = chars.length; i < 25; i++)
			writeChars[i] = 0;
		for(char c: writeChars)
			buffer.putChar(c);
		//finally, add the file number in the binary file, and write it
		buffer.putInt(usedFiles);
		buffer.flip();
		worker.addRequest(location, buffer.array());
		//go through file, get variable, place in array in proper order
		//convert array to byte buffer and write it to the file. 
		buffer = ByteBuffer.allocate(file.numberOfVariables*8);
		for(String b: file.getBlocks())
		{
			for(Variable v: file.getField(b).getVariables())
			{
				//ensure that variable exists
				Long firstInstance = variableInfo.get(v);
				if(firstInstance == null)
				{
					boolean newVariable = addVariable(v);
					if(newVariable)
					{
						//the variable is new, previous files must have an entry. This entry 
						//will be the maximum double value to signify an error in the data
						ByteBuffer buf = ByteBuffer.allocate(8);
						buf.putDouble(Double.MAX_VALUE);
						buf.flip();
						for(int i = this.usedFiles; i > 0; i--)
						{
							worker.addRequest(this.variableInfo.get(v) + (this.offset*i), buf.array());
						}
					}
					firstInstance = variableInfo.get(v);
				}
				//proceed with parsing the file. 
				int index = (int) (firstInstance - HEADER_SIZE - 
						(VARIABLE_BINARY_SIZE*availableVars) - (FILE_BINARY_SIZE*availableFiles));
				try
				{
				buffer.putDouble(index, Double.parseDouble(v.getValue()));
				}
				catch(NumberFormatException e)
				{
					//the value is a string. these will be removed from the binary file to
					//retain constant data access time.
				}
			}
		}	
		
		//detect inconsistencies
		LinkedList<String> inconsistencies = new LinkedList<String>();
		try
		{
			inconsistencies = FileChecker.reportDifferences(file, template);
		} 
		catch(Exception e)
		{
			System.err.println("Error detecting inconsistencies for file: " + file.getFileName() + " Template: " + template.getFileName());
			e.printStackTrace();
		}
		int i = 0; 
		for(String s: inconsistencies)
		{
			//the File checker method used will return the coded message for each even element 
			//and the message to write (in human readable terms) for each odd element.
			//The human readable message corresponds to the previous message.
			if(i%2 == 0)
				resolveError(s, template);
			else
			{
				if(out == null)
				{
					try
					{
					File logFile = new File(binary.getParent() + "\\inconsistencies.log");
					if(!logFile.exists())
						logFile.createNewFile();
				
					out = new PrintWriter(new FileWriter(logFile));
					}
					catch(IOException e)
					{
						e.printStackTrace();
						System.err.println("Error occured creating log file");
					}
				}
				out.println(s);
			}
			i++;
		}
		
		buffer.flip();
		long fileLocation = HEADER_SIZE + (VARIABLE_BINARY_SIZE * availableVars)
				+ (FILE_BINARY_SIZE * availableFiles) + (usedFiles * offset);
		worker.addRequest(fileLocation, buffer.array());
		
		//add the file to the map
		fileInfo.put(file.getFileName().substring(0, file.getFileName().lastIndexOf('.'))
				, new FileInfo(file.lastModified(), usedFiles));
		usedFiles++;
		
		//update the header values for when the binary is reopened.
		updateBinary();
		
		return true;
		
	}
	
	/**
	 * helper method used to  resolve inconsistencies 
	 * @param s
	 */
	private void resolveError(String inconsistency, SlhaFile template) 
	{
		//missing/unmatched variables can be added in separate writes for now, consider altering the buffer to save writes.
		Scanner s = new Scanner(inconsistency);
		//the errors will follow a specific format
		String blockName = "";
		int code = s.nextInt();
		switch(code)
		{
		case 1: 
			//missing block. resolve all errors
			//the error contains the missing block. all variables within this block from the template must be 
			//resolved
			this.loggedInconsistencies++;
			blockName = s.next();
			s.close();
			IBlock blockToAdd = null;
			try
			{
			blockToAdd = template.getField(blockName);
			}
			catch(InvalidParameterException e)
			{
				break;
			}
			for(Variable v : blockToAdd.getVariables())
			{
				long location = this.variableInfo.get(v);
				//write a double max to the offset location
				ByteBuffer b = ByteBuffer.allocate(8);
				b.putDouble(Double.MAX_VALUE);
				b.flip();
				this.worker.addRequest(location + (this.offset * this.usedFiles), b.array());
			}
			break;
		case 2:
			//unmatched variable, the controller should make it so this never happens.
			this.loggedInconsistencies++;
			s.close();
			break;
		case 3:
			//missing variable, (the ideal case)
			this.loggedInconsistencies++;
			String pdg = null, pdg_2 = null, pdg_3 = null;
			String description = null;
			try
			{
				pdg = s.next();
				pdg_2 = s.next();
				pdg_3 = s.next();
				blockName = s.next();
				description = s.next();
				
			}
			catch(NoSuchElementException e) {}	//Description does not exist most likley, use what has been provided.
			long location = this.variableInfo.get(new Variable(pdg,pdg_2,pdg_3,null,description, blockName));
			//write a double max to the offset location
			ByteBuffer b = ByteBuffer.allocate(8);
			b.putDouble(Double.MAX_VALUE);
			b.flip();
			this.worker.addRequest(location + (this.offset * this.usedFiles), b.array());
			s.close();
			break;
		default:
			this.loggedInconsistencies++;
			s.close();
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Helper method. Updates binary file header with new values for future access.
	 */
	private void updateBinary() {
		
		ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE);
		b.putInt(availableVars);
		b.putInt(usedVars);
		b.putInt(availableFiles);
		b.putInt(usedFiles);
		try
		{
		worker.addRequest(0, b.array());
		}
		catch(IllegalArgumentException e)
		{
			e.printStackTrace();
			System.err.print("Error Updating File");
		}
	}
	
	/**
	 * Helper method. Adds a variable to the binary file. If the variable already exists,
	 * the method will return false, otherwise the method will return true.
	 * @param var -- The variable to be added.
	 */
	private boolean addVariable(Variable var) {
		if(usedVars == availableVars)
			grow();
		//check to see if the variable already exists, if it does, just add the info
		Long l = this.variableInfo.get(var);

		if(l == null)
		{

			//the map does not contain the variable, add it in
			//variable information starts at byte 16. variables are allocated 90 bytes each, so we multiply the number of variables in the file
			//by the offset value to find how far in the header we must move ahead.			
			long offset = (this.usedVars * VARIABLE_BINARY_SIZE) + HEADER_SIZE;

			//write the variable information here. Information regarding how the variable headers are set up is in a separate file (hopefully included with
			//this source.)

			//use a byteBuffer, then write the entire buffer to the binary.
							

			ByteBuffer b = ByteBuffer.allocate(VARIABLE_BINARY_SIZE);

			b.putInt(Integer.parseInt(var.getPDG()));
			b.putInt(Integer.parseInt(var.getPDG2()));
			b.putInt(Integer.parseInt(var.getPDG3()));
			char[] letters = var.getBlock().trim().toCharArray();
			for(int i = 0; i < 20; i++)
			{
				if(i >= letters.length)
					b.putChar((char) 0);
				else
					b.putChar(letters[i]);
			}
			letters = var.getDescription().trim().toCharArray();
			for(int i = 0; i < 25; i++)
			{
				if(i >= letters.length)
					b.putChar((char) 0);
				else
					b.putChar(letters[i]);
			}
			int DOUBLE_BYTE_SIZE = 8;
			long StartingVariableLocation = HEADER_SIZE + (availableVars*VARIABLE_BINARY_SIZE) + (availableFiles*FILE_BINARY_SIZE) + (usedVars*DOUBLE_BYTE_SIZE);
			b.putLong(StartingVariableLocation);

			b.flip();
			try
			{
				worker.addRequest(offset, b.array());
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
				System.err.println("Error Adding Variable");
			}

			this.variableInfo.put(var, StartingVariableLocation);

			this.usedVars++;

			return true;
		}

		return false;
		
		
	}

	private void grow() {
		//wait for all pending writes to complete before continuing. 
		while(worker.writeRequestQueue.size() > 0)
		{
			try
			{
			Thread.sleep(250);
			}
			catch(InterruptedException e){}
		}
		//First, determine what has to grow. Useful for when more files need to be added, but no more variables
		boolean growFiles = false;
		boolean growVars = false;
		float f = usedVars/availableVars;
		//determine what needs to grow
		if(f >= LOAD)
			growVars = true;
		f = usedFiles/availableFiles;
		if(f >= LOAD)
			growFiles = true;		
		//create a temporary binary
		File tempFile = new File(binary.getParentFile().getAbsolutePath()+"\\temp.bin");
		try
		{
			tempFile.createNewFile();
			RandomAccessFile tempBin = new RandomAccessFile(tempFile,"rwd");
			//create a new worker to reduce time spend writing.
			BinaryWorker tempWorker = new BinaryWorker(tempBin);
			new Thread(tempWorker).start();
			//start by writing the header
			int oldVars = availableVars;
			int oldFiles = availableFiles;
			writeNewHeader(growVars,growFiles, tempBin, tempWorker);
			//now calculate the new offset to use for this file
			long oldOffset = offset;	//used later for accessing old file.
			offset = (8*availableVars); //the new offset is calculated by the number of bytes required for each file.
			
			//allocate empty variable record
			byte[] block = new byte[availableVars*VARIABLE_BINARY_SIZE];
			tempBin.write(block);
			//allocate empty file record
			block = new byte[availableFiles * FILE_BINARY_SIZE];
			tempBin.write(block);
			
			//copy all of the old variable and file records over to the new file
			HashMap<Variable, Long> tempMap = copyOldRecords(oldVars, tempWorker);
			
			//copy all of the old data.
			CopyData(oldOffset, oldVars, oldFiles, tempWorker, tempMap);
			
			//close off everything, replace, open up new threads and whatnot
			ChangePrimaryFile(tempBin, tempWorker, tempFile, tempMap);
			
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.err.println("something went wrong with the grow method");
		}
	}
	
	/**
	 * Helper method, switches the primary binary and worker thread over to a temporary file. 
	 * The temporary file replaces the old one. NOTE: THIS DELETES THE OLD FILE.
	 * @param tempBin -- The temporary binary to switch to.
	 * @param tempWorker -- The worker thread for the temporary binary
	 * @param tempFile -- the file that the binary and thread are connected to. 
	 * @exception IOException -- An IOException can occur if there is an issue with growing the 
	 * files when there is access to them.
	 */
	private void ChangePrimaryFile(RandomAccessFile tempBin, BinaryWorker tempWorker, File tempFile, HashMap<Variable,Long> tempMap) {
		try {
			while(tempWorker.writeRequestQueue.size() > 0)
			{
				try
				{
					Thread.sleep(250);
					
				}
				catch(InterruptedException e)
				{}
			}
			this.bin.close();
			tempBin.close();
			this.worker.Close();
			tempWorker.Close();
			binary.delete();
			tempFile.renameTo(binary);
			//open up new stuff and start it up.
			bin = new RandomAccessFile(binary, "rwd");
			worker = new BinaryWorker(bin);
			new Thread(worker).start();
			//update the map
			this.variableInfo = tempMap;
		} catch (IOException e) 
		{
			e.printStackTrace();
			System.err.println("Error Growing binary file, issue with switching files.");
		}
		
		
	}

	/**
	 * private helper method that will copy all of the data in given binary to a new file
	 * @throws IOException 
	 */
	private void CopyData(long oldOffset, int oldVars, int oldFiles, BinaryWorker tempWorker, HashMap<Variable,Long> tempMap) throws IOException 
	{
		//go through the current variable map and the temp map, place variables in the correct place. 
		for(int i = 0; i < usedFiles; i++)
		{
			//use same algorithm for adding file data.	
			ByteBuffer b = ByteBuffer.allocate(8*this.usedVars);
				for(Variable v: variableInfo.keySet())
				{
					Long firstInstance = tempMap.get(v);
					//proceed with gathering the data and placing it within the new file.
					double d = getData(v, i);
					//build new file binary "block."
					int index = (int) (firstInstance - HEADER_SIZE - 
							(VARIABLE_BINARY_SIZE*availableVars) - (FILE_BINARY_SIZE*availableFiles));
					b.putDouble(index, d);
				}
				b.flip();
				long fileLocation = HEADER_SIZE + (VARIABLE_BINARY_SIZE * availableVars)
						+ (FILE_BINARY_SIZE * availableFiles) + (i * offset);
				tempWorker.addRequest(fileLocation, b.array());
		}
	}

	/**
	 * Private helper method used by the file growth method. Copies the 
	 * header information from the old file, and moves it to the new file. 
	 * @param oldVars -- The old number of available variables from the original variables.
	 * @param tempWorker -- A binary worker thread set to write to the temporary file. BE SURE
	 * TO SET TO WRITE FILES TO THE CORRECT BINARY.
	 */
	private HashMap<Variable,Long> copyOldRecords(int oldVars, BinaryWorker tempWorker)
	{
		//copy over all existing variables, give a new starting point.
		HashMap<Variable,Long> tempMap = copyVariables(tempWorker);

		//copy over file record
		copyFiles(tempWorker, oldVars);
		
		return tempMap;
	}
	
	
	/**
	 * Helper method,Copies over the file table from the current binary file in this object to a given file.
	 * @param tempWorker -- The binary worker thread that will write to the temporary file.
	 * @param oldVariables -- (used for seeking) the number of variables available to this binary
	 * file.
	 */
	private void copyFiles(BinaryWorker tempWorker, int oldVars)
	{
		try
		{
			this.bin.seek(HEADER_SIZE + (oldVars*VARIABLE_BINARY_SIZE));	//start of the old file table
			for(int i = 0; i < usedFiles; i++)
			{
				ByteBuffer buff = ByteBuffer.allocate(FILE_BINARY_SIZE);
				buff.putLong(bin.readLong());	//modified date
				for(int j = 0; j < 25; j++)
				{
					//copy all chars used for the name over
					buff.putChar(bin.readChar());
				}
				buff.putInt(bin.readInt());	//copy the data location int
				buff.flip();
				tempWorker.addRequest(HEADER_SIZE + (availableVars*VARIABLE_BINARY_SIZE)
						+ (i*FILE_BINARY_SIZE), buff.array());
			}
		}
		catch(IOException e)
		{
			System.err.println("Error reading file table in binary");
			e.printStackTrace();
			return;
		}
	}
	
	/**
	 * Copies of the variables from the current binary file in this to a given temporary file.
	 * @param tempWorker -- A Binary worker to write the table to the file. (Assumes headers will
	 * be of the same size.)
	 */
	private HashMap<Variable,Long> copyVariables(BinaryWorker tempWorker) 
	{
		try {
			bin.seek(HEADER_SIZE);	//seek the start of the variable table.
			HashMap<Variable, Long> tempMap = new HashMap<Variable, Long>();
			for(int i = 0; i < usedVars; i++)
			{
				ByteBuffer buff = ByteBuffer.allocate(VARIABLE_BINARY_SIZE);
				//read the variable from the old file and buffer it(construct a variable along the way.)
				int PDG = bin.readInt();
				int PDG_2 = bin.readInt();
				int PDG_3 = bin.readInt();
				buff.putInt(PDG);
				buff.putInt(PDG_2);
				buff.putInt(PDG_3);
				StringBuilder blockName = new StringBuilder();
				for(int j = 0; j < 20; j++)
				{
					char c = bin.readChar();
					buff.putChar(c);
			    	if(c == 0)
			    		continue;
					blockName.append(c);
				}
				String block = blockName.toString().trim();
				StringBuilder description = new StringBuilder();
				for(int j = 0; j < 25; j++)
				{
					char c = bin.readChar();
					buff.putChar(c);
					if(c == 0)
						continue;
					description.append(c);
				}
				String desc = description.toString().trim();
				bin.readLong();
				//create the variable from the given info
				Variable var = new Variable(PDG + "", PDG_2 + "", PDG_3 + "", null, desc,block);
				int DOUBLE_BYTE_SIZE = 8;
				long StartingVariableLocation = HEADER_SIZE + (availableVars*VARIABLE_BINARY_SIZE) 
						+ (availableFiles*FILE_BINARY_SIZE) + (i*DOUBLE_BYTE_SIZE);	//calc new starting location.
				buff.putLong(StartingVariableLocation);
				buff.flip();
				tempWorker.addRequest(HEADER_SIZE + (i*VARIABLE_BINARY_SIZE),buff.array());
				//place in temporary map (for data copy.)
				tempMap.put(var, StartingVariableLocation);
			}
			return tempMap;
		} 
		catch (IOException e1) 
		{
			e1.printStackTrace();
			System.err.println("Error seeking binary VariableTable in temp file, copyOldRecords method");
			return null;
		}
		
	}

	
	/**
	 * Adds the listener to the list of classes to be notified when
	 * a write event completes. Will call upon the WriteComplete method. 
	 * @param listener -- The listener to notified.
	 */
	public void addBinaryListener(IBinaryListener listener)
	{
		listeners.add(listener);
	}
	

	/**
	 * Private Helper method, creates a new header at the start of the given file.
	 * If the boolean values either growVars or growFiles is set to true, the header
	 * will indicate a 25% increase in the available space for those tables within the
	 * binary
	 * @param growVars -- Used to indicate if there will be an increase in space within the
	 * binary file for more variables. THIS WILL UPDATE THIS OBJECTS AVAILABLE VARIABLES VARIABLE.
	 * @param growFiles -- Used to indicate if there will be an increase in space within the binary
	 * file for more files. THIS WILL UPDATE THIS OBJECTS AVAILABLE FILE VARIABLE
	 * @param binaryFile -- The binary file to have the header written to. This method is normally
	 * used for growing files. The temporary file for growth would be placed here.
	 * @param tempWorker -- A binaryWorker thread to help speed up the writing process. BE SURE
	 * TO USE THE CORRECT WRITER THAT WILL WRITE TO THE CORRECT FILE.
	 */
	private void writeNewHeader(boolean growVars, boolean growFiles, RandomAccessFile binaryFile, BinaryWorker tempWorker)
	{
		//create a new header and write it to the beginning of the file.
		try {
			ByteBuffer b = ByteBuffer.allocate(HEADER_SIZE);
			
			if(growVars)
			{
				availableVars = (int)(availableVars * 1.25);
				b.putInt(availableVars);
				b.putInt(usedVars);
			}
			else
			{
				b.putInt(availableVars);
				b.putInt(usedVars);
			}
			if(growFiles)
			{
				availableFiles = (int)(availableFiles*2);
				b.putInt(availableFiles);
				b.putInt(usedFiles);
			}
			else
			{
				b.putInt(availableFiles);
				b.putInt(usedFiles);
			}
			b.flip();
			tempWorker.addRequest(0, b.array());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Exception thrown in the writeNewHeaderMethod. Cause Unknown");
		}
	}
	
	/**
	 * returns whether or not this file already exists within 
	 * this binary file.
	 * @param f -- the file to be checked. Files are stored by their
	 * names, so a matching name will return true.
	 * @return -- True if the file is contained within this binary already, 
	 * false otherwise.
	 */
	public boolean CotainsFile(File f)
	{
		//strip out the .txt if it exists
		String name = f.getName();
		int index = name.lastIndexOf(".");
		if(index > 0)
			name = name.substring(0,index);
		FileInfo i = this.fileInfo.get(name);
		return i != null;
	}
	
	
	public double getData(Variable var, int FileNumber)
	{
		long location = variableInfo.get(var);
		try {
			bin.seek(location+(offset*FileNumber));
			return bin.readDouble();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error accessing Binary File");
		}
		return Double.MAX_VALUE;
	}
	
	public int getNumberOfInconsistencies()
	{
		return this.loggedInconsistencies;
	}
	
	public int getMaximumFiles()
	{
		return availableFiles;
	}
	
	public int getMaximumVariables()
	{
		return availableVars;
	}
	
	public int getUsedFiles()
	{
		return usedFiles;
	}
	
	public int getUsedVariables()
	{
		return usedVars;
	}

	public Iterable<Variable> getAllVariables()
	{
		LinkedList<Variable> rtList = new LinkedList<Variable>();
		for(Variable v: variableInfo.keySet())
			rtList.addLast(v);
		return rtList;
	}
	
	private class BinaryWorker implements Runnable
	{
		//private members for the class
		private RandomAccessFile bin;
		private LinkedList<Request> writeRequestQueue;
		private boolean writing;
		private Object async = new Object();
		private int totalRequests;
		private int completedRequests;
		
		public BinaryWorker(RandomAccessFile bin)
		{
			totalRequests = 0;
			completedRequests = 0;
			this.bin = bin;
			writing = false;
			writeRequestQueue = new LinkedList<Request>();
		}

		public void run() {
			writing = true;
			while(writing)
			{
				synchronized(async)
				{
					//process writes one at a time, if there are no pending writes, we wait and check again later
					if(writeRequestQueue.size() > 0)
					{
						Request r = writeRequestQueue.removeFirst();
						try
						{
						bin.seek(r.location);
						bin.write(r.data);
						
						}
						catch(IOException e)
						{
							System.err.println("Error Writing Infomration to Binary File");
						}
						completedRequests++;
						notifyListeners(completedRequests, totalRequests);
					}
					else
					{
						try
						{
							Thread.sleep(500);
						}
						catch(InterruptedException e){}
					}
				}
			}
		}
		
		
		
		/**
		 * notifies all registered listeners to a binary write event being completed.
		 */
		private void notifyListeners(int progress, int completed)
		{
			for(IBinaryListener b: listeners)
			{
				b.completedWrites(progress, completed);
			}
		}
		
		public void addRequest(long location, byte[] data)
		{
			synchronized(this)
			{
				totalRequests++;
				writeRequestQueue.addLast(new Request(location,data));
			}
		}
		
		public void Close()
		{
			this.writing = false;
		}
	}
	
	private class Request
	{
		public long location;
		public byte[] data;
		public Request(long location, byte[] data)
		{
			this.location = location;
			this.data  = data;
		}
	}
	
	private class FileInfo
	{
		public long lastModified;
		public int fileLocation;
		public FileInfo(long lastModified, int fileLocation)
		{
			this.lastModified = lastModified;
			this.fileLocation = fileLocation;
		}
	}

	public void closeInconsistencyLogger() {
		if(out != null)
			out.close();
	}
}
