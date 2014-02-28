package guiControl;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.LinkedList;
import java.util.Scanner;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;


import binaryUtil.IBinaryListener;
import binaryUtil.ProgramModel;
import binaryUtil.SlhaFile;
import binaryUtil.Variable;



import gui.ProgramView;

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
 *
 *This class instantiates the view and model for the program.  
 */
public class ParserProgramController implements ActionListener, IBinaryListener, TreeSelectionListener  {

	private ProgramView view;
	private ProgramModel model;
	
	
	//used to track wheter or not writing has completed
	private int completedWrites;
	private int pendingWrites;
	//variables to be set by the view
	private JFileChooser chooser;

	
	/**
	 * The main starting point for the parser. The controller will instantiate a view
	 * and a model and begin connecting them together. all operations done in either
	 * the model or the view will pass through this class as per MVC architecture. 
	 */
	public ParserProgramController()
	{
		final ParserProgramController ctlr = this;
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				view = new ProgramView(ctlr);
			}
		});
		model = new ProgramModel(ctlr);
		
		completedWrites = 0;
		pendingWrites = 0;
		
	}

	/**
	 * Action listener to handle events.
	 */
	public void actionPerformed(ActionEvent e) {
		if(e.getSource() instanceof JMenuItem)
		{
			JMenuItem j = (JMenuItem) e.getSource();
			processJMenuItem(j.getName());
			return;
		}
		else if(e.getSource() instanceof JButton)
		{
			JButton b = (JButton) e.getSource();
			processButton(b.getName())	;
			return;
		}
		
		
	}
	
	/**
	 * Processes when a button is pushed on the screen
	 * @param name -- The name of the button to distinguish it from the others.
	 * This is how the logic is tracked for each button.
	 */
	private void processButton(String name)
	{
		if(name.equalsIgnoreCase(view.EXPORT_BUTTON))
		{
			PromptFileWrite();
			return;
		}
		else if(name.equalsIgnoreCase(view.REMOVE_BUTTON))
		{
			removeSelectedVariables();
		}
	}

	/**
	 * If the parser has completed the modifying the file then it will
	 * allow the user to select a directory to save a output data file.
	 */
	private void PromptFileWrite() {
		if(!model.hasFile())
		{
			System.out.println("no File selected");
			return;
		}
		if(completedWrites != pendingWrites)
		{
			//notify the user that writing is still in progress.
			JOptionPane.showMessageDialog(null, "Please wait for the program to complete reading in the files.", "Working...", 
					JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		//invoke the view method to save a file.
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				view.saveFile();
			}
		});
	}

	/**
	 * Helper method. Removes all selected variables from the JList being 
	 * used in the window.
	 */
	private void removeSelectedVariables() {
		//get the updated list from the transfer handler.
		int[] toBeRemoved = view.getList().getSelectedIndices();
		DefaultListModel<String> model = null;
		try
		{
			model = (DefaultListModel<String>)view.getList().getModel();
		}
		catch(ClassCastException e){return;}
		for(int i = 0; i < toBeRemoved.length; i++)
		{
			model.removeElementAt(view.getList().getSelectedIndices()[0]);
		}
	}

	/**
	 * processes the JMenuItemRequest
	 * @param j
	 */
	private void processJMenuItem(String name) {
		if(name.equalsIgnoreCase(view.MENU_OPEN))
			invokePromptUser();
		else if(name.equalsIgnoreCase(view.MENU_CLOSE))
		{
			model.Close();
			System.exit(0);
		}
	}
	
	/**
	 * invokes the prompt user method in the view to get the file for the user.
	 */
	private void invokePromptUser()
	{
		//currently my only known way to safely invoke gui methods.
		
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				view.promptUser();
			}
		});
		
	}
	
	/**
	 * Called upon by the view when the user selects the files they wish to work with.
	 * @param c -- a file chooser that contains a selected file or set of files.
	 */
	public void passChooser(JFileChooser c)
	{
		chooser = c;
		File[] f = chooser.getSelectedFiles();
		if(f == null || f.length < 1)
			return;
		
		//clear out the list and the tree
		view.clear();
		if(f[0].isDirectory() && f.length == 1)
		{
			model.ParseDirectory(f[0]);
		}
		else
		{
			model.parseFiles(f);
		}
	}
	
	/**
	 * Called by the model to notify the view to update the available variable list 
	 * using the given slha file
	 * @param f -- A slha file, whose variables will be used in the view for the user 
	 * to select.
	 */
	public void InvokeDisplayFileContents(SlhaFile f)
	{
		view.updateAvailableVariables(f);
		view.getTree().addTreeSelectionListener(this);	//add a listener to update the view on the selected variable
	}
	
	/**
	 * invoked by the model to notify the controller that progress
	 * has occurred in parsing files. 
	 */
	public void UpdateProgress(int progress, int total) {
		final int prog = progress;
		final int tot = total;
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				view.updateFileStatus(prog,tot);
			}
		});
	}

	public void completedWrites(int progress, int total) {
		completedWrites = progress;
		pendingWrites = total;
		final int prog = progress;
		final int tot = total;
		javax.swing.SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				view.updateWriteStatus(prog,tot);
			}
		});
		
	}

	public void valueChanged(TreeSelectionEvent arg0) {
		JTree tree = (JTree)arg0.getSource();
		TreePath path = tree.getSelectionPath();
		if(path == null)
			return;
		if(tree.getModel().isLeaf(path.getLastPathComponent()))
		{
			DefaultMutableTreeNode d = (DefaultMutableTreeNode) path.getLastPathComponent();
			DefaultMutableTreeNode d2 = (DefaultMutableTreeNode)d.getUserObject();
			final Variable var = (Variable)d2.getUserObject();
			javax.swing.SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					view.updateSelectedVariable(var);
				}
			});
		}
		
	}

	/**
	 * Called by the view to notify the controller that the user has selected a file to 
	 * save their data output to. 
	 * @param chooser
	 */
	public void passSaveChooser(JFileChooser chooser) {
		//add the .dat if necessary
		File f = chooser.getSelectedFile();
		if(f==null)
			return;
		String name = f.toString();
		if(!name.contains(".dat"))
		{
			File f2 = new File(name+".dat");
			f = f2;
		}
		//ensure the user does not want to overwrite their file
		if(f.exists())
		{
			int option = JOptionPane.showConfirmDialog(null, "Overwrite existing file?", "Overwrite file", JOptionPane.YES_NO_OPTION);
			if(option == JOptionPane.NO_OPTION)
				return;
		}
		//tell the model to write a file. 
		model.writeDataFile(f);
	}

	/**
	 * Returns a linked list of the currently selected variables from the view, in the order listed.
	 * The first variable will be the top, with the last on the bottom.
	 * @return -- a list of the selected variables in order listed in the view (first on top, last on bottom)
	 */
	public LinkedList<Variable> getSelectedVariables() {
		LinkedList<Variable> vars = new LinkedList<Variable>();
		JTree tree = view.getTree();
		JList<String> list = view.getList();
		TreeModel model = tree.getModel();
		for(int i = 0; i < list.getModel().getSize(); i++)
		{
			String selectedVar = list.getModel().getElementAt(i);
			Scanner s = new Scanner(selectedVar);
			//parse the info down.
			s.next();
			String block = s.next();
			while(s.hasNext())
			{
				String add = s.next();
				if(add.equalsIgnoreCase("Description:"))
					break;
				
				block+= " " + add;
			}
			String description = s.next();
			while(s.hasNext())
			{
				description += " " + s.next();
			}
			s.close();
			//get the index of the block in the tree
			int index = findChild(block, (DefaultMutableTreeNode)model.getRoot());
			int childIndex = findChild(description, (DefaultMutableTreeNode)model.getChild(model.getRoot(), index));
			//now knowing the index of the parent and the child, look up each variable and add it to the return list
			DefaultMutableTreeNode parent = (DefaultMutableTreeNode)model.getChild(model.getRoot(), index);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)model.getChild(parent, childIndex);
			node = (DefaultMutableTreeNode)node.getUserObject(); 		//User objects are two layers deep here
			vars.addLast((Variable) node.getUserObject());
		}
		return vars;
	}

	/**
	 * finds the index of the given child of the tree root. Helper method used by the getSelectedVariablesMethod.
	 * @return returns the index of string occurrence within the tree as a child given the parent. -1 if does not exist.
	 */
	public int findChild(String name, DefaultMutableTreeNode parent)
	{
		for(int i = 0; i < parent.getChildCount(); i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(i);
			if(child.getUserObject() instanceof DefaultMutableTreeNode)
			{
				child = (DefaultMutableTreeNode)child.getUserObject();
				Variable v = (Variable)child.getUserObject();
				if(v.getDescription().equalsIgnoreCase(name))
					return i;
			}
			if(child.toString().equalsIgnoreCase(name))
				return i;
		}
		return -1;
	}
}
