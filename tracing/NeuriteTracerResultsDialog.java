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
	
	Button saveButton;
	Button loadButton;   
	Button uploadButton;
	Button getMyButton;
	Button getAnyButton;
	
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
	
	public NeuriteTracerResultsDialog( String title, NeuriteTracer_ plugin, boolean launchedByArchive ) {
		
		super( IJ.getInstance(), title, false );
		this.plugin = plugin;
		this.launchedByArchive = launchedByArchive;
		
		if( launchedByArchive ) {
			
			uploadButton = new Button("Upload");
			uploadButton.addActionListener( this );
			getMyButton = new Button("Get (Mine)");
			getMyButton.addActionListener( this );
			getAnyButton = new Button("Get (Any)");
			getAnyButton.addActionListener( this );
			setLayout( new FlowLayout() );
			add( uploadButton );
			add( getMyButton );
			add( getAnyButton );
			
			
		} else {
			
			setLayout( new BorderLayout() );
			
			ScrollPane scrollPane=new ScrollPane();
			Panel pathsPanel=new Panel();
			pathsPanel.setLayout( new GridBagLayout() );
			
			GridBagConstraints c=new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			pathsPanel.add(new Label("Test"),c);
			c.gridx = 0;
			c.gridy = 1;
			pathsPanel.add(new Label("Testing Again"),c);
			
			scrollPane.add(pathsPanel);
			
			add(scrollPane, BorderLayout.CENTER );
			
			saveButton = new Button("Save");
			saveButton.addActionListener( this );
			loadButton = new Button("Load");
			loadButton.addActionListener( this );
			
			Panel saveAndLoadPanel=new Panel();
			saveAndLoadPanel.setLayout( new FlowLayout() );
			saveAndLoadPanel.add( saveButton );
			saveAndLoadPanel.add( loadButton );
			add( saveAndLoadPanel, BorderLayout.SOUTH );
			
		}
		
		pack();
		setVisible( true );
	}
	
	public void actionPerformed( ActionEvent e ) {
		
		Object source = e.getSource();
		
		if( source == uploadButton ) {
			plugin.uploadTracings();
		} else if( source == getMyButton ) {
			plugin.getTracings( true );
		} else if( source == getAnyButton ) {
			plugin.getTracings( false );
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
