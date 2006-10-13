package vib_app.gui.dialog;

import vib_app.Options;
import vib_app.FileGroup;

import java.awt.event.*;
import java.awt.*;

import java.io.File;

public class ChannelsPanel extends Panel implements ItemListener {

	private Options options;

	private Choice numChannelChoice;
	private Choice refChannelChoice;
	
	public ChannelsPanel(Options op) {

		this.options = op;

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,5,5,5);

		TextArea ta = new TextArea("Specify the number of this. This " +
				"will in most cases be 2, none channel for the reference " + 
				"staining and one for the expression pattern",
				3,50, TextArea.SCROLLBARS_NONE);
		ta.setEnabled(false);
		ta.setBackground(new Color(255,255,100));
		c.gridy++;
		c.weightx = 0.5;
		gridbag.setConstraints(ta, c);
		this.add(ta);
		
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
	
		Panel panel = new Panel(new FlowLayout());
		Label l = new Label("Number of channels");
		panel.add(l);
		numChannelChoice = new Choice();
		numChannelChoice.add("1");
		numChannelChoice.add("2");
		panel.add(numChannelChoice);
		c.gridy++;
		gridbag.setConstraints(panel, c);
		this.add(panel);

		panel = new Panel(new FlowLayout());
		l = new Label("Number of the reference channel");
		panel.add(l);
		refChannelChoice = new Choice();
		refChannelChoice.add("1");
		refChannelChoice.add("2");
		panel.add(refChannelChoice);
		c.gridy++;
		gridbag.setConstraints(panel, c);
		this.add(panel);
		
		
		// just to fill the rest of the space, otherwise all 
		// components are centered, which looks ugly
		panel = new Panel();
		c.gridy++;
		c.weighty = 0.5;
		gridbag.setConstraints(panel, c);
		this.add(panel);

		fillForms();
	}

	public void fillForms() {
		refChannelChoice.select(Integer.toString(options.getRefChannel()));
		numChannelChoice.select(Integer.toString(options.getNumChannels()));
	}

	public void setOptions(Options options) {
		this.options = options;
		fillForms();
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == numChannelChoice) {
			options.setNumChannels(
					Integer.parseInt(numChannelChoice.getSelectedItem()));
		} else if(e.getSource() == refChannelChoice) {
			options.setRefChannel(
					Integer.parseInt(refChannelChoice.getSelectedItem()));
		}
	}
}
