package gui;


import guiControl.ParserProgramController;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;

import binaryUtil.SlhaFile;
import binaryUtil.Variable;

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
 * The view portion of the application. Contains all visible components along
 * with the logic to drive the drag and drop functionality.
 * @author Patrick
 *
 */
@SuppressWarnings("serial")
public class ProgramView extends JFrame {

	//read-only members
	private ParserProgramController controller;
	private JTree tree;
	private JList<String> selectedVars;
	
	private JPanel leftPane;
	private JPanel rightPane;
	private SlhaTransferHandler handler;
	
	//for the status panel
	private JLabel selectedVariable;
	private JLabel completedWrites;
	private JLabel parsedFiles;

	//constants for button identification outside of class.
	public final String EXPORT_BUTTON = "Export Data";
	public final String REMOVE_BUTTON = "Remove Selection";
	public final String MENU_OPEN = "Open";
	public final String MENU_CLOSE = "Close";

	/**
	 * Initializes the view portion of this application. The view comprises all of
	 * the visible components of the app, along with the logic supporting drag and drop 
	 * operations.
	 * @param defaultController -- a controller to invoke changes in the view. 
	 */
	public ProgramView(ParserProgramController defaultController)
	{
		super("Parsing Program");

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		controller = defaultController;

		setLookAndFeel();

		buildMenuBar();

		setMinimumSize(new Dimension(400,400));
		setPreferredSize(new Dimension(600,600));

		buildGui();

		trySetFrameLocation();
		
		setVisible(true);
		
		pack();
	}

	/**
	 * Builds the main elements to populate this frame
	 */
	private void buildGui() {

		JPanel mainPanel = new JPanel(new BorderLayout());
		//create two buttons and add them to the top
		JPanel top = new JPanel();
		top.add(createButton(EXPORT_BUTTON));
		top.add(createButton(REMOVE_BUTTON));
		
		mainPanel.add(top,BorderLayout.NORTH);
		
		//add the split pane with a JTree on the left, and a JList on the right
		leftPane = new JPanel(new GridLayout(1,1));
		leftPane.setBorder(BorderFactory.createTitledBorder("Available Variables"));
		
		
		rightPane = new JPanel(new GridLayout(1,1));
		rightPane.setBorder(BorderFactory.createTitledBorder("SelectedVariables"));
		selectedVars = new JList<String>();
		JScrollPane scroll = new JScrollPane(selectedVars);
		rightPane.add(scroll);
		
		JSplitPane pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,leftPane,rightPane);
		pane.setDividerLocation(250);
		mainPanel.add(pane,BorderLayout.CENTER);
		
		//add the transfer handler
		handler = new SlhaTransferHandler(selectedVars, tree);
		selectedVars.setTransferHandler(handler);
		selectedVars.setDropMode(DropMode.INSERT);
		selectedVars.setDragEnabled(true);
		
		//Status bar initialization.
		JPanel statusPanel = new JPanel();
		selectedVariable = new JLabel("Select a Variable to See Details");
		completedWrites = new JLabel("");
		parsedFiles = new JLabel("");
		statusPanel.add(selectedVariable);
		statusPanel.add(completedWrites);
		statusPanel.add(parsedFiles);
		mainPanel.add(statusPanel,BorderLayout.SOUTH);
		
