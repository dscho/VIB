package vib.segment;
import ij.measure.Calibration;
import ij.plugin.*;
import ij.*;
import ij.gui.*;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * Segmentation_Editor : ImageJ plugin.
 * Adds a panel containing all tools needed for a Segmentation Editor to
 * the left side of the current stack.
 * 
 * @author François KUSZTOS
 * @version 3.0
 */
public class Segmentation_Editor implements PlugIn {
	CustomCanvas cc;
	ContainerPanel containerPanel;

	public void run(String arg) {
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageProcessor ip;
		if (imp==null) {
			IJ.error("No image?");
			return;
		}
		cc = new CustomCanvas(imp);
		new CustomStackWindow(imp);
	}

	class CustomCanvas extends ImageCanvas {

		CustomCanvas(ImagePlus imp) {
			super(imp);
			setBounds(new Rectangle(getSize()));
		}

		public Dimension getMinimumSize() {
			return getSize();
		}

		public void mousePressed(MouseEvent e) {
			super.mousePressed(e);
		}

		public void mouseExited(MouseEvent e) {
			super.mouseExited(e);
			containerPanel.pInfos.updateLabels("     -     ", "     -     ", "     -     ", "     -     ");
		}

		public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);

			int x = offScreenX(e.getX());
			int y = offScreenY(e.getY());

			double posX, posY, posZ;
			String voxelValue;

			if(x<imp.getWidth() && y<imp.getHeight()) {
				Calibration cal = imp.getCalibration();
				posX = cal.getX(x);
				posX = Double.valueOf(IJ.d2s(posX)).doubleValue();

				posY = cal.getY(y);
				posY = Double.valueOf(IJ.d2s(posY)).doubleValue();

				posZ = cal.getZ(imp.getCurrentSlice()-1);
				posZ = Double.valueOf(IJ.d2s(posZ)).doubleValue();

				voxelValue = getValueAsString(x, y);

				containerPanel.pInfos.updateLabels(posX, posY, posZ, voxelValue);
			} else {
				containerPanel.pInfos.updateLabels("     -     ", "     -     ", "     -     ", "     -     ");
			}

		}

		/**
		 * Method copied from ij.ImagePlus. It calculates the voxel value of the
		 * current location. It must be a copy because in ij.ImagePlus, this method
		 * is inaccessible (private).
		 * @param x		the horizontal position of the mouse - double
		 * @param y		the vertical position of the mouse - double
		 * @return		the value calculated - String
		 */
		public String getValueAsString(int x, int y) {
			//the values of the types are dedined in the class ImagePlus
			Calibration cal = imp.getCalibration();
			int[] v = imp.getPixel(x, y);
			int type = imp.getType();
			switch (type) {
				case 0 /*GRAY8*/: case 1 /*GRAY16*/: case 3 /*COLOR_256*/:
					if (type==3 /*COLOR_256*/) {
						if (cal.getCValue(v[3])==v[3]) // not calibrated
							return("index=" + v[3] + ", value=" + v[0] + "," + v[1] + "," + v[2]);
						else
							v[0] = v[3];
					}
					double cValue = cal.getCValue(v[0]);
					if (cValue==v[0])
						return("" + v[0]);
					else
						return(IJ.d2s(cValue) + " ("+v[0]+")");
				case 2 /*GRAY32*/:
					return("" + Float.intBitsToFloat(v[0]));
				case 4 /*COLOR_RGB*/:
					return("" + v[0] + "," + v[1] + "," + v[2]);
				default: return("");
			}
		}

		public void setMagnification(double magnification) {
			super.setMagnification(magnification);
			if(containerPanel != null &&
					containerPanel.pMain != null)
				containerPanel.pMain.updateLZoomLevel(magnification);
		}

		/** Overrides handlePopupMenu() in ImageCanvas to suppress the right-click popup menu. */
		//protected void handlePopupMenu(MouseEvent e) {
		//}

	} // CustomCanvas inner class


	class CustomStackWindow extends StackWindow {

		Labelizer iLabelizer;
		Roi[] svgRoi;
		ImageStack segmentedImage;
		int oldSlice;

		ImagePlus ip;
		StackWindow sw;

		CustomStackWindow(ImagePlus imp) {
			super(imp, cc);
			setLayout(new BorderLayout());
			setBackground(Color.LIGHT_GRAY);
			remove(sliceSelector);
			remove(cc);
			addPanels();

			Container sliceAndImage = new Container();
			sliceAndImage.setSize(cc.getSize());
			sliceAndImage.setLayout(new BorderLayout());
			sliceAndImage.add(sliceSelector, BorderLayout.NORTH);
			sliceAndImage.add(new Label(" "));
			sliceAndImage.add(cc, BorderLayout.CENTER);
			repaint();

			add(sliceAndImage, BorderLayout.CENTER);
			pack();

			svgRoi = new Roi[imp.getStack().getSize() + 1]; // "+1" is to have enough place if the slice
			// number is handled from 1 to numSlices
			int x = imp.getWidth();
			int y = imp.getHeight();
			segmentedImage = new ImageStack(x, y);
			for(int i=0; i<= imp.getStack().getSize(); i++) {
				segmentedImage.addSlice("slice"+i, new ByteProcessor(x, y));
			}
			oldSlice = sliceSelector.getValue();
			new ImagePlus("labels", segmentedImage).show();
		} 

		public Dimension getMinimumSize() {
			return getSize();
		}

		void addPanels() {
			containerPanel = new ContainerPanel(cc);
			containerPanel.setVisible(true);
			add(containerPanel, BorderLayout.WEST);
			pack();
		}

		public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
			super.adjustmentValueChanged(e);
			svgRoi[oldSlice] = imp.getRoi();
			segmentedImage.getProcessor(oldSlice).setRoi(imp.getRoi());
			oldSlice = sliceSelector.getValue();
			try {
				imp.setRoi(svgRoi[oldSlice]);
				iLabelizer.addToMaterial();
			} catch(NullPointerException exc) {
				System.out.println("Not yet ROI for this slice (number " + oldSlice + ")");
			}
		}

		public void setLabelizer(Labelizer lab) {
			iLabelizer = lab;
		}

		/**
		 * overridden in order to fix the problem of drawing a rectangle close to the ImageCanvas
		 */
		public void update(Graphics g) {}
		public void paint(Graphics g) {
			drawInfo(g);
		}
	}

}
