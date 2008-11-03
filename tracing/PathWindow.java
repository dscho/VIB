/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Simple Neurite Tracer".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package tracing;

import ij.*;
import ij.io.*;

import javax.swing.*;

import java.awt.BorderLayout;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeModel;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import java.util.HashSet;
import java.util.Iterator;

public class PathWindow extends JFrame implements PathAndFillListener, TreeSelectionListener {

	public static class HelpfulJTree extends JTree {

		public HelpfulJTree(TreeNode root) {
			super( root );
		}

		public boolean isExpanded( Object [] path ) {
			TreePath tp = new TreePath( path );
			return isExpanded( tp );
		}

		public void setExpanded( Object [] path, boolean expanded ) {
			TreePath tp = new TreePath( path );
			System.out.println("  setExpandedState ("+expanded+") for "+tp);
			setExpandedState( tp, expanded );
		}

		public void setSelected( Object [] path ) {
			TreePath tp = new TreePath( path );
			setSelectionPath( tp );
		}

	}

	public void valueChanged( TreeSelectionEvent e ) {
		System.out.println("Got TreeSelectionEvent: "+e);
		TreePath [] selectedPaths = tree.getSelectionPaths();
		Path [] paths = new Path[selectedPaths.length];
		for( int i = 0; i < selectedPaths.length; ++i ) {
			TreePath tp = selectedPaths[i];
			DefaultMutableTreeNode node =
				(DefaultMutableTreeNode)(tp.getLastPathComponent());
			paths[i] = (Path)node.getUserObject();
		}
		pathAndFillManager.setSelected(paths,this);
	}

	public static class PathTreeNode extends DefaultMutableTreeNode {
	}

	JScrollPane scrollPane;

	HelpfulJTree tree;
	DefaultMutableTreeNode root;

	JPanel buttonPanel;

	JButton renameButton;
	JButton fillOutButton;
	JButton makePrimaryButton;
	JButton deleteButton;

	PathAndFillManager pathAndFillManager;

	public PathWindow(PathAndFillManager pathAndFillManager) {
		super("All Paths");

		this.pathAndFillManager = pathAndFillManager;
		
		setBounds(60,60,400,300);
		root = new DefaultMutableTreeNode("All Paths");
		tree = new HelpfulJTree(root);
		// tree.setRootVisible(false);
		tree.addTreeSelectionListener(this);
		scrollPane = new JScrollPane();
		scrollPane.getViewport().add(tree);
		add(scrollPane, BorderLayout.CENTER);

		buttonPanel = new JPanel();

		renameButton = new JButton("Rename");
		fillOutButton = new JButton("Fill Out");
		makePrimaryButton = new JButton("Make Primary");
		deleteButton = new JButton("Delete");

		buttonPanel.add(renameButton);
		buttonPanel.add(fillOutButton);
		buttonPanel.add(makePrimaryButton);
		buttonPanel.add(deleteButton);

		add(buttonPanel, BorderLayout.PAGE_END);

	}

	void getExpandedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet set ) {
		int count = model.getChildCount(node);
		for( int i = 0; i < count;  i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild( node, i );
			Path p = (Path)child.getUserObject();
			if( tree.isExpanded( (Object[])(child.getPath()) ) ) {
				set.add(p);
			}
			if( ! model.isLeaf(child) )
				getExpandedPaths( tree, model, child, set );
		}
	}

	void setExpandedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet set, Path justAdded ) {
		int count = model.getChildCount(node);
		for( int i = 0; i < count;  i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild( node, i );
			Path p = (Path)child.getUserObject();
			if( set.contains(p) || ((justAdded != null) && (justAdded == p)) ) {
				System.out.println("---- Setting "+p+" to be expanded");
				tree.setExpanded( (Object[])(child.getPath()), true );
			} else {
				System.out.println("---- Not expanding: "+p);
			}
			if( ! model.isLeaf(child) )
				setExpandedPaths( tree, model, child, set, justAdded );
		}

	}
	
	public void setSelectedPaths( HashSet selectedPaths, Object source ) {
		if( source == this )
			return;
		TreePath [] noTreePaths = {};
		tree.setSelectionPaths( noTreePaths );
		setSelectedPaths( tree, tree.getModel(), root, selectedPaths );
	}

	void setSelectedPaths( HelpfulJTree tree, TreeModel model, MutableTreeNode node, HashSet set ) {
		int count = model.getChildCount(node);
		for( int i = 0; i < count;  i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) model.getChild( node, i );
			Path p = (Path)child.getUserObject();
			if( set.contains(p) ) {
				System.out.println("---- Setting "+p+" to be expanded");
				tree.setSelected( (Object[])(child.getPath()) );
			} else {
				System.out.println("---- Not expanding: "+p);
			}
			if( ! model.isLeaf(child) )
				setSelectedPaths( tree, model, child, set );
		}

	}

	public void setPathList( String [] pathList, Path justAdded ) {

		// Save the selection state:
		
		TreePath [] selectedBefore = tree.getSelectionPaths();
		HashSet selectedPathsBefore = new HashSet();
		HashSet expandedPathsBefore = new HashSet();

		if( selectedBefore != null )
			for( int i = 0; i < selectedBefore.length; ++i ) {
				TreePath tp = selectedBefore[i];
				System.out.println("=== TreePath is: "+tp);
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode)tp.getLastPathComponent();
				Path p = (Path)dmtn.getUserObject();
				selectedPathsBefore.add(p);
			}

		// Save the expanded state:
		getExpandedPaths( tree, tree.getModel(), root, expandedPathsBefore );

		/* Ignore the arguments and get the real path list
		   from the PathAndFillManager: */
		
		DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode("All Paths");
		DefaultTreeModel model = new DefaultTreeModel(newRoot);
		// DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
		Path [] primaryPaths = pathAndFillManager.getPathsStructured();
		System.out.println("Got primaryPaths of length: "+primaryPaths.length);
		for( int i = 0; i < primaryPaths.length; ++i ) {
			Path primaryPath = primaryPaths[i];
			addNode( newRoot, primaryPath, model );
			System.out.println("  added the path: "+primaryPath);
		}
		root = newRoot;
		tree.setModel(model);

		model.reload();

		// Set back the expanded state:
		setExpandedPaths( tree, model, root, expandedPathsBefore, justAdded );

		setSelectedPaths( tree, model, root, selectedPathsBefore );

	}

	public void addNode( MutableTreeNode parent, Path childPath, DefaultTreeModel model ) {
		MutableTreeNode newNode = new DefaultMutableTreeNode(childPath);
		System.out.println("Inserting node: "+newNode+" into parent "+parent);
		model.insertNodeInto(newNode, parent, parent.getChildCount());
		Iterator<Path> ci = childPath.children.iterator();
		while( ci.hasNext() ) {
			Path p = ci.next();
			System.out.println("Now adding for child path: "+p);
			addNode( newNode, p, model );
		}
	}

	public void setFillList( String [] fillList ) {

	}

	public void setSelectedPaths( int [] selectedIndices ) {

	}
}
