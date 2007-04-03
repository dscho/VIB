package isosurface;

import java.util.List;
import java.util.Vector;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;

import vis3d.Content;
import vis3d.Image3DUniverse;

import vib.Resample_;

import javax.vecmath.Color3f;

public class MeshGroup extends Content {

	public MeshGroup(String name, Color3f color, List mesh) {
		super(name, color);
		// create the IsoShape for this image and add it
		IsoShape shape = new IsoShape(mesh, color, name);
		pickTr.addChild(shape);

		compile();
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
		univ.addMesh(mesh, threshold, color, name);
	}
}

