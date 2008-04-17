package orthoslice;

import java.awt.Polygon;
import java.awt.image.IndexColorModel;

import java.util.List;
import java.util.Vector;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.vecmath.Point2d;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;
import javax.media.j3d.View;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.BranchGroup;
import com.sun.j3d.utils.behaviors.picking.PickingCallback;

import ij.ImagePlus;
import ij.IJ;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.Calibration;

import ij3d.Content;
import ij3d.ImageCanvas3D;
import ij3d.Image3DUniverse;
import ij3d.ColorTable;

import vib.Resample_;
import voltex.*;

public class OrthoGroup extends Content {

	private Renderer renderer;
	private TransformGroup tg;
	private int[] slices;

	public OrthoGroup(String name, Color3f color, ImagePlus image, 
		boolean[] channels, int resamplingF) {
		
		super(name, color, image, channels, resamplingF);
		float scale = image.getWidth() * 
				(float)image.getCalibration().pixelWidth;

		IndexColorModel cmodel = color == null ? 
			ColorTable.getOpaqueIndexedColorModel(image, channels) :
			ColorTable.getOpaqueAverageGrayColorModel(image, channels);
		ImagePlus imp = resamplingF == 1 ? image 
				: Resample_.resample(image, resamplingF);
		renderer = new Orthoslice(imp, cmodel, 
				color, getTransparency());
		slices = new int[] {imp.getWidth()/2, imp.getHeight()/2,
					imp.getStackSize()/2};
		renderer.fullReload();

		getLocalRotate().addChild(renderer.getVolumeNode());

		compile();
	}

	public void calculateMinMaxCenterPoint() {
		ImagePlus imp = getImage();
		int w = imp.getWidth(), h = imp.getHeight();
		int d = imp.getStackSize();
		Calibration c = imp.getCalibration();
		minPoint = new Point3f();
		maxPoint = new Point3f();
		centerPoint = new Point3f();
		minPoint.x = w * (float)c.pixelHeight;
		minPoint.y = h * (float)c.pixelHeight;
		minPoint.z = d * (float)c.pixelDepth;
		maxPoint.x = 0;
		maxPoint.y = 0;
		maxPoint.z = 0;

		long count = 0;
		for(int zi = 0; zi < d; zi++) {
			float z = zi * (float)c.pixelDepth;
			byte[] p = (byte[])imp.getStack().getPixels(zi+1);
			for(int i = 0; i < p.length; i++) {
				if(p[i] == 0) continue;
				float x = (i % w) * (float)c.pixelWidth;
				float y = (i / w) * (float)c.pixelHeight;
				if(x < minPoint.x) minPoint.x = x;
				if(y < minPoint.y) minPoint.y = y;
				if(z < minPoint.z) minPoint.z = z;
				if(x > maxPoint.x) maxPoint.x = x;
				if(y > maxPoint.y) maxPoint.y = y;
				if(z > maxPoint.z) maxPoint.z = z;
				centerPoint.x += x;
				centerPoint.y += y;
				centerPoint.z += z;
				count++;
			}
		}
		centerPoint.x /= count;
		centerPoint.y /= count;
		centerPoint.z /= count;
		
		createBoundingBox();
		showBoundingBox(false);
	}

// 	public void calculateMinMaxCenterPoint() {
// 		ImagePlus imp = getImage();
// 		Calibration c = imp.getCalibration();
// 		minPoint = new Point3f();
// 		maxPoint = new Point3f((float)(imp.getWidth()*c.pixelWidth),
// 				(float)(imp.getHeight()*c.pixelHeight),
// 				(float)(imp.getStackSize()*c.pixelDepth));
// 		centerPoint = new Point3f(maxPoint.x/2, maxPoint.y/2, 
// 				maxPoint.z/2);
// 		createBoundingBox();
// 		showBoundingBox(false);
// 	}
		
	public static OrthoGroup addContent(Image3DUniverse univ, 
							ImagePlus grey) {
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
		gd.addChoice("Color", ColorTable.colorNames, 
						ColorTable.colorNames[0]);
		gd.addNumericField("Resampling factor", 1, 0);
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, 
					new String[] {"red", "green", "blue"}, 
					new boolean[]{true, true, true});
		gd.showDialog();
		if(gd.wasCanceled())
			return null;
			
		if(grey == null)
			grey = WindowManager.getImage(gd.getNextChoice());
		String name = gd.getNextString();
		Color3f color = ColorTable.getColor(gd.getNextChoice());
		int factor = (int)gd.getNextNumber();
		boolean[] channels = new boolean[]{gd.getNextBoolean(), 
						gd.getNextBoolean(), 
						gd.getNextBoolean()};
		

		if(univ.contains(name)) {
			IJ.error("Could not add new content. A content with " +
				"name \"" + name + "\" exists already.");
			return null;
		}
		
		return univ.addOrthoslice(grey, color, name, channels, factor);
	}

	public int[] getSlices() {
		return slices;
	}

	public void eyePtChanged(View view) {
		renderer.eyePtChanged(view);
	}

	public void thresholdUpdated(int d) {
		renderer.setThreshold(d);
	}

	public void channelsUpdated(boolean[] channels) {
		IndexColorModel cmodel = getColor() == null ?
			ColorTable.getOpaqueIndexedColorModel(
				getImage(), channels) :
			ColorTable.getOpaqueAverageGrayColorModel(
				getImage(), channels);
		renderer.setColorModel(cmodel);
	}

	public void colorUpdated(Color3f oldColor, Color3f newColor) {
		// color model only needs update if there is a switch
		// between null and non-null color
		if(oldColor == null && newColor != null || 
			oldColor != null && newColor == null) {

			IndexColorModel cmodel = newColor == null ?
				ColorTable.getOpaqueIndexedColorModel(
					getImage(), getChannels()) :
				ColorTable.getOpaqueAverageGrayColorModel(
					getImage(), getChannels());
			renderer.setColorModel(cmodel);
		}
		renderer.setColor(newColor);
	}

	public void transparencyUpdated(float transparency) {
		renderer.setTransparency(transparency);
	}

	public void setSlices(int x, int y, int z) {
		((Orthoslice)renderer).setSlices(x, y, z);
	}

	public void flush() {
		image = null;
		renderer = null;
	}
}

