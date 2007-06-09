package isosurface;

import java.util.List;
import java.util.Vector;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import ij.measure.Calibration;

import ij3d.Content;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import vib.Resample_;

import marchingcubes.MCTriangulator;

import javax.media.j3d.View;
import javax.media.j3d.Transform3D;
import javax.vecmath.Vector3f;
import javax.vecmath.Point3f;
import javax.vecmath.Color3f;

public class MeshGroup extends Content {

	IsoShape shape; 
	int threshold;
	Triangulator triangulator = new MCTriangulator();
//	Triangulator triangulator = new discMC.DiscMCTriangulator();
	
	public MeshGroup(String name, Color3f color, 
			List mesh, int thresh, Transform3D initial) {
		super(name, color, initial);
		this.threshold = thresh;
		if(color == null) {
			color= new Color3f(
				thresh/255f, thresh/255f, thresh/255f);
		}
		shape = new IsoShape(mesh, color, thresh, getTransparency());
		initialTG.addChild(shape);
		compile();
	}

	public MeshGroup(String name, Color3f color, ImagePlus image, boolean[] 
		channels, int resamplingF, int threshold, Transform3D initial) {

		super(name, color, image, channels, resamplingF, initial);
		this.threshold = threshold;
		List mesh = triangulator.getTriangles(
				image, threshold, channels, resamplingF);
		if(color == null) {
			int value = image.getProcessor().
					getColorModel().getRGB(threshold);
			color = new Color3f(new Color(value));
		}
		shape = new IsoShape(mesh, color, threshold, getTransparency());
		initialTG.addChild(shape);
		compile();
	}
		

	public void eyePtChanged(View view) {
		// do nothing
	}

	public void channelsUpdated(boolean [] channels) {
		List mesh = triangulator.getTriangles(getImage(), 
			threshold, channels, getResamplingFactor());
		shape.mesh = mesh;
		shape.update();
	}

	public void calculateMinMaxCenterPoint() {
		minPoint = new Point3f(); maxPoint = new Point3f();
		centerPoint = new Point3f();
		if(shape != null) {
			shape.calculateMinMaxCenterPoint(minPoint, maxPoint,
				centerPoint);
		}
	}

	public void colorUpdated(Color3f oldColor, Color3f newColor) {
		if(newColor == null){
			int val = getImage().getProcessor().
					getColorModel().getRGB(threshold);
			newColor = new Color3f(new Color(val));
		}
		shape.setColor(newColor);	
	}

	public void transparencyUpdated(float transparency) {
		shape.setTransparency(transparency);
	}

	public static void addContent(Image3DUniverse univ, ImagePlus mesh) {
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
		gd.addChoice("Color", ColorTable.colorNames, 
						ColorTable.colorNames[0]);
		gd.addNumericField("Threshold", 50, 0);
		gd.addNumericField("Resampling factor", 2, 0);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
					new String[] {"red", "green", "blue"}, 
					new boolean[]{true, true, true});

		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		if(mesh == null)
			mesh = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		Color3f color = ColorTable.getColor(gd.getNextChoice());
		int threshold = (int)gd.getNextNumber();
		int factor = (int)gd.getNextNumber();
		boolean[] channels = new boolean[] {gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		Vector3f tr = new Vector3f();
		if(mesh != null) {
			Calibration c = mesh.getCalibration();
			tr.x = (float)(-mesh.getWidth() * c.pixelWidth/2f);
			tr.y = (float)(-mesh.getHeight() * c.pixelHeight/2f); 
			tr.z = (float)(-mesh.getStackSize() * c.pixelDepth/2f);
		}
		univ.addMesh(mesh, color, 
			name, threshold, channels, factor, tr);
	}
}

