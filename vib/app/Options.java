package vib.app;

import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Properties;

import ij.IJ;

public class Options {

	public static String VIBgreyTransformation =
											"VIBgreyTransformation";
	public static String VIBlabelSurfaceTransformation =
											"VIBlabelSurfaceTransformation";
	public static String VIBcenterTransformation =
											"VIBcenterTransformation";
	public static String VIBlabelDiffusionTransformation =
											"VIBlabelDiffusionTransformation";

	private File workingDirectory;
	private FileGroup filegroup;
	private File template;
	private int numChannels = 2;
	private int refChannel = 2;
	private int resamplingFactor = 1;
	private String transformationMethod = VIBlabelDiffusionTransformation;

	// Constructors
	public Options() {
		filegroup = new FileGroup("NewFilegroup");
	}

	public Options(String dirname) {
		this();
		File dir = new File(dirname);
		if(!dir.exists()){
			dir.mkdir();
		}
		workingDirectory = dir;
	}

	public Options(File dir) {
		this();
		workingDirectory = dir;
	}

	// Setter
	public void setWorkingDirectory(File f) {
		workingDirectory = f;
	}

	public void setFileGroup(FileGroup fg) {
		filegroup = fg;
	}

	public void setTemplate(File f){
		template = f;
	}

	public boolean setTemplate(String path) {
		File file = new File(path);
		if(!file.exists()){
			return false;
		}
		template = file;
		return true;
	}


	public void setNumChannels(int i) {
		numChannels = i;
	}

	public void setRefChannel(int i) {
		refChannel = i;
	}

	public boolean setTransformationMethod(String method) {
		if(transformationValid(method)) {
			transformationMethod = method;
			return true;
		}
		return false;
	}

	public void setResamplingFactor(int factor) {
		resamplingFactor = factor;
	}

	// Getter
	public File getWorkingDirectory() {
		return workingDirectory;
	}

	public FileGroup getFileGroup() {
		return filegroup;
	}

	public File getTemplate() {
		return template;
	}

	public int getNumChannels() {
		return numChannels;
	}

	public int getRefChannel() {
		return refChannel;
	}

	public String getTransformationMethod() {
		return transformationMethod;
	}

	public int getResamplingFactor() {
		return resamplingFactor;
	}

	// Validity

	public boolean transformationValid(String method) {
		return (method.equals(VIBgreyTransformation) ||
				method.equals(VIBlabelSurfaceTransformation) ||
				method.equals(VIBcenterTransformation) ||
				method.equals(VIBlabelDiffusionTransformation));

	}

	public boolean isValid(){
		return !filegroup.isEmpty() &&
			template != null && template.exists() &&
			numChannels > 0 &&
			refChannel > 0 && refChannel <= numChannels &&
			workingDirectory.exists() &&
			workingDirectory.isDirectory() &&
			transformationValid(transformationMethod);
	}

	// Utility functions
	public Options clone() {
		Options clone = new Options(workingDirectory);
		clone.template = template;
		clone.numChannels = numChannels;
		clone.refChannel = refChannel;
		clone.filegroup = filegroup;
		clone.transformationMethod = transformationMethod;
		clone.resamplingFactor = resamplingFactor;
		return clone;
	}

	public void copy(Options options) {
		workingDirectory = options.workingDirectory;
		template = options.template;
		numChannels = options.numChannels;
		refChannel = options.refChannel;
		filegroup = options.filegroup;
		transformationMethod = options.transformationMethod;
		resamplingFactor = options.resamplingFactor;
	}

	public void saveTo(String path) {
		Properties properties = new Properties();
		properties.setProperty("workingDirectory",
									workingDirectory.getAbsolutePath());
		properties.setProperty("template",
									template.getAbsolutePath());
		properties.setProperty("numChannels",
									Integer.toString(numChannels));
		properties.setProperty("refChannel",
									Integer.toString(refChannel));
		properties.setProperty("resamplingFactor",
									Integer.toString(resamplingFactor));
		properties.setProperty("transformationMethod",
									transformationMethod);
		properties.setProperty("filegroup", filegroup.toCSV());

		try {
			OutputStream out = new FileOutputStream(path);
			properties.store(out, "Created by the VIB application");
		} catch(FileNotFoundException e) {
			IJ.showMessage("Can't find file " + path);
		} catch(IOException e) {
			IJ.showMessage("Can't write to file " + path);
		}
	}

	public void loadFrom(String path) {
		Properties properties = null;
		try {
			properties = new Properties();
			properties.load(new FileInputStream(path));
		} catch (FileNotFoundException e) {
			IJ.showMessage("Can't find file " + path);
			return;
		} catch(IOException e) {
			IJ.showMessage("Can't read from file " + path);
			return;
		}
		workingDirectory =
			new File(properties.getProperty("workingDirectory"));
		template =
			new File(properties.getProperty("template"));
		numChannels =
			Integer.parseInt(properties.getProperty("numChannels"));
		refChannel =
			Integer.parseInt(properties.getProperty("refChannel"));
		transformationMethod =
			properties.getProperty("transformationMethod");
		resamplingFactor =
			Integer.parseInt(properties.getProperty("resamplingFactor"));
		if(!filegroup.fromCSV(properties.getProperty("filegroup"))) {
			IJ.showMessage("Not all files specified in the file group exist.");
		}
		if(!isValid()){
			IJ.showMessage("There occured an error while setting the " +
					"options. I set some of them, but you should check them " +
					"manually.");
			return;
		}
	}

	// debug
	public void debug() {
		System.out.println("\nOptions:");
		System.out.println("workingDirectory = " + workingDirectory);
		System.out.println("template = " + template);
		System.out.println("numChannels = " + numChannels);
		System.out.println("refChannel = " + refChannel);
		System.out.println("transformationMethod = " + transformationMethod);
		System.out.println("resamplingFactor = " + resamplingFactor);
		System.out.println("filegroup: ");
		filegroup.debug();
	}

	public static void main(String[] args){
		System.out.println("Options");
	}
}
