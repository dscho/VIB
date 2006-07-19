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

	class CustomCanvas extends SegmentationViewerCanvas {

		CustomCanvas(ImagePlus imp) {
			super(imp);
			//setBounds(new Rectangle(getSize()));
		}

		public ImagePlus getImage() {
			return imp;
		}

		public Dimension getMinimumSize() {
			return getSize();
		}

		public void mouseExited(MouseEvent e) {
			super.mouseExited(e);
			containerPanel.pInfos.updateLabels();
		}

		public void mouseMoved(MouseEvent e) {
			super.mouseMoved(e);

			int x = offScreenX(e.getX());
			int y = offScreenY(e.getY());

			double posX, posY, posZ;
			int voxelValue;
			int materialID;
			String materialName;

			if(x<imp.getWidth() && y<imp.getHeight()) {
				Calibration cal = imp.getCalibration();
				posX = cal.getX(x);
				posX = Double.valueOf(IJ.d2s(posX)).doubleValue();

				posY = cal.getY(y);
				posY = Double.valueOf(IJ.d2s(posY)).doubleValue();
				int z = imp.getCurrentSlice()-1;
				posZ = cal.getZ(z);
				posZ = Double.valueOf(IJ.d2s(posZ)).doubleValue();

				voxelValue = imp.getProcessor().get(x, y);
				
				materialID = getLabels().getStack().getProcessor(z+1).get(x,y);
				materialName = containerPanel.pMain.materials.params.getMaterialName(materialID);
				
				containerPanel.pInfos.updateLabels(x, y, z,
						posX, posY, posZ,
						voxelValue, materialName);
			} else
				containerPanel.pInfos.updateLabels();

		}

		public void setMagnification(double magnification) {
			super.setMagnification(magnification);
			if(containerPanel != null &&
					containerPanel.pMain != null)
				containerPanel.pMain.updateLZoomLevel(magnification);
		}

	}

	class CustomStackWindow extends StackWindow implements AdjustmentListener, KeyListener{
		Roi[] savedRois;
		int oldSlice;

		CustomStackWindow(ImagePlus imp) {
			super(imp, cc);
			
			savedRois = new Roi[imp.getStack().getSize() + 1];
			oldSlice = sliceSelector.getValue();
			sliceSelector.addAdjustmentListener(this);
			
			// Remove ij from the key listeners to avoid zooming when pressing + or -
			cc.removeKeyListener(ij);
			cc.addKeyListener(this);
			
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

			add(sliceAndImage, BorderLayout.CENTER);
			pack();
		} 

		public Dimension getMinimumSize() {
			return getSize();
		}

		void addPanels() {
			containerPanel = new ContainerPanel(cc, this, savedRois);
			//containerPanel.setVisible(true);
			add(containerPanel, BorderLayout.WEST);
		}

		public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
			super.adjustmentValueChanged(e);
			savedRois[oldSlice] = imp.getRoi();
			oldSlice = sliceSelector.getValue();
			if (savedRois[oldSlice] == null)
				imp.killRoi();
			else
				imp.setRoi(savedRois[oldSlice]);
		}

		/**
		 * overridden in order to fix the problem of drawing a rectangle close to the ImageCanvas
		 */
		// public void update(Graphics g) {}
		public void paint(Graphics g) {
			drawInfo(g);
		}
		
		public void keyTyped(KeyEvent e) {}
		public void keyPressed(KeyEvent e) {}

		public void keyReleased(KeyEvent e) {
			int c = e.getKeyCode();
			if(c == KeyEvent.VK_UP || c == KeyEvent.VK_RIGHT){
				imp.setSlice(oldSlice + 1);
				adjustmentValueChanged(new AdjustmentEvent(sliceSelector,AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,AdjustmentEvent.BLOCK_INCREMENT,oldSlice+1));
			} else if (c == KeyEvent.VK_DOWN || c == KeyEvent.VK_LEFT){
				imp.setSlice(oldSlice - 1);
				adjustmentValueChanged(new AdjustmentEvent(sliceSelector,AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,AdjustmentEvent.BLOCK_DECREMENT,oldSlice-1));
			} else if (c == KeyEvent.VK_PAGE_DOWN){
				imp.setSlice(oldSlice - 5);
				adjustmentValueChanged(new AdjustmentEvent(sliceSelector,AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,AdjustmentEvent.BLOCK_DECREMENT,oldSlice-5));
			} else if (c == KeyEvent.VK_PAGE_UP){
				imp.setSlice(oldSlice + 5);
				adjustmentValueChanged(new AdjustmentEvent(sliceSelector,AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,AdjustmentEvent.BLOCK_DECREMENT,oldSlice+5));
			} else if (e.getKeyChar() == '+'){
				containerPanel.pMain.processPlusButton();
			}else if (e.getKeyChar() == '-'){
				containerPanel.pMain.processMinusButton();
			}			
		}
	}
}
