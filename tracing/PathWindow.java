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

public class PathWindow extends JFrame implements PathAndFillListener {

	public static class PathTreeNode extends DefaultMutableTreeNode {

	}

	JTree tree;
	DefaultMutableTreeNode root;

	JPanel buttonPanel;

	JButton renameButton;
	JButton fillOutButton;
	JButton makePrimaryButton;

	PathAndFillManager pathAndFillManager;

	public PathWindow(PathAndFillManager pathAndFillManager) {

		this.pathAndFillManager = pathAndFillManager;
		
		setBounds(60,60,400,300);
		root = new DefaultMutableTreeNode("All Paths");
		tree = new JTree(root);
		add(tree, BorderLayout.CENTER);

		buttonPanel = new JPanel();

		renameButton = new JButton("Rename");
		fillOutButton = new JButton("Fill Out");
		makePrimaryButton = new JButton("Make Primary");

		buttonPanel.add(renameButton);
		buttonPanel.add(fillOutButton);
		buttonPanel.add(makePrimaryButton);

		add(buttonPanel, BorderLayout.PAGE_END);

	}

	HashMap<Path,DefaultMutableTreeNode> pathToNode;

	void setPathList( String [] pathList ) {
		/* Ignore the arguments and get the real path list
		   from the PathAndFillManager:

                   Go through the whole list and pick candidate
                   primary neurons - these are those with no "starts
                   on" defined and for which no "ends on" is defined.
                   In some cases a neuron might be marked as primary


		 
		*/
	}
	
	void setFillList( String [] fillList ) {
		
	}

	void setSelectedPaths( int [] selectedIndices );

}
