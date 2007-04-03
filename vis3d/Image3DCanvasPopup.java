package vis3d;

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;

import ij.gui.GenericDialog;
import ij.IJ;
import ij.WindowManager;
import ij.ImagePlus;
import vib.Resample_;

import javax.vecmath.Color3f;

public class Image3DCanvasPopup extends PopupMenu 
						 implements ActionListener {

	private Image3DUniverse univ;

	private MenuItem mesh;
	private MenuItem paint;
	private MenuItem delete;
	private MenuItem startRecord;
	private MenuItem stopRecord;

	private Point p = null;

	public Image3DCanvasPopup(Image3DUniverse universe) {
		super();
		this.univ = universe;

		mesh = new MenuItem("Add mesh");
		mesh.addActionListener(this);
		this.add(mesh);

		delete = new MenuItem("Delete");
		delete.addActionListener(this);
		this.add(delete);
		
		paint = new MenuItem("Painting");
		paint.addActionListener(this);
		this.add(paint);

		this.addSeparator();

		startRecord = new MenuItem("Start recording");
		startRecord.addActionListener(this);
		this.add(startRecord);

		stopRecord = new MenuItem("Stop recording");
		stopRecord.addActionListener(this);
		this.add(stopRecord);

		this.addSeparator();

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

		if(e.getSource() == mesh) {
			addNewImage(univ, null);
		}

		if(e.getSource() == delete) {
			if(p != null) {
				univ.removeContentAt(p.x, p.y);
				p = null;
			}
		}
		
		if(e.getSource() == startRecord) {
			univ.recording = true;
		}

		if(e.getSource() == stopRecord) {
			univ.stopRecording().show();
		}
	}

	public static void addNewImage(Image3DUniverse univ,ImagePlus mesh){
		GenericDialog gd = new GenericDialog("Add mesh");
		int img_count = WindowManager.getImageCount();
		Vector meshV = new Vector();
		String[] meshs;
		if(mesh == null) {
			for(int i=1; i<=img_count; i++) {
				int id = WindowManager.getNthImageID(i);
				ImagePlus imp = WindowManager.getImage(id);
				if(imp != null){
					 meshV.add(imp.getTitle());
				}
			}
			if(meshV.size() == 0)
				IJ.error("No images open");
			meshs = (String[])meshV.toArray(new String[]{});
			gd.addChoice("Image", meshs, meshs[0]);
		}
		String tmp = mesh != null ? mesh.getTitle() : "";
		gd.addStringField("Name", tmp, 10);
		gd.addChoice("Color", colorNames, colorNames[0]);
		gd.addNumericField("Threshold", 50, 0);
		gd.addNumericField("Resampling factor", 2, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		if(mesh == null)
			mesh = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		Color3f color = getColor(gd.getNextChoice());
		int threshold = (int)gd.getNextNumber();
		int factor = (int)gd.getNextNumber();
		if(factor != 1)
			mesh = Resample_.resample(mesh, factor);
		univ.addVoltex(mesh, color, name);
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

