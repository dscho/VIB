package isosurface;

import java.util.List;
import java.util.Vector;

import java.awt.Color;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import vis3d.Content;
import vis3d.Image3DUniverse;
import vis3d.ColorTable;

import vib.Resample_;

import marchingcubes.MCTriangulator;

import javax.media.j3d.View;
import javax.vecmath.Color3f;

public class MeshGroup extends Content {

	IsoShape shape; 
	int threshold;
	Triangulator triangulator = new MCTriangulator();

	public MeshGroup(String name, String color, List mesh, int thresh) {
		super(name, color);
		this.threshold = thresh;
		Color3f col = ColorTable.getColor(color);
		if(col == null) {
			col = new Color3f(thresh/255f, thresh/255f, thresh/255f);
		}
		shape = new IsoShape(mesh, col, thresh);
		pickTr.addChild(shape);
		compile();
	}

	public MeshGroup(String name, String color, ImagePlus image, 
				boolean[] channels, int resamplingF, int threshold) {
		super(name, color, image, channels, resamplingF);
		this.threshold = threshold;
		List mesh = triangulator.getTriangles(
						image, threshold, channels, resamplingF);
		Color3f col = ColorTable.getColor(color);
		if(col == null) {
			int value = 
				image.getProcessor().getColorModel().getRGB(threshold);
			col = new Color3f(new Color(value));
		}
		shape = new IsoShape(mesh, col, threshold);
		pickTr.addChild(shape);
		compile();
	}
		

	public void eyePtChanged(View view) {
		// do nothing
	}

	public void colorUpdated(String color, boolean[] channels) {
		Color3f c = null;
		if(color.equals("None")){
			int val = getImage().
					getProcessor().getColorModel().getRGB(threshold);
			c = new Color3f(new Color(val));
		} else {
			c = ColorTable.getColor(color);
		}
		boolean[] ch = getChannels();
		if(channelsChanged) {
			List mesh = triangulator.getTriangles(getImage(), 
				threshold, channels, getResamplingFactor());
			shape.mesh = mesh;
			shape.update();
		}
		shape.setColor(c);	
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
		gd.addCheckboxGroup(1, 3, new String[] {"red", "green", "blue"}, 
						new boolean[]{true, true, true});

		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		if(mesh == null)
			mesh = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		String color = gd.getNextChoice();
		int threshold = (int)gd.getNextNumber();
		int factor = (int)gd.getNextNumber();
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
								gd.getNextBoolean(), 
								gd.getNextBoolean()};
		univ.addMesh(mesh, color, name, threshold, channels, factor);
	}
}

