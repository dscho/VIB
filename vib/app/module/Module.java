package vib.app.module;

import vib.app.gui.dialog.Console;

import java.awt.Component;
import java.awt.Panel;
import java.util.List;
import java.util.ArrayList;

public abstract class Module {

	/** the required files etc are not here, calculation is impossible */
	public static final int REQUIREMENTS_UNAVAILABLE = 0;
	/** requirements ok, and results do not exist */
	public static final int RESULTS_UNAVAILABLE = 2;
	/** requirements ok and results exist, but are out of date */
	public static final int RESULTS_OUT_OF_DATE = 3;
	/** requirements are ok and results both exist and are uptodate */
	public static final int RESULTS_OK = 4;

	protected Panel outputPanel;
	protected Console console = Console.instance();

	public static class Error {
		String message;
		int id;

		public Error(int id, String message) {
			this.id = id; 
			this.message = message;
		}

		public int id() { return id; }
		
		public String message() { return message; }

		public boolean equals(Object o) {
			return (o instanceof Error  && ((Error)o).id == this.id);
		}
	}

	public void setOutputPanel(Panel p) {
		this.outputPanel = p;
	}

	protected void fillOutputPanel(Panel p) {
		if(outputPanel != null) {	
			outputPanel.removeAll();
			outputPanel.add(p);
			outputPanel.validate();
		}
	}

	protected void clearOutputPanel() {
		if(outputPanel != null) {
			Component[] c = outputPanel.getComponents();
			if(c.length == 0 || 
					(c.length == 1 && c[0] == Console.instance()))
				return;
			outputPanel.removeAll();
			outputPanel.add(Console.instance());
			outputPanel.validate();
		}
	}

	public abstract String getName();
	
	public abstract Error checkDependency();
	
	public abstract Object execute();
}
