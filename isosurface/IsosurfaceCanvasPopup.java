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
	private MenuItem delete;

	private Point p = null;

	public IsosurfaceCanvasPopup(IsosurfaceUniverse universe) {
		super();
		this.univ = universe;

		image = new MenuItem("Add image");
		image.addActionListener(this);
		this.add(image);

		delete = new MenuItem("Delete");
		delete.addActionListener(this);
		this.add(delete);
		
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
					p = e.getPoint();
					show(univ.getCanvas(), 
							e.getX(),e.getY());
				}
			}
		});
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getSource() == paint) {
			System.out.println("painting");
		} 

		if(e.getSource() == image) {
			addNewImage(univ, null);
		}

		if(e.getSource() == delete) {
			if(p != null) {
				univ.removeImageAt(p.x, p.y);
				p = null;
			}
		}
	}

	public static void addNewImage(IsosurfaceUniverse univ,ImagePlus image){
		GenericDialog gd = new GenericDialog("Add image");
		int img_count = WindowManager.getImageCount();
		Vector imageV = new Vector();
		String[] images;
		if(image == null) {
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
		}
		String tmp = image != null ? image.getTitle() : "";
		gd.addStringField("Name", tmp, 10);
		gd.addChoice("Color", colorNames, colorNames[0]);
		gd.addNumericField("Threshold", 50, 0);
		gd.addNumericField("Resampling factor", 2, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		if(image == null)
			image = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		Color3f color = getColor(gd.getNextChoice());
		int threshold = (int)gd.getNextNumber();
		int factor = (int)gd.getNextNumber();
		if(factor != 1)
			image = Resample_.resample(image, factor);
		univ.addImage(image, threshold, color, name);
	}	

	private static Color3f getColor(String name) {
		for(int i = 0; i < colors.length; i++) {
			if(colorNames[i].equals(name)){
				return colors[i];
			}
		}
		return null;
	}


	private static String[] colorNames = new String[]{"White", "Red", 
				"Green", "Blue", "Cyan", "Magenta", "Yellow"};

	private static Color3f[] colors = {
				new Color3f(1.0f, 1.0f, 1.0f),
				new Color3f(1.0f, 0,    0),
				new Color3f(0,    1.0f, 0),
				new Color3f(0,    0,    1.0f),
				new Color3f(0,    1.0f, 1.0f),
				new Color3f(1.0f, 0,    1.0f),
				new Color3f(1.0f, 1.0f, 0)};
}

