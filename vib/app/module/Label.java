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

	public String getName() {
		return "Labelling";
	}

	public Module.Error checkDependency() {
		// check requirements available
		File ref = new File(image.getReferencePath());
		if(!ref.exists())
			return new Error(Module.REQUIREMENTS_UNAVAILABLE, 
									"Reference file does not exist");
		// check availability of results
		File labels = new File(image.getLabelsPath());
		if(!labels.exists()) {
			return new Error(Module.RESULTS_UNAVAILABLE, "");
		}
		
		// available, check uptodate
		if(labels.lastModified() != 0L && 
				labels.lastModified() > ref.lastModified()) {
			return new Error(Module.RESULTS_OK, "");
		}
		// available, but out of date
		return new Error(Module.RESULTS_OUT_OF_DATE, "");
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
		if(!image.saveLabels())
			console.append("Could not write labels");
		clearOutputPanel();
		return image;
	}
}
