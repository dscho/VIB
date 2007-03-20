package vib.segment;

import java.awt.*;
import java.awt.event.*;

import vib.IDT_Interpolate_Binary;

import ij.IJ;
import ij.measure.Calibration;
import ij.gui.StackWindow;
import ij.gui.Roi;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public class CustomStackWindow extends StackWindow
					 implements AdjustmentListener, 
								KeyListener, 
								ActionListener, 
								MouseMotionListener,
								MouseWheelListener {
	
	private Roi[] savedRois;
	private int oldSlice;

	/* Listener for the ok button, to get informed when 
	 * labelling is finished */
	private ActionListener al;

	private Sidebar sidebar;
	private CustomCanvas cc;
	private Button ok;

	

	public CustomStackWindow(ImagePlus imp) {
		super(imp, new CustomCanvas(imp));
		this.cc = (CustomCanvas)getCanvas();
		
		savedRois = new Roi[imp.getStack().getSize() + 1];
		oldSlice = sliceSelector.getValue();
		sliceSelector.addAdjustmentListener(this);
		
		// Remove ij from the key listeners to avoid zooming 
		// when pressing + or -
		cc.removeKeyListener(ij);
		cc.addKeyListener(this);
		cc.addMouseMotionListener(this);
		
		setLayout(new BorderLayout());
		setBackground(Color.LIGHT_GRAY);
		remove(sliceSelector);
		remove(cc);
		sidebar = new Sidebar(cc, this);
		add(sidebar, BorderLayout.WEST);

		Container sliceAndImage = new Container();
		sliceAndImage.setSize(cc.getSize());
		sliceAndImage.setLayout(new BorderLayout());
		sliceAndImage.add(sliceSelector, BorderLayout.NORTH);
		sliceAndImage.add(new Label(" "));
		sliceAndImage.add(cc, BorderLayout.CENTER);

		add(sliceAndImage, BorderLayout.EAST); 

		Panel buttonPanel = new Panel(new FlowLayout());
        ok = new Button("Ok");
        ok.addActionListener(this);
        buttonPanel.add(ok);
        add(buttonPanel, BorderLayout.SOUTH);

		pack();
	}

	public void cleanUp() {
		savedRois = null;
		al = null;
		sidebar.getMaterials().labels = null;
		sidebar = null;
		ok = null;
		cc.releaseImage();
		cc = null;
		imp.close();
		imp = null;
	}

	public ImagePlus getLabels() {
		return cc.getLabels();
	}

	public void setLabels(ImagePlus labels) {
		sidebar.setLabelImage(labels);
	}

	public void addActionListener(ActionListener al) {
		this.al = al;
	}
	
	public Sidebar getSidebar() {
		return sidebar;
	}

	public CustomCanvas getCustomCanvas() {
		return cc;
	}

	public Dimension getMinimumSize() {
		return getSize();
	}

	public void processPlusButton(){
		int currentSlice = cc.getImage().getCurrentSlice();
		Roi roi = cc.getImage().getRoi();
		assignSliceTo(currentSlice,roi,sidebar.currentMaterialID());	
		cc.getImage().killRoi();
		if(sidebar.is3d()){
			for(int i=0;i<savedRois.length;i++){
				roi = savedRois[i];
				if(roi != null){
					assignSliceTo(i,roi,sidebar.currentMaterialID());
					savedRois[i] = null;
				}
			}
		}
		cc.getImage().setSlice(currentSlice);
		cc.getLabels().setSlice(currentSlice);
		cc.getImage().updateAndDraw();
		cc.getLabels().updateAndDraw();
	}
	
	public void processMinusButton(){
		int currentSlice = cc.getImage().getCurrentSlice();
		Roi roi = cc.getImage().getRoi();
		releaseSliceFrom(currentSlice, roi, sidebar.currentMaterialID());
		cc.getImage().killRoi();
		if(sidebar.is3d()){
			for(int i=0;i<savedRois.length;i++){
				roi = savedRois[i];
				if(roi != null){
					releaseSliceFrom(i,roi,sidebar.currentMaterialID());
					savedRois[i] = null;
				}
			}
		}
		cc.getImage().setSlice(currentSlice);
		cc.getLabels().setSlice(currentSlice);
		cc.getImage().updateAndDraw();
		cc.getLabels().updateAndDraw();
	}

	public void processInterpolateButton() {
		updateRois();
		new Thread(new Runnable() {
			public void run() {
				setCursor(Cursor.WAIT_CURSOR);
				new IDT_Interpolate_Binary().run(cc.getImage(), savedRois);
				setCursor(Cursor.DEFAULT_CURSOR);
			}
		}).start();
	}
	
	public void assignSliceTo(int slice, Roi roi, int materialID){
		ImagePlus grey = cc.getImage();
		ImagePlus labels = cc.getLabels();
		if (grey == null || labels == null)
			return;			
		if (roi == null)
			return;
		ImageProcessor labP = labels.getStack().getProcessor(slice);
		labP.setRoi(roi);
		Rectangle bounds = roi.getBoundingRect();
		for(int i=bounds.x;i<=bounds.x+bounds.width;i++){
			for(int j=bounds.y;j<=bounds.y+bounds.height;j++){
				if(roi.contains(i,j)) labP.set(i,j,materialID);
			}
		}
		cc.updateSlice(slice);
	}
	
	public void releaseSliceFrom(int slice, Roi roi, int materialID){
		ImagePlus grey = cc.getImage();
		ImagePlus labels = cc.getLabels();
		if (grey == null || labels == null)
			return;			
		if (roi == null)
			return;
		ImageProcessor labP = labels.getStack().getProcessor(slice);
		labP.setRoi(roi);
		Rectangle bounds = roi.getBoundingRect();
		for(int i=bounds.x;i<=bounds.x+bounds.width;i++){
			for(int j=bounds.y;j<=bounds.y+bounds.height;j++){
				if(roi.contains(i,j) && labP.get(i,j)==materialID){ 
					labP.set(i,j,sidebar.getMaterials().getDefaultMaterialID());
				}
			}
		}
		cc.updateSlice(slice);
	}	

	/*
	 * MouseMotionListener interface
	 */
	public void mouseDragged(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {

		int x = cc.offScreenX(e.getX());
		int y = cc.offScreenY(e.getY());

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
			
			materialID = cc.getLabels().getStack().getProcessor(z+1).get(x,y);
			materialName = sidebar.getMaterials()
								.params.getMaterialName(materialID);
			
			IJ.showStatus("x=" + posX + ", y=" + posY + ", z=" + posZ + 
					", value=" + voxelValue + ", material=" + materialName);
		}
	}	
	
	/*
	 * overridden in order to fix the problem of drawing a rectangle 
	 * close to the ImageCanvas
	 */
	public void paint(Graphics g) {
		super.paint(g);
		drawInfo(g);
	}
	
	/*
	 * ActionListener interface
	 */
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("zoomin")) {
			cc.zoomIn(cc.getWidth()/2, cc.getHeight()/2);
		} else if (command.equals("zoomout")) {
			cc.zoomOut(cc.getWidth()/2, cc.getHeight()/2);
		} else if (command.equals("plus")) {
			processPlusButton();
		} else if (command.equals("minus")) {
			processMinusButton();
		} else if (command.equals("interpolate")) {
			processInterpolateButton();
		} else if (command.equals("Ok")) {
			// call the action listener before destroying the window
			if(al != null)
				al.actionPerformed(e);
			// new StackWindow(getImagePlus());
			getImagePlus().close();
		}
	}

	/*
	 * AdjustmentListener interface
	 */
	public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		super.adjustmentValueChanged(e);
		updateRois();
	}

	public synchronized void updateRois() {
		savedRois[oldSlice] = imp.getRoi();
		oldSlice = sliceSelector.getValue();
		if (savedRois[oldSlice] == null)
			imp.killRoi();
		else
			imp.setRoi(savedRois[oldSlice]);
		repaint();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		super.mouseWheelMoved(e);
		updateRois();
	}
	
	/*
	 * KeyListener interface
	 */
	public void keyTyped(KeyEvent e) {}
	
	public void keyPressed(KeyEvent e) {}

	public void keyReleased(KeyEvent e) {
		int c = e.getKeyCode();
		if(c == KeyEvent.VK_UP || c == KeyEvent.VK_RIGHT){
			imp.setSlice(oldSlice + 1);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_INCREMENT,
						oldSlice+1));
		} else if (c == KeyEvent.VK_DOWN || c == KeyEvent.VK_LEFT){
			imp.setSlice(oldSlice - 1);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_DECREMENT,
						oldSlice-1));
		} else if (c == KeyEvent.VK_PAGE_DOWN){
			imp.setSlice(oldSlice - 5);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_DECREMENT,
						oldSlice-5));
		} else if (c == KeyEvent.VK_PAGE_UP){
			imp.setSlice(oldSlice + 5);
			adjustmentValueChanged(new AdjustmentEvent(
						sliceSelector,
						AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED,
						AdjustmentEvent.BLOCK_DECREMENT,
						oldSlice+5));
		} else if (e.getKeyChar() == '+'){
			processPlusButton();
		} else if (e.getKeyChar() == '-'){
			processMinusButton();
		}			
	}
}
