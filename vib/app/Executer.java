package vib.app;

import ij.ImagePlus;

import vib.app.gui.dialog.Console;
import vib.app.gui.dialog.OptionsDialog;
import vib.app.module.*;

import java.io.File;

import java.awt.Panel;

public class Executer {
	
	private boolean[] finished;
	private int state;
	private Panel panel;
	private Options options;
	private Thread execution_thread;

	public Executer(Options options){
		this.options = options;
	}

	public void initialize(Options o) {
		FileGroup fg = o.getFileGroup();
		int n = 1 + // Options
				fg.size() * 4 + // Load - Label - Resample - Statistics
				1 + // Register
				1;  // Result
		finished = new boolean[n];
		for(int i = 0; i < n; i++) finished[i] = false;
		state = 0;
	}

	public int getState() {
		return state;
	}

	public String getStateString() {
		return null;
	}

	public void setOutputPanel(Panel panel) {
		this.panel = panel;
	}

	private void finishedNextStep() {
		System.out.println("Finished step " + state);
		finished[state++] = true;
	}

	private void executeModule(Module m) {
		Module.Error error = m.checkDependency();
		if(error.id() == Module.RESULTS_AVAILABLE) {
			System.out.println("step " + state + ": skipping, since available");
			finishedNextStep();
		}
		if(error.id() == Module.DEPENDENCIES_UNMET)
			throw new RuntimeException(error.message());
		if(error.id() == Module.DEPENDENCIES_MET) {
			m.setOutputPanel(panel);
			m.execute();
			finishedNextStep();
		}
	}

	public Thread getExecutionThread() {
		return execution_thread;
	}

	VIBImage image = null;
	public void run(){
		// new thread to ensure that the GUI isn't blocked
		execution_thread = new Thread(new Runnable() {
			public void run() {
				Console.instance().append("-------------------------");
				Console.instance().append("1. Preprocessing");
				Console.instance().append("-------------------------");
				Module m = new OptionsModule(options);
				m.setOutputPanel(panel);
				m.execute();
				// after the options are set, the Executer can be initialized
				initialize(options);
				finishedNextStep();
				// cycle through load - label - resample - statistics
				FileGroup fg = options.getFileGroup();
				for(int i = 0; i < fg.size(); i++) {
					// The original file we are working on
					File file = fg.get(i);
					// The VIBImage which stores references to all important
					// images
					image = new VIBImage(
							options.getWorkingDirectory().getAbsolutePath(),
							file.getName().substring(0, file.getName().
								lastIndexOf('.')) + ".tif",
							options.getRefChannel(),
							options.getResamplingFactor());
					// And now start off!
					m = new Load(image, file, options);
					executeModule(m);
					m = new Label(image, options);
					executeModule(m);
					image.releaseLabels();
					m = new Resample(image, options);
					executeModule(m);
				}
				System.out.println("finished thread " + 
					Thread.currentThread().getName());
			}
		}, "Executer");
		execution_thread.start();
	}
}
