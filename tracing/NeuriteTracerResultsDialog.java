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
import ij.gui.YesNoCancelDialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import java.util.HashSet;

class NeuriteTracerResultsDialog
	extends Dialog
	implements ActionListener, WindowListener, ItemListener, PathAndFillListener, TextListener, FillerProgressCallback {

	static final boolean verbose = Simple_Neurite_Tracer.verbose;

	PathWindow pw;

	// These are the states that the UI can be in:

	static final int WAITING_TO_START_PATH = 0;
	static final int PARTIAL_PATH          = 1;
	static final int SEARCHING             = 2;
	static final int QUERY_KEEP            = 3;
	static final int LOGGING_POINTS        = 4;
	static final int DISPLAY_EVS           = 5;
	static final int FILLING_PATHS         = 6;
	static final int CALCULATING_GAUSSIAN  = 7;

	static final String [] stateNames = { "WAITING_TO_START_PATH",
					      "PARTIAL_PATH",
					      "SEARCHING",
					      "QUERY_KEEP",
					      "LOGGING_POINTS",
					      "DISPLAY_EVS",
					      "FILLING_PATHS",
					      "CALCULATING_GAUSSIAN" };

	static final String SEARCHING_STRING = "Searching for path between points...";

	private int currentState;

	Simple_Neurite_Tracer plugin;

	Panel statusPanel;
	TextArea statusText;
	Button keepSegment, junkSegment;
	Button cancelSearch;

	Panel pathActionPanel;
	Button completePath;
	Button cancelPath;

	Choice viewPathChoice;
	String projectionChoice = "projected through all slices";
	String partsNearbyChoice = "parts in nearby slices";

	TextField nearbyField;

	Checkbox preprocess;
	Checkbox showEVs;

	List pathList;

	Button deletePaths;
	Button fillPaths;
	Button fitCircles;

	List fillList;
	Button deleteFills;
	Button reloadFill;

	Panel fillControlPanel;

	Label fillStatus;

	float maxThresholdValue = 0;

	TextField thresholdField;
	Label maxThreshold;
	Button setThreshold;
	Button setMaxThreshold;

	boolean currentlyFilling = true;
	Button pauseOrRestartFilling;

	Button saveFill;
	Button discardFill;

	Button view3D;
	Checkbox maskNotReal;
	Checkbox transparent;

	Button loadLabelsButton;

	Button saveButton;
	Button loadButton;
	Button uploadButton;
	Button fetchButton;

	Button quitButton;

	Button showOrHidePathList;

	public void setPathList( String [] newList, Path justAdded ) {
		pathList.removeAll();
		for( int i = 0; i < newList.length; ++i )
			pathList.add( newList[i] );
	}

	public void setFillList( String [] newList ) {
		fillList.removeAll();
		for( int i = 0; i < newList.length; ++i )
			fillList.add( newList[i] );
	}

	public void setSelectedPaths( HashSet selectedPathsSet, Object source ) {
		if( source == this )
			return;
		int items = pathList.getItemCount();
		boolean [] itemStates = new boolean[items];
		for( int i = 0; i < pathAndFillManager.size(); ++i ) {
			Path p = pathAndFillManager.getPath(i);
			if( selectedPathsSet.contains(p) )
				itemStates[i] = true;
		}
		for( int i = 0; i < items; ++i ) {
			if( itemStates[i] ) {
				if( ! pathList.isIndexSelected(i) )
					pathList.select(i);
			} else {
				if( pathList.isIndexSelected(i) )
					pathList.deselect(i);
			}
		}
		plugin.repaintAllPanes();
	}

	// ------------------------------------------------------------------------

	int preGaussianState;

	public void gaussianCalculated(boolean succeeded) {
		if( !succeeded )
			preprocess.setState(false);
		changeState(preGaussianState);
	}

	public void setPreprocessLabelSigma( double sigma ) {
		String basic = "Hessian-based analysis";
		if( sigma < 0 )
			preprocess.setLabel(basic);
		else
			preprocess.setLabel(basic+" (\u03C3="+sigma+")");
	}

	public void exitRequested() {

		// FIXME: check that everything is saved...

		if( plugin.pathsUnsaved() ) {

			YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Really quit?",
								     "There are unsaved paths. Do you really want to quit?" );

			if( ! d.yesPressed() )
				return;

		}

		plugin.cancelSearch();
		// What if we're filling?
		dispose();
		plugin.closeAndReset();
	}

	public void disableEverything() {
		statusText.setEnabled(false);
		keepSegment.setEnabled(false);
		junkSegment.setEnabled(false);
		cancelSearch.setEnabled(false);
		completePath.setEnabled(false);
		cancelPath.setEnabled(false);

		viewPathChoice.setEnabled(false);
		preprocess.setEnabled(false);

		pathList.setEnabled(false);
		deletePaths.setEnabled(false);
		fillPaths.setEnabled(false);
		fitCircles.setEnabled(false);
		fillStatus.setEnabled(false);

		thresholdField.setEnabled(false);
		maxThreshold.setEnabled(false);
		setThreshold.setEnabled(false);
		setMaxThreshold.setEnabled(false);
		pauseOrRestartFilling.setEnabled(false);
		saveFill.setEnabled(false);
		discardFill.setEnabled(false);

		fillList.setEnabled(false);
		deleteFills.setEnabled(false);
		reloadFill.setEnabled(false);

		view3D.setEnabled(false);
		maskNotReal.setEnabled(false);
		transparent.setEnabled(false);

		saveButton.setEnabled(false);
		loadButton.setEnabled(false);
		if( uploadButton != null ) {
			uploadButton.setEnabled(false);
			fetchButton.setEnabled(false);
		}
		loadLabelsButton.setEnabled(false);

		quitButton.setEnabled(false);
	}

	public void changeState( int newState ) {

		if (verbose) System.out.println("changeState to: "+stateNames[newState]);

		switch( newState ) {

		case WAITING_TO_START_PATH:
			statusText.setText("Click somewhere to start a new path...");
			disableEverything();

			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			viewPathChoice.setEnabled(true);
			preprocess.setEnabled(true);

			pathList.setEnabled(true);
			deletePaths.setEnabled(true);

			fillList.setEnabled(true);
			deleteFills.setEnabled(true);
			reloadFill.setEnabled(true);

			fillPaths.setEnabled(true);
			fitCircles.setEnabled(true);
			fillStatus.setEnabled(true);

			loadLabelsButton.setEnabled(true);

			saveButton.setEnabled(true);
			loadButton.setEnabled(true);
			if( uploadButton != null ) {
				uploadButton.setEnabled(true);
				fetchButton.setEnabled(true);
			}

			quitButton.setEnabled(true);

			break;

		case PARTIAL_PATH:
			statusText.setText("Now select a point further along that structure...");
			disableEverything();

			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			if( plugin.justFirstPoint() )
				completePath.setEnabled(false);
			else
				completePath.setEnabled(true);
			cancelPath.setEnabled(true);

			viewPathChoice.setEnabled(true);
			preprocess.setEnabled(true);

			pathList.setEnabled(true);

			quitButton.setEnabled(false);

			break;

		case SEARCHING:
			statusText.setText("Searching for path between points...");
			disableEverything();

			cancelSearch.setLabel("Abandon search");
			cancelSearch.setEnabled(true);
			cancelSearch.setVisible(true);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			completePath.setEnabled(false);
			cancelPath.setEnabled(false);

			quitButton.setEnabled(true);

			break;

		case QUERY_KEEP:
			statusText.setText("Keep this new path segment?");
			disableEverything();

			keepSegment.setEnabled(true);
			junkSegment.setEnabled(true);

			cancelSearch.setVisible(false);
			keepSegment.setVisible(true);
			junkSegment.setVisible(true);

			break;

		case FILLING_PATHS:
			statusText.setText("Filling out from neuron...");
			disableEverything();

			thresholdField.setEnabled(true);
			maxThreshold.setEnabled(true);
			setThreshold.setEnabled(true);
			setMaxThreshold.setEnabled(true);
			pauseOrRestartFilling.setEnabled(true);
			saveFill.setEnabled(true);
			discardFill.setEnabled(true);

			view3D.setEnabled(true);
			maskNotReal.setEnabled(true);
			transparent.setEnabled(true);

			break;

		case CALCULATING_GAUSSIAN:
			statusText.setText("Calculating Gaussian...");
			disableEverything();

			cancelSearch.setLabel("Cancel");
			cancelSearch.setEnabled(true);
			cancelSearch.setVisible(true);
			keepSegment.setVisible(true);
			junkSegment.setVisible(true);

			break;

		default:
			IJ.error("BUG: switching to an unknown state");
			return;
		}

		pack();

		int [] selectedIndices = pathList.getSelectedIndexes();
		HashSet<Path> selectedPaths = new HashSet<Path>();
		for( int i = 0; i < selectedIndices.length; ++i ) {
			int selectedIndex = selectedIndices[i];
			Path p = pathAndFillManager.getPath(selectedIndex);
			selectedPaths.add(p);
		}
		pathAndFillManager.setSelected( selectedPaths.toArray(new Path[]{}), this );
		plugin.repaintAllPanes();

		currentState = newState;

	}

	// ------------------------------------------------------------------------

	public void windowClosing( WindowEvent e ) {
		exitRequested();
	}

	public void windowActivated( WindowEvent e ) { }
	public void windowDeactivated( WindowEvent e ) { }
	public void windowClosed( WindowEvent e ) { }
	public void windowOpened( WindowEvent e ) { }
	public void windowIconified( WindowEvent e ) { }
	public void windowDeiconified( WindowEvent e ) { }

	public void updateSearchingStatistics( int pointsInOpenBoundary ) {
		statusText.setText( SEARCHING_STRING + " ("+pointsInOpenBoundary+" boundary points.)" );
	}

	private PathAndFillManager pathAndFillManager;

	boolean launchedByArchive;

	public NeuriteTracerResultsDialog( String title,
					   Simple_Neurite_Tracer plugin,
					   boolean launchedByArchive ) {

		super( IJ.getInstance(), title, false );
		this.plugin = plugin;
		this.launchedByArchive = launchedByArchive;

		pathAndFillManager = plugin.getPathAndFillManager();
		pw = new PathWindow(pathAndFillManager);

		addWindowListener(this);

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 3, 3, 3, 3 );

		{ /* Add the status panel */

			statusPanel = new Panel();
			statusPanel.setLayout(new BorderLayout());
			statusPanel.add(new Label("Instructions:"), BorderLayout.NORTH);
			statusText = new TextArea("Initial status text...",2,25,TextArea.SCROLLBARS_NONE);
			statusPanel.add(statusText,BorderLayout.CENTER);

			keepSegment = new Button("Yes");
			junkSegment = new Button("No");
			cancelSearch = new Button("Abandon Search");

			keepSegment.addActionListener( this );
			junkSegment.addActionListener( this );
			cancelSearch.addActionListener( this );

			Panel statusChoicesPanel = new Panel();
			statusChoicesPanel.setLayout( new GridBagLayout() );
			GridBagConstraints cs = new GridBagConstraints();
			cs.gridx = 0; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(keepSegment,cs);
			cs.gridx = 1; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(junkSegment,cs);
			cs.gridx = 2; cs.gridy = 0; cs.anchor = GridBagConstraints.LINE_START;
			statusChoicesPanel.add(cancelSearch,cs);

			statusPanel.add(statusChoicesPanel,BorderLayout.SOUTH);

			c.gridx = 0;
			c.gridy = 0;
			add(statusPanel,c);
		}

		{ /* Add the panel of actions to take on half-constructed paths */

			pathActionPanel = new Panel();
			completePath = new Button("Complete Path");
			cancelPath = new Button("Cancel Path");
			completePath.addActionListener( this );
			cancelPath.addActionListener( this );
			pathActionPanel.add(completePath);
			pathActionPanel.add(cancelPath);

			c.gridx = 0;
			c.gridy = 1;
			add(pathActionPanel,c);
		}

		{ /* Add the panel with other options - preprocessing and the view of paths */

			Panel otherOptionsPanel = new Panel();

			otherOptionsPanel.setLayout(new GridBagLayout());
			GridBagConstraints co = new GridBagConstraints();
			co.anchor = GridBagConstraints.LINE_START;
			viewPathChoice = new Choice();
			viewPathChoice.addItem(projectionChoice);
			viewPathChoice.addItem(partsNearbyChoice);
			viewPathChoice.addItemListener( this );

			Panel nearbyPanel = new Panel();
			nearbyPanel.setLayout(new BorderLayout());
			nearbyPanel.add(new Label("(up to"),BorderLayout.WEST);
			nearbyField = new TextField("2",2);
			nearbyField.addTextListener(this);
			nearbyPanel.add(nearbyField,BorderLayout.CENTER);
			nearbyPanel.add(new Label("slices to each side)"),BorderLayout.EAST);

			preprocess = new Checkbox();
			setPreprocessLabelSigma(-1);
			preprocess.addItemListener( this );
			showEVs = new Checkbox("Just show eigenvectors / eigenvalues");

			co.gridx = 0;
			co.gridy = 0;
			otherOptionsPanel.add(new Label("View paths: "),co);
			co.gridx = 1;
			co.gridy = 0;
			otherOptionsPanel.add(viewPathChoice,co);

			co.gridx = 0;
			co.gridy = 1;
			co.gridwidth = 2;
			co.anchor = GridBagConstraints.LINE_END;
			otherOptionsPanel.add(nearbyPanel,co);

			co.gridx = 0;
			co.gridy = 2;
			co.gridwidth = 2;
			co.anchor = GridBagConstraints.LINE_START;
			otherOptionsPanel.add(preprocess,co);

			c.gridx = 0;
			c.gridy = 2;
			add(otherOptionsPanel,c);
		}

		showOrHidePathList = new Button("Show / Hide Path List");
		add( showOrHidePathList );
		showOrHidePathList.addActionListener(this);

		{ /* Add the panel with the path list. */

			Panel pathListPanel = new Panel();
			pathListPanel.setLayout(new BorderLayout());
			pathList = new List(3);
			pathList.setMultipleMode(true);
			pathList.addItemListener(this);
			pathListPanel.add(pathList,BorderLayout.CENTER);

			{ /* The sub-panel with buttons for the path list... */

				Panel buttonsForListPanel = new Panel();
				buttonsForListPanel.setLayout(new GridBagLayout());
				GridBagConstraints cl = new GridBagConstraints();
				deletePaths = new Button("Delete Path(s)");
				deletePaths.addActionListener( this );
				buttonsForListPanel.add(deletePaths,cl);

				cl.gridx = 1;
				fillPaths = new Button("Fill Out Path(s)");
				fillPaths.addActionListener( this );
				buttonsForListPanel.add(fillPaths,cl);

				cl.gridx = 0;
				cl.gridy = 1;
				fitCircles = new Button("Fit Centres and Circles");
				fitCircles.addActionListener( this );
				/* FIXME: put this back when the smoothing works...
				   buttonsForListPanel.add(fitCircles,cl);
				*/
				pathListPanel.add(buttonsForListPanel,BorderLayout.SOUTH);
			}

			c.gridx = 0;
			c.gridy = 3;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(pathListPanel,c);
		}

		{
			c.insets = new Insets( 8, 8, 1, 8 );
			fillList = new List(3);
			c.gridx = 0;
			c.gridy = 4;
			c.fill = GridBagConstraints.HORIZONTAL;
			add(fillList,c);

			Panel fillListCommandsPanel = new Panel();
			fillListCommandsPanel.setLayout(new BorderLayout());

			deleteFills = new Button("Delete Fill(s)");
			deleteFills.addActionListener( this );
			fillListCommandsPanel.add(deleteFills,BorderLayout.WEST);

			reloadFill = new Button("Reload Fill");
			reloadFill.addActionListener( this );
			fillListCommandsPanel.add(reloadFill,BorderLayout.CENTER);

			c.insets = new Insets( 1, 8, 8, 8 );
			c.gridx = 0;
			c.gridy = 5;

			add(fillListCommandsPanel,c);
		}

		{ /* The panel with options for filling out neurons... */

			Panel fillingOptionsPanel = new Panel();

			fillingOptionsPanel.setLayout(new GridBagLayout());

			GridBagConstraints cf = new GridBagConstraints();

			cf.gridx = 0;
			cf.gridy = 0;
			cf.gridwidth = 4;
			cf.weightx = 1;
			cf.anchor = GridBagConstraints.LINE_START;
			cf.fill = GridBagConstraints.HORIZONTAL;
			fillStatus = new Label("(Not filling at the moment.)");
			fillingOptionsPanel.add(fillStatus,cf);

			thresholdField = new TextField("",10);
			thresholdField.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 1;
			cf.weightx = 0;
			cf.gridwidth = 2;
			cf.fill = GridBagConstraints.NONE;
			fillingOptionsPanel.add(thresholdField,cf);

			maxThreshold = new Label("(0)                  ",Label.LEFT);
			cf.gridx = 2;
			cf.gridy = 1;
			cf.gridwidth = 1;
			cf.fill = GridBagConstraints.HORIZONTAL;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(maxThreshold,cf);

			setThreshold = new Button("Set");
			setThreshold.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 2;
			cf.gridwidth = 1;
			cf.fill = GridBagConstraints.NONE;
			fillingOptionsPanel.add(setThreshold,cf);

			setMaxThreshold = new Button("Set Max");
			setMaxThreshold.addActionListener(this);
			cf.gridx = 1;
			cf.gridy = 2;
			fillingOptionsPanel.add(setMaxThreshold,cf);

			view3D = new Button("Create Image Stack from Fill");
			view3D.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 3;
			cf.gridwidth = 2;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(view3D,cf);

			maskNotReal = new Checkbox("Create as Mask");
			maskNotReal.addItemListener(this);
			cf.gridx = 0;
			cf.gridy = 4;
			cf.gridwidth = 3;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(maskNotReal,cf);

			transparent = new Checkbox("Transparent fill display (slow!)");
			transparent.addItemListener(this);
			cf.gridx = 0;
			cf.gridy = 5;
			cf.gridwidth = 3;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(transparent,cf);

			{
				fillControlPanel = new Panel();
				fillControlPanel.setLayout(new BorderLayout());

				pauseOrRestartFilling = new Button("Pause");
				currentlyFilling = true;
				pauseOrRestartFilling.addActionListener(this);
				fillControlPanel.add(pauseOrRestartFilling,BorderLayout.WEST);

				saveFill = new Button("Save Fill");
				saveFill.addActionListener(this);
				fillControlPanel.add(saveFill,BorderLayout.CENTER);

				discardFill = new Button("Cancel Fill");
				discardFill.addActionListener(this);
				fillControlPanel.add(discardFill,BorderLayout.EAST);

				cf.gridx = 0;
				cf.gridy = 6;
				cf.gridwidth = 3;
				cf.fill = GridBagConstraints.HORIZONTAL;
				cf.anchor = GridBagConstraints.LINE_START;

				fillingOptionsPanel.add(fillControlPanel,cf);
			}

			c.gridx = 0;
			c.gridy = 6;
			c.insets = new Insets( 8, 8, 8, 8 );
			add(fillingOptionsPanel,c);
		}

		{ /* The panel with options for saving, loading, network storage, etc. */

			Panel traceFileOptionsPanel = new Panel();

			traceFileOptionsPanel.setLayout(new GridBagLayout());

			GridBagConstraints ct = new GridBagConstraints();

			ct.gridy = 0;

			if( false ) {

				uploadButton = new Button("Upload Traces");
				uploadButton.addActionListener( this );
				fetchButton = new Button("Fetch Traces");
				fetchButton.addActionListener( this );
				ct.gridx = 0;
				traceFileOptionsPanel.add( uploadButton, ct );
				ct.gridx = 1;
				traceFileOptionsPanel.add( fetchButton, ct );

				++ ct.gridy;

			}

			c.gridx = 0;
			c.gridy = 7;
			c.anchor = GridBagConstraints.CENTER;
			c.fill = GridBagConstraints.NONE;

			loadLabelsButton = new Button("Load Labels");
			loadLabelsButton.addActionListener( this );
			add(loadLabelsButton,c);

			saveButton = new Button("Save Traces File");
			saveButton.addActionListener( this );
			loadButton = new Button("Load Traces File");
			loadButton.addActionListener( this );
			ct.gridx = 0;
			ct.gridy = 1;
			traceFileOptionsPanel.add( saveButton, ct );
			ct.gridx = 1;
			ct.gridy = 1;
			traceFileOptionsPanel.add( loadButton, ct );

			c.gridx = 0;
			c.gridy = 8;
			add(traceFileOptionsPanel,c);

		}

		/* Just add the quit button at the bottom... */

		quitButton = new Button("Quit Tracer");
		quitButton.addActionListener(this);
		c.gridx = 0;
		c.gridy = 9;
		c.anchor = GridBagConstraints.CENTER;
		add(quitButton,c);

		changeState( WAITING_TO_START_PATH );

		pack();
		setVisible( true );

	}

	public void showMouseThreshold( float t ) {
		if( t < 0 ) {
			fillStatus.setText( "Not reached by search yet" );
		} else {
			fillStatus.setText( "Distance from path is: " + t );
		}
	}

	public boolean createMask() {
		return maskNotReal.getState();
	}

	public void actionPerformed( ActionEvent e ) {

		Object source = e.getSource();

		if( source == uploadButton ) {
			plugin.uploadTracings();
		} else if( source == fetchButton ) {
			plugin.getTracings( true );
		} else if( source == saveButton ) {

			FileInfo info = plugin.file_info;
			SaveDialog sd;

			if( info == null ) {

				sd = new SaveDialog("Save traces as...",
						    "image",
						    ".traces");

			} else {

				String fileName = info.fileName;
				String directory = info.directory;

				String suggestedSaveFilename;

				suggestedSaveFilename = fileName;

				sd = new SaveDialog("Save traces as...",
						    directory,
						    suggestedSaveFilename,
						    ".traces");
			}

			String savePath;
			if(sd.getFileName()==null) {
				return;
			} else {
				savePath = sd.getDirectory()+sd.getFileName();
			}

			File file = new File(savePath);
			if ((file!=null)&&file.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Save traces file...", "The file "+
					    savePath+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}

			IJ.showStatus("Saving traces to "+savePath);

			try {

				pathAndFillManager.writeXML( savePath, plugin, true );

			} catch( IOException ioe ) {
				IJ.error("Writing traces to '"+savePath+"' failed: "+ioe);
				return;
			}

			plugin.unsavedPaths = false;

		} else if( source == loadButton ) {

			if( plugin.pathsUnsaved() ) {
				YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Warning",
									     "There are unsaved paths. Do you really want to load new traces?" );

				if( ! d.yesPressed() )
					return;
			}

			plugin.loadTracings();

		} else if( source == loadLabelsButton ) {

			plugin.loadLabels();

		} else if( source == cancelSearch ) {

			if( currentState == SEARCHING ) {
				statusText.setText("Cancelling path search...");
				plugin.cancelSearch();
			} else if( currentState == CALCULATING_GAUSSIAN ) {
				statusText.setText("Cancelling Gaussian generation...");
				plugin.cancelGaussian();
			} else {
				IJ.error("BUG! (wrong state for cancelling...)");
			}

		} else if( source == keepSegment ) {

			plugin.confirmTemporary( );

		} else if( source == junkSegment ) {

			plugin.cancelTemporary( );

		} else if( source == completePath ) {

			plugin.finishedPath( );

		} else if( source == cancelPath ) {

			plugin.cancelPath( );

		} else if( source == deletePaths ) {

			// if (verbose) System.out.println("deletePaths called");
			int [] selectedIndices = pathList.getSelectedIndexes();
			if( selectedIndices.length < 1 ) {
				IJ.error("No path was selected for deletion");
				return;
			}
			pathAndFillManager.deletePaths( selectedIndices );
			plugin.repaintAllPanes();

		} else if( source == deleteFills ) {

			if (verbose) System.out.println("deleteFills called");
			int [] selectedIndices = fillList.getSelectedIndexes();
			if( selectedIndices.length < 1 ) {
				IJ.error("No fill was selected for deletion");
				return;
			}
			pathAndFillManager.deleteFills( selectedIndices );
			plugin.repaintAllPanes();

		} else if( source == reloadFill ) {

			int [] selectedIndices = fillList.getSelectedIndexes();
			if( selectedIndices.length != 1 ) {
				IJ.error("You must have a single fill selected in order to reload.");
				return;
			}
			pathAndFillManager.reloadFill(selectedIndices[0]);

		}  else if( source == fillPaths ) {

			int [] selectedIndices = pathList.getSelectedIndexes();

			if( selectedIndices.length < 1 ) {
				IJ.error("You must have one or more paths in the list selected");
				return;
			}

			currentlyFilling = true;
			pauseOrRestartFilling.setLabel("Pause");
			plugin.startFillingPaths();

		} else if( source == quitButton ) {

			exitRequested();

		} else if( source == setMaxThreshold ) {

			plugin.setFillThreshold( maxThresholdValue );

		} else if( source == setThreshold ) {

			try {
				double t = Double.parseDouble( thresholdField.getText() );
				if( t < 0 ) {
					IJ.error("The fill threshold cannot be negative.");
					return;
				}
				plugin.setFillThreshold( t );
			} catch( NumberFormatException nfe ) {
				IJ.error("The threshold '" + thresholdField.getText() + "' wasn't a valid number.");
				return;
			}

		} else if( source == discardFill ) {

			plugin.discardFill();

		} else if( source == saveFill ) {

			plugin.saveFill();

		} else if( source == pauseOrRestartFilling ) {

			plugin.pauseOrRestartFilling();

		} else if( source == view3D ) {

			plugin.viewFillIn3D();

		} else if( source == fitCircles ) {

			int [] indices = pathList.getSelectedIndexes();
			if( indices.length != 1 ) {
				IJ.error("You must have exactly one path selected in order to show the normal panes for it.");
				return;
			}

			plugin.fitCircles(indices[0],true,40);

		}  else if( source == showOrHidePathList ) {
			pw.setVisible(!pw.isVisible());
			System.out.println("created PathWindow: "+pw);

		}

	}

	public void thresholdChanged( double f ) {
		thresholdField.setText(""+f);
	}

	public boolean nearbySlices( ) {
		return ( viewPathChoice.getSelectedIndex() > 0 );
	}

	public void itemStateChanged( ItemEvent e ) {

		Object source = e.getSource();

		if( source == viewPathChoice ) {

			plugin.justDisplayNearSlices(nearbySlices(),getEitherSide());

		} else if( source == pathList ) {

			// Show in green the selected ones....
			int [] selectedIndices = pathList.getSelectedIndexes();
			HashSet<Path> selectedPaths = new HashSet<Path>();
			for( int i = 0; i < selectedIndices.length; ++i ) {
				int selectedIndex = selectedIndices[i];
				Path p = pathAndFillManager.getPath(selectedIndex);
				selectedPaths.add(p);
			}
			pathAndFillManager.setSelected( selectedPaths.toArray(new Path[]{}), this );
			plugin.repaintAllPanes();

		} else if( source == transparent ) {

			plugin.setFillTransparent( transparent.getState() );


		} else if( source == preprocess ) {

			if( preprocess.getState() ) {
				preGaussianState = currentState;
				plugin.enableHessian(true);
			} else {
				plugin.enableHessian(false);
				changeState(preGaussianState);
			}

		}

	}

	public void paint(Graphics g) {
		super.paint(g);
	}

	boolean reportedInvalid;

	public int getEitherSide( ) {

		String s = nearbyField.getText();
		if( s.equals("") ) {
			reportedInvalid = false;
			return 0;
		}

		try {
			int e = Integer.parseInt( s );
			if( e < 0 ) {
				if( ! reportedInvalid ) {
					IJ.error("The number of slices either side cannot be negative.");
					reportedInvalid = true;
					return 0;
				}
			}
			reportedInvalid = false;
			return e;

		} catch( NumberFormatException nfe ) {
			if( ! reportedInvalid ) {
				IJ.error("The number of slices either side must be a non-negative integer.");
				reportedInvalid = true;
				return 0;
			}
			return 0;
		}

	}

	public void textValueChanged( TextEvent e ) {
		plugin.justDisplayNearSlices(nearbySlices(),getEitherSide());
	}


	public void threadStatus( SearchThread source, int threadStatus ) {
		if (verbose) System.out.println("threadStatus reported as: "+threadStatus);
		switch(threadStatus) {
		case FillerThread.STOPPING:
			pauseOrRestartFilling.setLabel("Stopping..");
			pauseOrRestartFilling.setEnabled(false);
			break;
		case FillerThread.PAUSED:
			pauseOrRestartFilling.setLabel("Continue");
			break;
		case FillerThread.RUNNING:
			pauseOrRestartFilling.setLabel("Pause");
			break;
		}
		fillControlPanel.doLayout();
	}

	public void maximumDistanceCompletelyExplored( SearchThread source, float f ) {
		maxThreshold.setText("("+f+")");
		maxThresholdValue = f;
	}

	public void finished( SearchThread source, boolean success ) {
		// Unused
	}

	public void pointsInSearch( SearchThread source, int inOpen, int inClosed ) {
		// Unused
	}
}
