package vib.app.module;

import java.io.File;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import vib.app.VIBImage;
import vib.app.Options;

import vib.segment.CustomStackWindow;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;

public class Label extends Module {

	public Label(VIBImage imp, Options options) {
		super(imp, options, false);
		dependingOn.add(Load.class);
	}

	public String getName() {
		return "Labelling";
	}

	public int checkResults() {
		// check availability of results
		File ref = new File(image.referencePath);
		File labels = new File(image.labelsPath);
		if(!labels.exists()) {
			return RESULTS_UNAVAILABLE;
		}
		
		// available, check uptodate
		if(labels.lastModified() != 0L && 
				labels.lastModified() > ref.lastModified()) {
			return RESULTS_OK;
		}
		// available, but out of date
		return RESULTS_OUT_OF_DATE;
	}

	protected void runThisModule() {
		ActionListener a = new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("Ok")) {
					synchronized(Label.this) {
						Label.this.notifyAll();
					}
				}
			}
		};
		
		CustomStackWindow csw = 
			new CustomStackWindow(image.getReferenceChannel());
		csw.addActionListener(a);
		synchronized(this) {
			try {
				this.wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		ImagePlus labels = csw.getLabels();
		csw.cleanUp();
		image.setLabels(labels);
		console.append("...saving labels");
		if(!image.saveLabels())
			console.append("Could not write labels");
		image.releaseReferenceChannel();
		image.releaseLabels();
	}
}
