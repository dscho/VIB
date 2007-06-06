/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;
import ij.io.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

class NeuriteTracerResultsDialog
	extends Dialog
	implements ActionListener, WindowListener {
	
	NeuriteTracer_ plugin;

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

	List pathList;

	Button deletePath;

	Button saveButton;
	Button loadButton;   
	Button uploadButton;
	Button fetchButton;
	
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
	
	boolean launchedByArchive;
	
	public NeuriteTracerResultsDialog( String title,
					   NeuriteTracer_ plugin,
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

		c.gridx = 0;
		c.gridy = 0;
		add(statusPanel,c);

		pathActionPanel = new Panel();
		completePath = new Button("Complete Path");
		cancelPath = new Button("Cancel Path");
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

		preprocess = new Checkbox("Hessian-based analysis");
		justLog = new Checkbox("Just log points");

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
		pathListPanel.add(pathList,BorderLayout.CENTER);
		deletePath = new Button("Delete Path");
		pathListPanel.add(deletePath,BorderLayout.SOUTH);

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

		/*
			
		ScrollPane scrollPane=new ScrollPane();
		Panel pathsPanel=new Panel();
		pathsPanel.setLayout( new GridBagLayout() );
			
		c.gridx = 0;
		c.gridy = 0;
		pathsPanel.add(new Label("Test"),c);
		c.gridx = 0;
		c.gridy = 1;
		pathsPanel.add(new Label("Testing Again"),c);
			
		scrollPane.add(pathsPanel);
		
		add(scrollPane, BorderLayout.CENTER );
			
		Panel saveAndLoadPanel=new Panel();
		saveAndLoadPanel.setLayout( new FlowLayout() );
		saveAndLoadPanel.add( saveButton );
		saveAndLoadPanel.add( loadButton );
		add( saveAndLoadPanel, BorderLayout.SOUTH );

		*/
		
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
			
		}
	}
	
        public void paint(Graphics g) {
                super.paint(g);
        }
}
