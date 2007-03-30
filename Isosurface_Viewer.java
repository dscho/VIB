import ij.process.ImageProcessor;
import ij.ImageStack;
import ij.ImagePlus;
import ij.plugin.filter.PlugInFilter;
import ij.gui.GenericDialog;

import ij3d.ImageWindow3D;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.Checkbox;
import java.awt.Button;
import java.awt.Frame;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.*;

import java.util.Vector;

import vib.Resample_;
import vib.InterpolatedImage;

import isosurface.IsosurfacePanel;

public class Isosurface_Viewer implements PlugInFilter {

	private ImagePlus image;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Isosurface Viewer");
		gd.addNumericField("Threshold", 50, 0);
		gd.addNumericField("Resampling factor", 2, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int threshold = (int)gd.getNextNumber();
		int resample = (int)gd.getNextNumber();
		if(resample != 1)
			image = Resample_.resample(image, resample, resample, resample);
		IsosurfacePanel isopanel = new IsosurfacePanel(image, threshold);
		isopanel.canvas.setSize(new Dimension(512, 512));
		ImageWindow3D win = new ImageWindow3D("Surface Viewer",
				isopanel.canvas, isopanel.simpleU);
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | NO_CHANGES;
	}
}
