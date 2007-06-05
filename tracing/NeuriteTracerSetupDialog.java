/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package tracing;

import ij.*;

import java.awt.*;
import java.awt.event.*;

import java.util.Date;

import java.text.SimpleDateFormat;

class NeuriteTracerSetupDialog
	extends Dialog
	implements ActionListener, WindowListener, ItemListener {
	
	Button okButton;
	Button cancelButton;
	
	Checkbox logCheckbox;
	Checkbox evCheckbox;
	Checkbox traceCheckbox;
	
	Checkbox preprocessCheckbox;
	Checkbox timeoutCheckbox;
	
	TextField timeoutValue;
	Panel timeoutValuePanel;
	
	Panel traceOptionsPanel;
	
	NeuriteTracer_ plugin;	
	
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
	
	public void actionPerformed( ActionEvent e ) {
		
		Object source = e.getSource();
		
		if( source == okButton ) {
			
			float timeout;
			
			try {
				timeout = Float.parseFloat( timeoutValue.getText() );				
			} catch( NumberFormatException ne ) {
				IJ.error("The timeout value must be a number");
				return;
			}
			
			if( timeout < 0 ) {
				IJ.error("The timeout value must be positive.");
				return;
			}
			
			Date now = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
			plugin.logFilename = "tracer-points-log-" + sdf.format( now );
			
			// Set parameters in the plugin...
			
			plugin.setupPreprocess = (preprocessCheckbox.getSelectedObjects() != null);
			plugin.setupTimeout = (timeoutCheckbox.getSelectedObjects() != null);
			plugin.setupTimeoutValue = timeout;
			plugin.setupTrace = (traceCheckbox.getSelectedObjects() != null);
			plugin.setupLog = (logCheckbox.getSelectedObjects() != null);
			plugin.setupEv = (evCheckbox.getSelectedObjects() != null);
			plugin.cancelled = false;
			dispose();
			
		} else if( source == cancelButton ) {
			
			plugin.cancelled = true;
			dispose();
			
		}
		
        /*
		Object source = e.getSource();
		for (int i=0; i < markButtons.length; ++i) {
			if(source == markButtons[i]) {
				plugin.mark(i);
				break;
			}
		}
		for (int i=0; i < showButtons.length; ++i) {
			if(source == showButtons[i]) {
				plugin.show(i);
				break;
			}
		}
		if(source == closeButton) {
			dispose();
		} else if (source == saveButton) {
			plugin.save();
		} else if (source == resetButton) {
			plugin.reset();
		}
        */
	}
	
	public void itemStateChanged( ItemEvent e ) {
		
		// System.out.println("Got itemStateChanged: " + e );
		
		Object source = e.getSource();
		
		if( source == logCheckbox || source == evCheckbox || source == traceCheckbox ) {
			
			// System.out.println("Yes, one of those.");
			
			if( (logCheckbox.getSelectedObjects() != null) ||
			    (evCheckbox.getSelectedObjects() != null) ) {
				
				traceOptionsPanel.setEnabled( false );
				timeoutValue.setEnabled( false );
				traceOptionsPanel.setVisible( false );
				
			} else {
				
				traceOptionsPanel.setEnabled( true );
				timeoutValue.setEnabled( true );
				traceOptionsPanel.setVisible( true );
				
			}
			
		}
		
		if( source == timeoutCheckbox ) {
			
			// System.out.println("Yes, from timeoutCheckbox" );
			
			if( timeoutCheckbox.getSelectedObjects() == null ) {
				timeoutValuePanel.setEnabled( false );
				timeoutValuePanel.setVisible( false );
			} else {
				timeoutValuePanel.setEnabled( true );
				timeoutValuePanel.setVisible( true );
			}
			
		}
		
	}
	
	public NeuriteTracerSetupDialog( String title, NeuriteTracer_ plugin ) {
		
		super( IJ.getInstance(), title, true );
		
		this.plugin = plugin;
		
		addWindowListener( this );
		
		setLayout( new BorderLayout() );
		
		Panel finishPanel = new Panel();
		finishPanel.setLayout( new FlowLayout() );
		okButton = new Button("OK");
		okButton.addActionListener( this );
		cancelButton = new Button("Cancel");
		cancelButton.addActionListener( this );
		finishPanel.add( okButton );
		finishPanel.add( cancelButton );
		add(finishPanel,BorderLayout.SOUTH);
		
		add( new Label("Tracing '" + plugin.getImagePlus().getShortTitle() + "'"), BorderLayout.NORTH );
		
		Panel optionsPanel = new Panel();
		
		CheckboxGroup checkboxGroup = new CheckboxGroup();
		
		logCheckbox = new Checkbox( "", checkboxGroup, false );
		Label logLabel = new Label("Just log points at each click", Label.LEFT );
		logCheckbox.addItemListener( this );
		
		evCheckbox = new Checkbox( "", checkboxGroup, false );
		Label evLabel = new Label("Show eigenvector at each point", Label.LEFT );
		evCheckbox.addItemListener( this );
		
		traceCheckbox = new Checkbox( "", checkboxGroup, true );
		Label traceLabel = new Label("Trace neurites", Label.LEFT );
		traceCheckbox.addItemListener( this );
		
		GridBagLayout optionsPanelLayout = new GridBagLayout();
		optionsPanel.setLayout( optionsPanelLayout );
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		optionsPanel.add(logCheckbox,c);
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		optionsPanel.add(evCheckbox,c);
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		optionsPanel.add(traceCheckbox,c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		optionsPanel.add(logLabel,c);
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		optionsPanel.add(evLabel,c);
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		optionsPanel.add(traceLabel,c);
		
		traceOptionsPanel = new Panel();
		traceOptionsPanel.setLayout( new GridBagLayout() );
		GridBagConstraints co = new GridBagConstraints();
		
		preprocessCheckbox = new Checkbox( );
		co.gridx = 0;
		co.gridy = 0;
		traceOptionsPanel.add( preprocessCheckbox, co );
		co.gridx = 1;
		co.gridy = 0;
		co.fill = GridBagConstraints.HORIZONTAL;
		traceOptionsPanel.add( new Label( "Preprocess Image?" ), co );
		
		timeoutCheckbox = new Checkbox( "", null, true );
		timeoutCheckbox.addItemListener( this );
		co.gridx = 0;
		co.gridy = 1;
		traceOptionsPanel.add( timeoutCheckbox, co );
		co.gridx = 1;
		co.gridy = 1;
		co.fill = GridBagConstraints.HORIZONTAL;
		traceOptionsPanel.add( new Label( "Timeout while tracing?" ), co );
		co.gridx = 1;
		co.gridy = 2;
		timeoutValuePanel = new Panel();
		timeoutValuePanel.setLayout( new GridBagLayout() );
		GridBagConstraints ct = new GridBagConstraints();
		ct.gridx = 0;
		ct.gridy = 0;
		timeoutValue = new TextField( "5", 8 );
		timeoutValuePanel.add( timeoutValue, ct );
		ct.gridx = 1;
		ct.gridy = 0;
		ct.fill = GridBagConstraints.HORIZONTAL;
		timeoutValuePanel.add( new Label("Timeout in seconds"), ct );
		
		co.gridx = 1;
		co.gridy = 2;
		traceOptionsPanel.add( timeoutValuePanel, co );
		
		c.gridx = 1;
		c.gridy = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		optionsPanel.add( traceOptionsPanel, c );
		
		add(optionsPanel,BorderLayout.CENTER);
		
   		pack();
		
		setVisible(true);
		
	}    
	
	public void paint(Graphics g) {
		super.paint(g);
	}	
	
	
}
