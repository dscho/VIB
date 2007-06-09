/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;
import ij.io.*;

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
	Checkbox justLog;
	Checkbox showEVs;

	List pathList;

	Button deletePath;
	Button createROI;

	Button saveButton;
	Button loadButton;   
	Button uploadButton;
	Button fetchButton;

	Button quitButton;

	// ------------------------------------------------------------------------

	public void changeState( int newState ) {

		switch( newState ) {

		case WAITING_TO_START_PATH:
			statusText.setText("Click somewhere to start a new path...");
			/*
			statusPanel.remove(cancelSearch);
			statusPanel.remove(keepSegment);
			statusPanel.remove(junkSegment);
			*/
			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			completePath.setEnabled(false);
			cancelPath.setEnabled(false);
			break;

		case PARTIAL_PATH:
			statusText.setText("Now select a point further along that structure...");
			/*
			statusPanel.remove(cancelSearch);
			statusPanel.remove(keepSegment);
			statusPanel.remove(junkSegment);
			*/

			cancelSearch.setVisible(false);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			if( plugin.justFirstPoint() )
				completePath.setEnabled(false);
			else
				completePath.setEnabled(true);
			cancelPath.setEnabled(true);
			break;

		case SEARCHING:
			statusText.setText("Searching for path between points...");
			/*
			statusPanel.remove(cancelSearch);
			statusPanel.remove(keepSegment);
			statusPanel.remove(junkSegment);
			statusPanel.add(cancelSearch,BorderLayout.SOUTH);
			*/

			cancelSearch.setVisible(true);
			keepSegment.setVisible(false);
			junkSegment.setVisible(false);

			completePath.setEnabled(false);
			cancelPath.setEnabled(false);
			break;

		case QUERY_KEEP:
			statusText.setText("Keep this new path segment?");
			/*
			statusPanel.remove(cancelSearch);
			statusPanel.remove(keepSegment);
			statusPanel.remove(junkSegment);
			statusPanel.add(keepSegment,BorderLayout.SOUTH);
			statusPanel.add(junkSegment,BorderLayout.SOUTH);
			*/

			cancelSearch.setVisible(false);
			keepSegment.setVisible(true);
			junkSegment.setVisible(true);

			completePath.setEnabled(false);
			cancelPath.setEnabled(false);
			break;

		default:
			IJ.error("BUG: trying to change to an unknown state");
			return;

		}

		pack();
		currentState = newState;

	}

	// ------------------------------------------------------------------------
	
	public void windowClosing( WindowEvent e ) {
		plugin.cancelled = true;
		dispose();
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

		setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();

		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets( 8, 8, 8, 8 );

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
		
		Panel otherOptionsPanel = new Panel();
	       
		otherOptionsPanel.setLayout(new GridBagLayout());
		GridBagConstraints co = new GridBagConstraints();
		co.anchor = GridBagConstraints.LINE_START;
		viewPathChoice = new Choice();
		viewPathChoice.addItem(projectionChoice);
		viewPathChoice.addItem(partsNearbyChoice);
		viewPathChoice.addItemListener( this );

		preprocess = new Checkbox("Hessian-based analysis");
		justLog = new Checkbox("Just log points");
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
		co.gridx = 0;
		co.gridy = 2;
		co.gridwidth = 2;
		otherOptionsPanel.add(justLog,co);

		c.gridx = 0;
		c.gridy = 2;
		add(otherOptionsPanel,c);

		Panel pathListPanel = new Panel();
		pathListPanel.setLayout(new BorderLayout());
		pathList = new List();
		pathList.setMultipleMode(true);
		pathListPanel.add(pathList,BorderLayout.CENTER);

		Panel buttonsForListPanel = new Panel();
		buttonsForListPanel.setLayout(new GridBagLayout());
		GridBagConstraints cl = new GridBagConstraints();
		deletePath = new Button("Delete Path");
		deletePath.addActionListener( this );
		buttonsForListPanel.add(deletePath,cl);
		cl.gridx = 1;
		createROI = new Button("Create ROI");
		createROI.addActionListener( this );
		buttonsForListPanel.add(createROI,cl);
		pathListPanel.add(buttonsForListPanel,BorderLayout.SOUTH);

		c.gridx = 0;
		c.gridy = 3;
		add(pathListPanel,c);

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
		c.gridy = 4;
		add(traceFileOptionsPanel,c);

		quitButton = new Button("Quit Tracer");
		c.gridx = 0;
		c.gridy = 5;
		c.anchor = GridBagConstraints.CENTER;
		add(quitButton,c);

		changeState( WAITING_TO_START_PATH );
		
		pack();
		setVisible( true );
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
				
				IJ.error( "BUG: implement me (no original file info available)" );
				return;
				
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
				System.out.println("no savePath found");
				return;
			} else {
				savePath = sd.getDirectory()+sd.getFileName();
				System.out.println("found savePath: "+savePath);
			}
			
			File file = new File(savePath);
			if ((file!=null)&&file.exists()) {
				if (!IJ.showMessageWithCancel(
					    "Save label annotation file...", "The file "+
					    savePath+" already exists.\n"+
					    "Do you want to replace it?"))
					return;
			}
			
			IJ.showStatus("Saving label annotations to "+savePath);
			
			plugin.writeTracesToFile( savePath );
			
		} else if( source == loadButton ) {
			
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
			
		} else if( source == deletePath ) {
			
			System.out.println("deletePath called");
			int [] selectedIndices = pathList.getSelectedIndexes();
			
			Arrays.sort(selectedIndices);

			for( int i = selectedIndices.length - 1; i >= 0; --i ) {
				System.out.println("removing path index: "+i);
				plugin.removePath(selectedIndices[i]);
				pathList.remove(i);
			}

		} else if( source == quitButton ) {
			
			statusText.setText("Quitting...");
			plugin.cancelSearch();
			dispose();

		}
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

	public void deletePathFromList( int index ) {

		synchronized(pathList) {
			if( (index >= pathList.getItemCount()) ||
			    (index < 0) ) {
				IJ.error("BUG: trying to delete index "+index+" in list of length "+pathList.getItemCount() );
			}

			plugin.removePath(index);

			pathList.remove(index);
		}

	}


}
