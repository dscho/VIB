package vib.app;

import java.awt.Frame;
import vib.app.gui.MainFrame;

import ij.plugin.PlugIn;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class App  implements PlugIn {

	private Options options;
	private MainFrame mainFrame;
	
	public App() {
	}

	public Options getOptions() {
		return options;
	}

	public Frame getFrame() {
		return mainFrame;
	}

	public Object getLockObject() {
		return this;
	}

	public void setOptions(Options options) {
		this.options = options;
		mainFrame.setOptions(options);
	}

	public void run(String arg) {
		options = new Options();
		if(arg != null && !arg.trim().equals(""))
			options.loadFrom(arg);
		mainFrame = new MainFrame(options);
	}

	public static void main(String[] args) {
		new App();
	}
}

