package vib_app;

import java.awt.Frame;
import vib_app.gui.MainFrame;

public class App {

	private static App instance = new App();
	private Options options;
	private MainFrame mainFrame;
	
	private App() {
		options = new Options();
		mainFrame = new MainFrame(options);
	}

	public static App instance() {
		return instance;
	}

	public Options getOptions() {
		return options;
	}

	public Frame getFrame() {
		return mainFrame;
	}

	public void setOptions(Options options) {
		this.options = options;
		mainFrame.setOptions(options);
	}

	public static void main(String[] args) {
		App.instance();
	}
}

	
