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
	private ImagePlus ret;
	private int minX = Integer.MAX_VALUE;
	private int maxX = Integer.MIN_VALUE;
	private int w;
	private int h;
	private int d;
	private IsosurfacePanel canvas;
	private static Frame frame;

	public void run(ImageProcessor ip) {
		GenericDialog gd = new GenericDialog("Isosurface Viewer");
		gd.addNumericField("Threshold", 50, 0);
		gd.addCheckbox("Smooth", false);
		gd.addNumericField("Resampling factor", 2, 0);
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		int threshold = (int)gd.getNextNumber();
		boolean smooth = gd.getNextBoolean();
		int resample = (int)gd.getNextNumber();
		process(threshold, smooth, resample);
		if(frame == null)
			showViewer(threshold);
		else 
			updateViewer(threshold);
	}

	public void process(int threshold, boolean smooth, int resample) { 
		init();
		if(smooth)
			smooth();
		if(resample != 1 )
			resample(resample);
	}

	private void init() {
		ret = new InterpolatedImage(image).cloneImage().getImage();
		w = ret.getWidth();
		h = ret.getHeight();
		d = ret.getStackSize();
	}
	
	private void smooth() {
		for(int z=0; z< d; z++)	{
			ImageStack stack = ret.getStack();
			ImageProcessor ip = stack.getProcessor(z+1);
			ip.smooth();
		}
	}

	public void resample(int fac) {
		ImagePlus resampled = Resample_.resample(ret, fac, fac, fac);
		w = resampled.getWidth();
		h = resampled.getHeight();
		d = resampled.getStackSize();
		ret = resampled;
	}

	public void showViewer(int threshold) {
		canvas = new IsosurfacePanel(ret, threshold);
		canvas.canvas.setSize(new Dimension(512, 512));
		ImageWindow3D win = new ImageWindow3D("Surface Viewer",
				canvas.canvas, canvas.simpleU);
	}

	public void updateViewer(int threshold) {
		canvas.updateShape(ret, threshold);
	}

	public int setup(String arg, ImagePlus img) {
		this.image = img;
		return DOES_8G | NO_CHANGES;
	}
}
