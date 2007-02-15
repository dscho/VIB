package vib.app.module;

import ij.IJ;

import vib.app.gui.Console;
import vib.app.Options;
import vib.app.VIBImage;
import vib.app.FileGroup;

import java.util.List;
import java.util.ArrayList;

public abstract class Module {

	/** requirements ok, and results do not exist */
	public static final int RESULTS_UNAVAILABLE = 2;
	/** requirements ok and results exist, but are out of date */
	public static final int RESULTS_OUT_OF_DATE = 3;
	/** requirements are ok and results both exist and are uptodate */
	public static final int RESULTS_OK = 4;

	protected Options options;
	protected List<Class> dependingOn;
	protected VIBImage image;
	protected Console console = Console.instance();
	protected boolean runDependingModulesWithAllImages;

	public Module(VIBImage image, Options options, 
							boolean runDependingModulesWithAllImg) {
		this.image = image;
		this.options = options;
		this.dependingOn = new ArrayList<Class>();
		this.runDependingModulesWithAllImages = runDependingModulesWithAllImg;
	}

	public void run() {
		runDependingModules();
		Console.instance().append("running " + getName());
		if(decideWhetherToRun()) {
			runThisModule();
		}
	}

	public abstract String getName();
	
	public abstract int checkResults();
	
	protected abstract void runThisModule();

	protected void runDependingModules() {
		if(runDependingModulesWithAllImages)
			runDependingModulesWithAllImages();
		else
			runDependingModulesWithOneImage();
	}

	private boolean decideWhetherToRun() {
		int dep = checkResults();
		if(dep == RESULTS_OK) {
			console.append("...results available...skipping");
            return false;
		}
        else if(dep == RESULTS_OUT_OF_DATE) {
            console.append("...results out of date,nevertheless...skipping");
            return false;
        }
		return true;
	}


	private Module getModule(Class modClass, VIBImage img, Options o) {
		Module module = null;
		try {
			module = (Module)modClass.getConstructors()[0].newInstance(
		                 img, o);
		} catch (Exception e) {
			console.append("Can't load module " +  modClass.getName());
			e.printStackTrace();
			throw new RuntimeException();
		}
		return module;
	}

	private void runDependingModulesWithAllImages(){
		FileGroup fg = options.getFileGroup();
		for(int i = 0; i < fg.size(); i++) {
			VIBImage image = new VIBImage(fg.get(i), options);
			for(Class modClass : dependingOn) {
				Module module = getModule(modClass, image, options);
				module.run();
			}
		}
	}

	private void runDependingModulesWithOneImage(){
		for(Class modClass : dependingOn) {
			Module module = getModule(modClass, image, options);
			module.run();
		}
	}
}
