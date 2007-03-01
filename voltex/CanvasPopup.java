package voltex;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;

import ij.gui.GenericDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;

import javax.vecmath.Color3f;

public class CanvasPopup extends PopupMenu 
						 implements ActionListener, ItemListener {

	private VolRend volrend;

	private MenuItem image;
	private MenuItem reset;
	private MenuItem fill;
	private MenuItem reload;
	private CheckboxMenuItem coord_cb;
	private CheckboxMenuItem perspective_cb;

	public CanvasPopup(VolRend volr) {
		super();
		this.volrend = volr;

		image = new MenuItem("Open image");
		image.addActionListener(this);
		this.add(image);

		fill = new MenuItem("Fill selection");
		fill.addActionListener(this);
		this.add(fill);

		coord_cb = new CheckboxMenuItem("Coordinate system", true);
		coord_cb.addItemListener(this);
		this.add(coord_cb);
		
		perspective_cb = new CheckboxMenuItem("Perspective Projection", true);
		perspective_cb.addItemListener(this);
		this.add(perspective_cb);

		reset = new MenuItem("Reset view");
		reset.addActionListener(this);
		this.add(reset);

		reload = new MenuItem("Reload image");
		reload.addActionListener(this);
		this.add(reload);

		volrend.getCanvas().add(this);
   		
		volrend.getCanvas().addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				showPopup(e);	
			}
			public void mouseReleased(MouseEvent e){
				showPopup(e);	
			}
			private void showPopup(MouseEvent e) {
				if(e.isPopupTrigger()){
					show(volrend.getCanvas(), e.getX(),e.getY());
				}
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == fill) {
			if(volrend.getCanvas().getRoi() == null) {
				IJ.showMessage("Selection required");
				return;
			}
			int intensity = (int)IJ.getNumber("Intensity: [0..255]", 0);
			if(intensity == IJ.CANCELED) 
				return;
			intensity = intensity < 0 ? 0 : intensity;
			intensity = intensity > 255 ? 255 : intensity;
			final byte fillVal = (byte)intensity;
			new Thread(new Runnable() {
				public void run() {
					volrend.fillRoiBlack(fillVal);
					volrend.update();
				}
			}).start();
		} 

		if(e.getSource() == reset) {
			volrend.resetView();
		}

		if(e.getSource() == image) {
			GenericDialog gd = new GenericDialog("Display image");
			int img_count = WindowManager.getImageCount();
			Vector imageV = new Vector();
			String[] images;
			System.out.println("img_count = " + img_count);
			for(int i=1; i<=img_count; i++) {
				int id = WindowManager.getNthImageID(i);
				ImagePlus imp = WindowManager.getImage(id);
				System.out.println("testing " + imp);
				if(imp != null){
					 imageV.add(imp.getTitle());
				}
			}
			if(imageV.size() == 0)
				IJ.error("No images open");
			images = (String[])imageV.toArray(new String[]{});
			gd.addChoice("Image", images, images[0]);

			String[] colors = ColorTable.colors();
			gd.addChoice("Color", colors, colors[0]);

//			gd.addCheckbox("Replace current image", true);
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			
			ImagePlus newImage = WindowManager.getImage(gd.getNextChoice());
			Color3f color = ColorTable.getColor(gd.getNextChoice());
			boolean replace = true; //gd.getNextBoolean();
			volrend.initContext(newImage, color, replace);
		}

		if(e.getSource() == reload) {
			volrend.reload();
		}
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == coord_cb) {
			volrend.showCoordinateSystem(coord_cb.getState());
		}

		if(e.getSource() == perspective_cb) {
			volrend.setPerspectiveProjection(perspective_cb.getState());
		}
	}
}

