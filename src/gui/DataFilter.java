package gui;

import java.io.File;

import javax.swing.filechooser.FileFilter;

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
public class DataFilter extends FileFilter {

	@Override
	public boolean accept(File f) {
		if(f.isDirectory())
			return true;
		String extension = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		 if (i > 0 &&  i < s.length() - 1) {
	            extension = s.substring(i+1).toLowerCase();
	        }
		 if(extension == null)
			 return false;
		 if(extension.equalsIgnoreCase("dat"))
			 return true;
		 else
			 return false;
	}

	@Override
	public String getDescription() {
		
		return ".dat Files";
	}

}
