package vib.app.module;

import vib.app.Options;
import vib.app.gui.dialog.OptionsDialog;

import ij.IJ;
import ij.macro.Interpreter;
import ij.ImagePlus;
import ij.io.FileSaver;

import java.io.File;
import java.io.IOException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class OptionsModule extends Module {

	private Options options;
	private File file;
	
	public OptionsModule(Options options) {
		this.options = options;
	}
	
	public Module.Error checkDependency() {
		return new Module.Error(Module.DEPENDENCIES_MET, "");
	}

	public Object execute() {
		OptionsDialog op = new OptionsDialog(options, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equals("next")) {
					synchronized(OptionsModule.this) {
						OptionsModule.this.notifyAll();
					}
				}
			}
		});
		fillOutputPanel(op);
		
		synchronized(this){
			try{
				this.wait();
			} catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		op.cleanUp();
		clearOutputPanel();
		return null;
	}
}
