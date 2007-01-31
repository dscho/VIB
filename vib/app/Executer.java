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
		finished[state++] = true;
	}

	private void executeModule(Module m) {
		Console console = Console.instance();
		console.append("...executing " + m.getName());
		Module.Error error = m.checkDependency();
		if(error.id() == Module.RESULTS_OK) {
			console.append("skipping! Results are available and uptodate");
			finishedNextStep();
		}
		else if(error.id() == Module.RESULTS_OUT_OF_DATE) {
			console.append("skipping! Results are not uptodate, but available");
			finishedNextStep();
		}
		else if(error.id() == Module.REQUIREMENTS_UNAVAILABLE) {
			throw new RuntimeException(error.message());
		}
		else if(error.id() == Module.RESULTS_UNAVAILABLE) {
			m.setOutputPanel(panel);
			m.execute();
			finishedNextStep();
		}
	}

	public Thread getExecutionThread() {
		return execution_thread;
	}

	VIBImage image = null;
	VIBImage template = null;
	public void run(){
		// new thread to ensure that the GUI isn't blocked
		execution_thread = new Thread(new Runnable() {
			public void run() {
				Console console = Console.instance();
				console.clear();
				console.append("----------------------------------------");
				console.append("1. Preprocessing");
				console.append("----------------------------------------");
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
					m = new TissueStatistics(image, options);
					executeModule(m);
				}
				console.append("\n\n");
				console.append("----------------------------------------");
				console.append("2. Registration");
				console.append("----------------------------------------");
				// create the template VIBImage
				String templFile = options.getTemplate().getName();
				template = new VIBImage(
					options.getWorkingDirectory().getAbsolutePath(),
								templFile.substring(0, 
								templFile.lastIndexOf('.')) + ".tif",
					options.getRefChannel(),
					options.getResamplingFactor());

				// now start with the actual work, again with the whole 
				// filegroup.
				for(int i = 0; i < fg.size(); i++) {
					// The original file we are working on
					File file = fg.get(i);
					// don't proceed the template file
					if(file.getName().equals(options.getTemplate().getName()))
						continue;
					// The VIBImage which stores references to all important
					// images
					image = new VIBImage(
							options.getWorkingDirectory().getAbsolutePath(),
							file.getName().substring(0, file.getName().
								lastIndexOf('.')) + ".tif",
							options.getRefChannel(),
							options.getResamplingFactor());
					
					// And now start off!
					m = new LabelCenterTransformation(image, template, options);
					executeModule(m);
				}
				console.append("\n\n...done");	
			}
		}, "Executer");
		execution_thread.start();
	}
}