		add(mainPanel);
}

	/**
	 * creates a button and adds this as the controller.
	 * @param name -- The name of the button and the text to be used on the button
	 * @return -- The created button.
	 */
	private JButton createButton(String name) {
	
		JButton b = new JButton(name);
		b.setName(name);
		b.addActionListener(this.controller);
		return b;

	}

	/** 
	 * Sets the theme of this program to match the native machine (as opposed to java's default look.)
	 */
	private void setLookAndFeel()
	{
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e) {
			return;
		}
	}

	/**
	 * Builds the menu bar at the top of this, and adds it to this frame. The menu bar 
	 * allows the user to select a data set from the local machine, and give it to the 
	 * binary reader to parse and handle.
	 */
	private void buildMenuBar()
	{
		//create main bar
		JMenuBar menuBar = new JMenuBar();
		//add various menus
		JMenu FileMenu = new JMenu("File");
		//add various actions
		JMenuItem openAction = new JMenuItem(MENU_OPEN);
		JMenuItem closeAction = new JMenuItem(MENU_CLOSE);
		openAction.setName(MENU_OPEN);
		closeAction.setName(MENU_CLOSE);
		FileMenu.add(openAction);
		FileMenu.add(closeAction);
		//add the menu to the main bar
		menuBar.add(FileMenu);
		//add to this
		setJMenuBar(menuBar);
		openAction.addActionListener(controller);
		closeAction.addActionListener(controller);
	}

	/**
	 * Attempts to move the frame to the center of the screen. if it fails, the frame pops up
	 * at the top left corner (0,0).
	 */
	private void trySetFrameLocation()
	{
		try
		{
			Dimension ScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
			setLocation((ScreenSize.width/2) - 200, (ScreenSize.height/2) - 250);
		}
		catch(Exception e)
		{
			//we default to setting the frame in the top left corner
		}
	}

	/**
	 * returns the jTree being displayed in the view.
	 */
	public JTree getTree()
	{
		return tree;
	}
	
	/**
	 * returns the JList being displayed in the view.
	 */
	public JList<String> getList()
	{
		return selectedVars;
	}
	
	/**
	 * Prompts the user to select a directory, or a group of files.
	 * @return -- returns the chooser based on how the user responds.
	 */
	public void promptUser()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setMultiSelectionEnabled(true);
		int selection = chooser.showOpenDialog(this);
		if(selection == JFileChooser.APPROVE_OPTION)
			controller.passChooser(chooser);
		//call on controller to use the given chooser
		
	}
	
	public void saveFile()
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new DataFilter());
		int selection = chooser.showSaveDialog(this);
		if(selection == JFileChooser.CANCEL_OPTION)
			return;
		controller.passSaveChooser(chooser);
	}
	
	/**
	 * Given a slhaFile object, this method will build a tree with the blocks divided up
	 * and the individual variables per block listed as children under the block.
	 * @param s
	 */
	public void updateAvailableVariables(SlhaFile s)
	{
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Variables");
		tree = new JTree(top);
		handler.setTree(tree);
		//this.tree.addTreeSelectionListener(this);
		DefaultMutableTreeNode category = null;
	    DefaultMutableTreeNode variable = null;
		 for(String str: s.getBlocks())
	       {
	    	   category = new DefaultMutableTreeNode(str.trim());
	    	   
	    	   top.add(category);
	    	   for(Variable v: s.getField(str).getVariables())
	    	   {
	    		   try
	    		   {
	    			   DefaultMutableTreeNode node = new DefaultMutableTreeNode(v);
	    			   variable = new DefaultMutableTreeNode(node);  				
	    			   category.add(variable);
	    		   }
	    		   catch(Exception e)
	    		   {
	    			   continue;
	    		   }
	    		  
	    	   }
	       }		
		 tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		//add to scrollable pane
		 JScrollPane Panel = new JScrollPane(tree);
		leftPane.add(Panel);
//		leftPanel.add(treeView,BorderLayout.CENTER);
		//enable drag and drop
		tree.setDragEnabled(true);
		//selectedVarsList.setDragEnabled(true);		
		this.validate();
	}
	
	/**
	 * The transfer handler used by this view to support drag and drop.
	 */
	public SlhaTransferHandler getTransferHandler()
	{
		return this.handler;
	}
	
	/**
	 * Updates the file parsing status of the status bar. That is the number of files being
	 * read and the number of completed files.
	 * @param completed -- The number of files that have been read
	 * @param total -- The total number of files that have been read.
	 */
	public void updateFileStatus(int completed, int total)
	{
		parsedFiles.setText("Reading Files: " + completed + " / " + total + " files completed.");
	}

	/**
	 * updates the file writing status of the status bar. that is the number of writes that
	 * have been completed when parsing all of the files.
	 * @param completed -- Total number of completed writes to the binary
	 * @param total -- overall total of writes to the binary queued up.
	 */
	public void updateWriteStatus(int completed, int total) {
		completedWrites.setText("Writing to Binary: " + completed + " / " + total + " writes completed.");
		
	}

	/**
	 * updates the selected variable in the status bar. This shows which variable in the tree or list has
	 * been selected.
	 * @param var -- The variable to be displayed.
	 */
	public void updateSelectedVariable(Variable var) {	
		String displayString = "";
		if(!var.getBlock().equalsIgnoreCase(""))
			displayString+= "BLOCK: " + var.getBlock() + " ";
		if(!var.getPDG().equalsIgnoreCase("0"))
			displayString += "Code: " + var.getPDG() + ", ";
		if(!var.getPDG2().equalsIgnoreCase("0"))
			displayString +=   var.getPDG2() + ", ";
		if(!var.getPDG3().equalsIgnoreCase("0"))
			displayString +=  var.getPDG3() + " ";
		if(!var.getBlock().equalsIgnoreCase(""))
			displayString += " BlockName: " + var.getBlock() + " ";
		if(!var.getDescription().equalsIgnoreCase(""))
			displayString += " Description: " + var.getDescription();
		selectedVariable.setText(displayString);
		
	}

	public void clear() {
		try
		{
		leftPane.remove(0);
		handler.clearList();
		}
		catch(ArrayIndexOutOfBoundsException e)
		{}
		
	}

}



