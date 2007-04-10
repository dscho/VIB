package voltex;

import java.util.List;
import java.util.Vector;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import com.sun.j3d.utils.behaviors.picking.PickingCallback;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;
import ij.gui.GenericDialog;

import vis3d.Content;
import vis3d.Image3DUniverse;

import vib.Resample_;

public class VoltexGroup extends Content {

	private Renderer renderer;

	public VoltexGroup(String name, Color3f color, ImagePlus image) {
		super(name, color);
		
		float scale = image.getWidth() * 
					(float)image.getCalibration().pixelWidth;

		renderer = new Axis2DRenderer(image, color);
		renderer.update();

		Point3d maxCoord = renderer.volume.maxCoord;
		Point3d minCoord = renderer.volume.minCoord;
		
		Transform3D translate = new Transform3D();
		translate.setTranslation(new Vector3d(
					-(maxCoord.x-minCoord.x)/2, 
					-(maxCoord.y-minCoord.y)/2, 
					-(maxCoord.z-minCoord.z)/2));
		TransformGroup tg = new TransformGroup(translate);
		
		pickTr.addChild(tg);
		tg.addChild(renderer.getVolumeNode());

		compile();
	}

	public static void addContent(Image3DUniverse univ, ImagePlus grey) {
		GenericDialog gd = new GenericDialog("Add grey");
		int img_count = WindowManager.getImageCount();
		Vector greyV = new Vector();
		String[] greys;
		if(grey == null) {
			for(int i=1; i<=img_count; i++) {
				int id = WindowManager.getNthImageID(i);
				ImagePlus imp = WindowManager.getImage(id);
				if(imp != null){
					 greyV.add(imp.getTitle());
				}
			}
			if(greyV.size() == 0)
				IJ.error("No images open");
			greys = (String[])greyV.toArray(new String[]{});
			gd.addChoice("Image", greys, greys[0]);
		}
		String tmp = grey != null ? grey.getTitle() : "";
		gd.addStringField("Name", tmp, 10);
		gd.addChoice("Color", colorNames, colorNames[0]);
		gd.addNumericField("Resampling factor", 2, 0);

		gd.showDialog();
		if(gd.wasCanceled())
			return;
			
		if(grey == null)
			grey = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		Color3f color = getColor(gd.getNextChoice());
		int factor = (int)gd.getNextNumber();
		if(factor != 1)
			grey = Resample_.resample(grey, factor);
		univ.addVoltex(grey, color, name);
	}

	public void update() {	
		Volume volume = renderer.volume;
		volume.update();
		renderer.update();
	}
		
	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	public void colorUpdated(Color3f color) {
	}
}

