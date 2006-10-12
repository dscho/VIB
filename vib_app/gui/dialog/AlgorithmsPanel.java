package vib_app.gui.dialog;

import vib_app.Options;
import vib_app.FileGroup;

import java.awt.event.*;
import java.awt.*;

import ij.IJ;

import java.io.File;

public class AlgorithmsPanel extends Panel {

	private Options options;

	private TextField resamplingTF;
	private Choice transformationChoice;
	
	public AlgorithmsPanel(Options op) {

		this.options = op;

		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(gridbag);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5,5,5,5);


		TextArea ta = 
			new TextArea("Resampling factor specifies with which factor " + 
				"the images are resampled. The higher this value is, the " + 
				"faster, but also less accurate will the results be. The " + 
				"choice of transformation method influences also both time " + 
				"and accuracy. A detailed description about each method can " + 
				"be found in the manual",
				6, 50, TextArea.SCROLLBARS_NONE);
		ta.setEnabled(false);
		ta.setBackground(new Color(255,255,100));
		c.gridy++;
		c.weightx = 0.5;
		gridbag.setConstraints(ta, c);
		this.add(ta);

		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;

		Panel panel = new Panel(new FlowLayout());
		Label l = new Label("Resampling factor");
		panel.add(l);
		resamplingTF = new TextField(3);
		resamplingTF.setBackground(new Color(255,255,100));
		panel.add(resamplingTF);
		c.gridy++;
		gridbag.setConstraints(panel, c);
		this.add(panel);

		panel = new Panel(new FlowLayout());
		l = new Label("Transformation method");
		panel.add(l);
		transformationChoice = new Choice();
		transformationChoice.add("VIBgreyTransformation");
		transformationChoice.add("VIBlabelSurfaceTransformation");
		transformationChoice.add("VIBcenterTransformation");
		transformationChoice.add("VIBlabelDiffusionTransformation");
		transformationChoice.addItemListener(new ItemListener(){
			public void itemStateChanged(ItemEvent e) {
				options.setTransformationMethod(
					transformationChoice.getSelectedItem());
			}
		});
		panel.add(transformationChoice);
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
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
		resamplingTF.setText(Float.toString(options.getResamplingFactor()));
		transformationChoice.select(options.getTransformationMethod());
	}

	public void setOptions(Options options) {
		this.options = options;
		fillForms();
	}

	public float getResamplingFactor() {
		float f = 1.0f;
		try {
			f = Float.parseFloat(resamplingTF.getText());
		} catch(NumberFormatException e) {
			IJ.showMessage("The resampling factor is not a valid floating " + 
					"point number. Using 1.0 instead");
		}
		return f;
	}
}
