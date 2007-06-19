/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;
import ij.io.*;
import ij.gui.YesNoCancelDialog;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Arrays;

class NeuriteTracerResultsDialog
	extends Dialog
	implements ActionListener, WindowListener, ItemListener {

	// These are the states that the UI can be in:

	static final int WAITING_TO_START_PATH = 0;
	static final int PARTIAL_PATH          = 1;
	static final int SEARCHING             = 2;
	static final int QUERY_KEEP            = 3;
	static final int LOGGING_POINTS        = 4;
	static final int DISPLAY_EVS           = 5;
	static final int FILLING_PATHS         = 6;
	static final int CALCULATING_GAUSSIAN  = 7;

	static final String SEARCHING_STRING = "Searching for path between points...";

	private int currentState;

	SimpleNeuriteTracer_ plugin;

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

	Checkbox preprocess;
	Checkbox showEVs;

	List pathList;

	Button deletePaths;
	Button fillPaths;
	Button fitCircles;

	Label fillStatus;

	float maxThresholdValue = 0;
	
	TextField thresholdField;
	Label maxThreshold;
	Button setThreshold;
	Button setMaxThreshold;

	boolean currentlyExploring = true;
	Button pauseOrRestartExploring;

	Button saveFill;
	Button discardFill;

	Button view3D;
	Checkbox maskNotReal;
	Checkbox transparent;

	Button saveButton;
	Button loadButton;   
	Button uploadButton;
	Button fetchButton;

	Button quitButton;

	// ------------------------------------------------------------------------

	int preGaussianState;

	public void gaussianCalculated() {
		changeState(preGaussianState);
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
		pathActionPanel.setEnabled(false);
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
		pauseOrRestartExploring.setEnabled(false);
		saveFill.setEnabled(false);
		discardFill.setEnabled(false);

		view3D.setEnabled(false);
		maskNotReal.setEnabled(false);
		transparent.setEnabled(false);

		saveButton.setEnabled(false);
		loadButton.setEnabled(false);
		uploadButton.setEnabled(false);
		fetchButton.setEnabled(false);

		quitButton.setEnabled(false);
	}

	public void changeState( int newState ) {

		switch( newState ) {

		case WAITING_TO_START_PATH:
			statusText.setText("Click somewhere to start a new path...");
			disableEverything();

			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			completePath.setEnabled(true);
			cancelPath.setEnabled(true);

			viewPathChoice.setEnabled(true);
			preprocess.setEnabled(true);

			pathList.setEnabled(true);
			deletePaths.setEnabled(true);
			fillPaths.setEnabled(true);
			fitCircles.setEnabled(true);
			fillStatus.setEnabled(true);
			
			saveButton.setEnabled(true);
			loadButton.setEnabled(true);
			uploadButton.setEnabled(true);
			fetchButton.setEnabled(true);

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
			pauseOrRestartExploring.setEnabled(true);
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

	boolean launchedByArchive;
	
	public NeuriteTracerResultsDialog( String title,
					   SimpleNeuriteTracer_ plugin,
					   boolean launchedByArchive ) {
		
		super( IJ.getInstance(), title, false );
		this.plugin = plugin;
		this.launchedByArchive = launchedByArchive;

		addWindowListener(this);

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 8, 8, 8, 8 );

		{ /* Add the status panel */

			statusPanel = new Panel();
			statusPanel.setLayout(new BorderLayout());
			statusPanel.add(new Label("Instructions:"), BorderLayout.NORTH);
			statusText = new TextArea("Initial status text...",3,20,TextArea.SCROLLBARS_NONE);
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
			
			preprocess = new Checkbox("Hessian-based analysis (early version)");
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
			otherOptionsPanel.add(preprocess,co);

			c.gridx = 0;
			c.gridy = 2;
			add(otherOptionsPanel,c);
		}

		{ /* Add the panel with the path list. */

			Panel pathListPanel = new Panel();
			pathListPanel.setLayout(new BorderLayout());
			pathList = new List();
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
				buttonsForListPanel.add(fitCircles,cl);
				pathListPanel.add(buttonsForListPanel,BorderLayout.SOUTH);				
			}

			c.gridx = 0;
			c.gridy = 3;
			add(pathListPanel,c);
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
			
			view3D = new Button("View with 3D Viewer");
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

			pauseOrRestartExploring = new Button("Pause Filling");
			currentlyExploring = true;
			pauseOrRestartExploring.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 6;
			cf.gridwidth = 3;
			cf.fill = GridBagConstraints.HORIZONTAL;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(pauseOrRestartExploring,cf);	
		
			saveFill = new Button("Save Fill");
			saveFill.addActionListener(this);
			cf.gridx = 0;
			cf.gridy = 7;
			cf.gridwidth = 2;
			cf.fill = GridBagConstraints.NONE;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(saveFill,cf);
			
			discardFill = new Button("Cancel Fill");
			discardFill.addActionListener(this);
			cf.gridx = 2;
			cf.gridy = 7;
			cf.gridwidth = 1;
			cf.anchor = GridBagConstraints.LINE_START;
			fillingOptionsPanel.add(discardFill,cf);

			c.gridx = 0;
			c.gridy = 4;
			c.gridwidth = 1;
			add(fillingOptionsPanel,c);
		}

		{ /* The panel with options for saving, loading, network storage, etc. */

			Panel traceFileOptionsPanel = new Panel();
			
			traceFileOptionsPanel.setLayout(new GridBagLayout());
			
			GridBagConstraints ct = new GridBagConstraints();
			
			uploadButton = new Button("Upload Traces");
			uploadButton.addActionListener( this );
			fetchButton = new Button("Fetch Traces");
			fetchButton.addActionListener( this );
			ct.gridx = 0;
			ct.gridy = 0;
			traceFileOptionsPanel.add( uploadButton, ct );
			ct.gridx = 1;
			ct.gridy = 0;
			traceFileOptionsPanel.add( fetchButton, ct );
			
			saveButton = new Button("Save");
			saveButton.addActionListener( this );
			loadButton = new Button("Load");
			loadButton.addActionListener( this );
			ct.gridx = 0;
			ct.gridy = 1;
			traceFileOptionsPanel.add( saveButton, ct );
			ct.gridx = 1;
			ct.gridy = 1;
			traceFileOptionsPanel.add( loadButton, ct );
			
			c.gridx = 0;
			c.gridy = 5;
			add(traceFileOptionsPanel,c);

		}

		/* Just add the quit button at the bottom... */

		quitButton = new Button("Quit Tracer");
		quitButton.addActionListener(this);
		c.gridx = 0;
		c.gridy = 6;
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
						    "image.traces",
						    ".traces");

			} else {
				
				String fileName = info.fileName;
				String directory = info.directory;
				
				String suggestedSaveFilename;
				
				suggestedSaveFilename = fileName+".traces";
				
				sd = new SaveDialog("Save traces as...",
						    directory,
						    suggestedSaveFilename,
						    ".traces");
			}
			
			String savePath;
			if(sd.getFileName()==null) {
				// System.out.println("no savePath found");
				return;
			} else {
				savePath = sd.getDirectory()+sd.getFileName();
				// System.out.println("found savePath: "+savePath);
			}
			
			File file = new File(savePath);
			if ((file!=null)&&file.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Save traces file...", "The file "+
					    savePath+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}
			
			IJ.showStatus("Saving label annotations to "+savePath);
			
			plugin.writeTracesToFile( savePath );
			
		} else if( source == loadButton ) {
			
			if( plugin.pathsUnsaved() ) {						
				YesNoCancelDialog d = new YesNoCancelDialog( IJ.getInstance(), "Warning",
									     "There are unsaved paths. Do you really want to load new traces?" );
				
				if( ! d.yesPressed() )
					return;
			}
			
			plugin.loadTracings();			

		} else if( source == cancelSearch ) {
			
			statusText.setText("Cancelling...");
			plugin.cancelSearch();

		} else if( source == keepSegment ) {
			
		       plugin.confirmTemporary( );

		} else if( source == junkSegment ) {

			plugin.cancelTemporary( );

		} else if( source == completePath ) {

			plugin.finishedPath( );

		} else if( source == cancelPath ) {

			plugin.cancelPath( );
			
		} else if( source == deletePaths ) {
			
			// System.out.println("deletePaths called");
			int [] selectedIndices = pathList.getSelectedIndexes();
			
			Arrays.sort(selectedIndices);

			for( int i = selectedIndices.length - 1; i >= 0; --i ) {
				System.out.println("removing path index: "+i);
				plugin.removePath(selectedIndices[i]);
				pathList.remove(i);
			}

		}  else if( source == fillPaths ) {
			
			currentlyExploring = true;
			pauseOrRestartExploring.setLabel("Pause Filling");
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

		} else if( source == pauseOrRestartExploring ) {

			if( currentlyExploring ) {
				currentlyExploring = false;
				pauseOrRestartExploring.setLabel("Continue Filling");
			} else {
				currentlyExploring = true;
				pauseOrRestartExploring.setLabel("Pause Filling");
			}

			plugin.pauseOrRestartExploring();

		} else if( source == view3D ) {

			plugin.viewFillIn3D();
			
		} else if( source == fitCircles ) {

			int [] indices = pathList.getSelectedIndexes();
			if( indices.length != 1 ) {
				IJ.error("You must have exactly one path selected in order to show the normal panes for it.");
				return;
			}

			plugin.fitCircles(indices[0],true,40);

		}

	}

	public void thresholdChanged( double f ) {
		thresholdField.setText(""+f);
	}

	public void itemStateChanged( ItemEvent e ) {

		Object source = e.getSource();
		
		if( source == viewPathChoice ) {

			System.out.println("e.getItem() is of class "+e.getItem().getClass());

			if( ((String)e.getItem()).equals( projectionChoice )) {
				plugin.justDisplayNearSlices(false);
			} else if( ((String)e.getItem()).equals( partsNearbyChoice )) {
				plugin.justDisplayNearSlices(true);
			}


		} else if( source == pathList ) {

			// Show in green the selected ones....
			int [] selectedIndices = pathList.getSelectedIndexes();			
			plugin.showPaths(selectedIndices);

		} else if( source == transparent ) {

			plugin.setFillTransparent( transparent.getState() );


		} else if( source == preprocess ) {
			
			System.out.println("change in status of preprocess to: "+preprocess.getState());

			if( preprocess.getState() ) {
				preGaussianState = currentState;
				synchronized (plugin) {
					if( plugin.hessian == null )
						preprocess.setEnabled(false);
					plugin.enableHessian(true);
				}
				changeState(CALCULATING_GAUSSIAN);
			} else {
				plugin.enableHessian(false);
				changeState(preGaussianState);
			}

		}

	}
	
        public void paint(Graphics g) {
                super.paint(g);
        }

	public void addPathToList( String nameForList ) {
	
		synchronized(pathList) {
			pathList.add(nameForList);
		}

	}

	public void setMaxDistanceExplored( float f ) {
		maxThreshold.setText("("+f+")");
		maxThresholdValue = f;
	}

}
