package vib.segment;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import vib.SegmentationViewerCanvas;

/**
 * Segmentation_Editor : ImageJ plugin.
 * Adds a panel containing all tools needed for a Segmentation Editor to
 * the left side of the current stack.
 * 
 * @author Francois KUSZTOS
 * @version 3.0
 */
public class SegmentationEditor implements PlugIn {

	private CustomCanvas cc;
	
	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageProcessor ip;
		if (imp==null) {
			IJ.error("No image?");
			return;
		}
		CustomCanvas cc = new CustomCanvas(imp);
		CustomStackWindow csw = new CustomStackWindow(imp, cc);
	}

	public ImagePlus getLabels() {
		return cc == null ? null : cc.getLabels();
	}
}
