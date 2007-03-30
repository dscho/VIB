package isosurface;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;

import ij.gui.GenericDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import vib.Resample_;

import javax.vecmath.Color3f;

public class IsosurfaceCanvasPopup extends PopupMenu 
						 implements ActionListener {

	private IsosurfaceUniverse univ;

	private MenuItem image;
	private MenuItem paint;

	public IsosurfaceCanvasPopup(IsosurfaceUniverse universe) {
		super();
		this.univ = universe;

		image = new MenuItem("Add image");
		image.addActionListener(this);
		this.add(image);
		
		paint = new MenuItem("Painting");
		paint.addActionListener(this);
		this.add(paint);

		univ.getCanvas().add(this);
   		
		univ.getCanvas().addMouseListener(new MouseAdapter(){
			public void mousePressed(MouseEvent e){
				showPopup(e);	
			}
			public void mouseReleased(MouseEvent e){
				showPopup(e);	
			}
			private void showPopup(MouseEvent e) {
				if(e.isPopupTrigger()){
					show(univ.getCanvas(), e.getX(),e.getY());
				}
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == paint) {
			System.out.println("painting");
		} 

		if(e.getSource() == image) {
			GenericDialog gd = new GenericDialog("Add image");
			int img_count = WindowManager.getImageCount();
			Vector imageV = new Vector();
			String[] images;
			for(int i=1; i<=img_count; i++) {
				int id = WindowManager.getNthImageID(i);
				ImagePlus imp = WindowManager.getImage(id);
				if(imp != null){
					 imageV.add(imp.getTitle());
				}
			}
			if(imageV.size() == 0)
				IJ.error("No images open");
			images = (String[])imageV.toArray(new String[]{});
			gd.addChoice("Image", images, images[0]);
			gd.addChoice("Color", colorNames, colorNames[0]);
			gd.addNumericField("Threshold", 50, 0);
			gd.addNumericField("Resampling factor", 2, 0);

			gd.showDialog();
			if(gd.wasCanceled())
				return;
			
			ImagePlus newImage = WindowManager.getImage(gd.getNextChoice());
			Color3f color = getColor(gd.getNextChoice());
			int threshold = (int)gd.getNextNumber();
			int factor = (int)gd.getNextNumber();
			newImage = Resample_.resample(newImage, factor, factor, factor);
			univ.addImage(newImage, threshold, color);
		}
	}

	private Color3f getColor(String name) {
		for(int i = 0; i < colors.length; i++) {
			if(colorNames[i].equals(name)){
				System.out.println("col=" + name);
				System.out.println("return " + colors[i]);
				return colors[i];
			}
		}
		System.out.println("return null");
		return null;
	}


	private static String[] colorNames = new String[]{"Red", "Green", "Blue", 
		"Magenta"};

	private static Color3f[] colors = {new Color3f(1.0f, 0, 0),
								new Color3f(0, 1.0f, 0),
								new Color3f(0, 0, 1.0f),
								new Color3f(0, 1.0f, 1.0f)};
}

