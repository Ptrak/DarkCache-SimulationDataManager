package gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTree;
import javax.swing.TransferHandler;
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
public class SlhaTransferHandler extends TransferHandler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private JList<String> list;
	private DefaultListModel<String> model;
	private JTree sourceTree;
	
	
	public  SlhaTransferHandler(JList<String> list, JTree sourceTree)
	{
		this.sourceTree = sourceTree;
		this.list = list;
		model = new DefaultListModel<String>();
	}
	
	public boolean canImport(TransferHandler.TransferSupport info) {
        // we only import Strings
        if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
        	System.err.println("nope");
            return false;
        }

        JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
        if (dl.getIndex() == -1) {
            return false;
        }
        return true;
    }

    public boolean importData(TransferHandler.TransferSupport info) {
    	if (!info.isDrop()) {
    		return false;
    	}

    	// Check for String flavor
    	if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
    		System.out.println("nope");
    		return false;
    	}

    	JList.DropLocation dl = (JList.DropLocation)info.getDropLocation();
    	// Get the string that is being dropped.
    	Transferable t = info.getTransferable();
    	String data;
    	try {
    		data = (String)t.getTransferData(DataFlavor.stringFlavor);
    	} 
    	catch (Exception e) { System.out.println("failed");return false; }

    	// add to the list based on where the information is dropped, and if the information is valid.
    	data = buildString(data, dl.getIndex());
    	if (data == null)
    	{
    		list.setModel(model);
    		list.validate();
    		return false;
    	}

    	if (dl.isInsert())
    	{
    		placeString(dl.getIndex(),data);
    	} 
    	else 
    	{
    		//dropped on top, default to doing nothing.
    	}
    	list.setModel(model);
    	list.validate();
    	return false;
    }
    
    private void placeString (int index, String toBeAdded)
    {
    	if (index == 0) 
		{
			//begging of list
			if(!model.contains(toBeAdded))
				model.add(0, toBeAdded);
			else
			{
				model.remove(model.indexOf(toBeAdded));
				model.add(0, toBeAdded);
			}
		}
		else if (index >= list.getModel().getSize())
		{
			//end of list
			if(!model.contains(toBeAdded))
				model.add(model.size(), toBeAdded);
			else
			{
				model.remove(model.indexOf(toBeAdded));
				model.add(model.size(), toBeAdded);
			}
		} 
		else 
		{
			//between elements
			if(!model.contains(toBeAdded))
				model.add(index, toBeAdded);
			else
			{
				model.remove(model.indexOf(toBeAdded));
				model.add(index, toBeAdded);
			}
		}
    }
    
    public int getSourceActions(JComponent c) {
        return COPY;
    }
    
    /**
     * handles whether or not the string already exists within the list and builds it appropriately. 
     * @param data
     * @return
     */
    private String buildString(String data, int index)
    {
    	//ensures that the given data is not duplicated.
    	if(data.contains("BLOCK:") && data.contains("Description:"))
    		return data;
    	else if(data.equalsIgnoreCase("Variables"))
    	{
    		Object rootNode = sourceTree.getModel().getRoot();
    		int parents = sourceTree.getModel().getChildCount(rootNode);
    		for(int i = 0; i < parents; i++)
    		{

    			//we have the correct node add all of the children
    			Object parent = sourceTree.getModel().getChild(rootNode,i);
    			int children = sourceTree.getModel().getChildCount(parent);
    			for(int j = 0; j < children; j++)
    			{
    				placeString(j+index,sourceTree.getModel().getChild(parent,j).toString());
    			}
    		}
    		return null;
    	}
    	else
    	{
    		//we have selected a parent. get all of the children of this node.
    		Object rootNode = sourceTree.getModel().getRoot();
    		int parents = sourceTree.getModel().getChildCount(rootNode);
    		for(int i = 0; i < parents; i++)
    		{
    			if(sourceTree.getModel().getChild(rootNode,i).toString().equalsIgnoreCase(data))
    					{
    						//we have the correct node add all of the children
    						Object parent = sourceTree.getModel().getChild(rootNode,i);
    						int children = sourceTree.getModel().getChildCount(parent);
    						for(int j = 0; j < children; j++)
    						{
    							placeString(j+index,sourceTree.getModel().getChild(parent,j).toString());
    						}
    						return null;
    					}
    		}
    		return null;
    	}
    }
    
    protected Transferable createTransferable(JComponent c) {
        @SuppressWarnings("rawtypes")
		JList list = (JList)c;
        @SuppressWarnings("deprecation")
		Object[] values = list.getSelectedValues();

        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < values.length; i++) {
            Object val = values[i];
            buff.append(val == null ? "" : val.toString());
            if (i != values.length - 1) {
                buff.append("\n");
            }
        }
        return new StringSelection(buff.toString());
    }

	public void removeListSelection() {
		int index = this.list.getSelectedIndex();
		if(index >= 0)
			model.removeElementAt(index);
		
	}
	
	public void setTree(JTree sourceTree)
	{
		this.sourceTree = sourceTree;
	}

	public void clearList() {
		for(int i = model.getSize() - 1; i >= 0; i--)
			model.remove(i);
		
	}

    
    
}
