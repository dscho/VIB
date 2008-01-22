import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.filter.PlugInFilter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.IJ;
import ij.gui.PointRoi;
import ij.gui.ShapeRoi;
import ij.gui.Roi;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.Blitter;

import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;

public class Local_Threshold implements PlugInFilter {

	private ImagePlus image;
	private static ImageProcessor copy;
	private static int lastMinThreshold = 10;
	private static int lastMaxThreshold = 255;

	public void run(final ImageProcessor ip) {
		if(image.getRoi() == null) {
			IJ.error("Selection required");
			return;
		}
		Roi roiCopy = (Roi)image.getRoi().clone();
		copy = ip.duplicate();
		final GenericDialog gd = 
				new GenericDialog("Adjust local threshold");
		gd.addSlider("min value", 0, 255, lastMinThreshold);
		gd.addSlider("max value", 0, 255, lastMaxThreshold);

		final Scrollbar minSlider = (Scrollbar)gd.getSliders().get(0);
		final Scrollbar maxSlider = (Scrollbar)gd.getSliders().get(1);

		AdjustmentListener listener = new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				applyThreshold(ip, image.getRoi(), 
						minSlider.getValue(),
						maxSlider.getValue());
				lastMinThreshold = minSlider.getValue();
				lastMaxThreshold = maxSlider.getValue();
				image.updateAndDraw();
			}
		};
		minSlider.addAdjustmentListener(listener);
		maxSlider.addAdjustmentListener(listener);

		applyThreshold(ip, image.getRoi(), 
				lastMinThreshold, lastMaxThreshold);
		image.updateAndDraw();
		gd.showDialog();

		// Convert area to selection
		ip.setRoi(image.getRoi());
		ImageProcessor newip = ip.crop();
		newip.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		ImagePlus tmp = new ImagePlus("", newip);
		ThresholdToSelection ts = new ThresholdToSelection();
		ts.setup("", tmp);
		ts.run(newip);
		newip.resetThreshold();
		ip.insert(copy, 0, 0);
		Rectangle roiCopyR = roiCopy.getBounds();
		if(tmp.getRoi() != null) {
			Rectangle roiTempR = tmp.getRoi().getBounds();
			tmp.getRoi().setLocation(roiCopyR.x + roiTempR.x, 
						roiCopyR.y + roiTempR.y);
			image.setRoi(tmp.getRoi());
		}
	}

	public static void applyThreshold(ImageProcessor ip, 
						Roi roi, int min, int max) {
		if(roi == null) {
			IJ.error("Selection required");
			return;
		}
		boolean mustCleanUp = copy == null;
		if(copy == null) {
			 copy = ip.duplicate();
		}

		Rectangle r = roi.getBounds();
		byte[] p = (byte[])ip.getPixels();
		byte[] c = (byte[])copy.getPixels();

		for(int y = r.y; y < r.y+r.height; y++) {
			for(int x = r.x; x < r.x+r.width; x++) {
				if(!roi.contains(x, y))
					continue;
				int index = y*ip.getWidth() + x;
				if(((int)c[index]&0xff) >= min &&
						((int)c[index]&0xff) <= max) {
					p[index] = (byte)255;
				} else {
					p[index] = c[index];
				}
			}
		}
		if(mustCleanUp) copy = null;
	}

	public int setup(String args, ImagePlus imp) {
		this.image = imp;
		return DOES_8G;
	}
}
