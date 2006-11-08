package vib.app.module;

import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import vib.app.VIBImage;
import vib.app.Options;

import vib.AmiraMeshEncoder;

import vib.app.gui.dialog.Console;
import vib.app.gui.dialog.LabelPanel;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

public class Label extends Module {

	private VIBImage image;
	private Options options;
	
	public Label(VIBImage imp, Options options) {
		this.image = imp;
		this.options = options;
	}

	public Module.Error checkDependency() {
		console.append("\n * Labelling...\n");
		// check requirements
		File ref = new File(image.getReferencePath());
		if(!ref.exists())
			return new Error(
					Module.DEPENDENCIES_UNMET, "Reference file does not exist");
		// check availability of results
		File labels = new File(image.getLabelsPath());
		if(labels.lastModified() != 0L && 
				labels.lastModified() > ref.lastModified()) {
			console.append("...skipping, since results are already available");
			return new Error(Module.RESULTS_AVAILABLE, "");
		}
		// not available, but at least the requirements are fullfilled		
		return new Error(Module.DEPENDENCIES_MET, "");
	}

	public Object execute() {
		ActionListener a = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("OK")) {
					synchronized(Label.this) {
						Label.this.notifyAll();
					}
				}
			}
		};
		
		LabelPanel label = new LabelPanel(image.getReferenceChannel(),a);
		fillOutputPanel(label);
		synchronized(this) {
			try {
				this.wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		ImagePlus labels = label.getLabels();
		image.setLabels(labels);
		clearOutputPanel();
		label.removeActionListener(a);
		label.cleanUp();
		fillOutputPanel(console);
		console.append("Labelling done");
		console.append("Saving labels...");
		boolean w = image.saveLabels();
		if(w) console.append("Labels successfully written");
		else console.append("Could not write labels");
		clearOutputPanel();
		return image;
	}
}
